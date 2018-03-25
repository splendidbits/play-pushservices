package services.pushservices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.pushservices.TasksDao;
import enums.pushservices.RecipientState;
import exceptions.pushservices.TaskValidationException;
import helpers.pushservices.TaskHelper;
import interfaces.pushservices.SortedPlatformResponse;
import interfaces.pushservices.TaskQueueCallback;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import models.pushservices.db.Task;
import play.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A singleton class that handles all Push Service task jobs.
 */
@Singleton
public class TaskQueue {
    // Delay each taskqueue polling to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 200;

    // Collection containing all messages that have not returned from the provider with a success or fail.
    private Set<Long> mActiveMessages = new HashSet<>(); // <Message.Id>
    private Map<Long, TaskQueueCallback> mListeners = new HashMap<>(); // <Message.Id, listener.TaskQueueListener>

    // TaskQueue thread and queue related members.
    private BlockingQueue<Message> mMessageProcessQueue = new ArrayBlockingQueue<>(5000);
    private MessageProducerThread mQueueProducerThread;
    private MessageConsumerThread mQueueConsumerThread;

    private GcmMessageDispatcher mGcmMessageDispatcher;
    private TasksDao mTasksDao;

    /**
     * Privately instantiate the TaskQueue with required Dependencies.
     *
     * @param tasksDao             Task persistence.
     * @param gcmMessageDispatcher GCM Google message dispatcher.
     */
    @Inject
    protected TaskQueue(TasksDao tasksDao, GcmMessageDispatcher gcmMessageDispatcher) {
        mTasksDao = tasksDao;
        mGcmMessageDispatcher = gcmMessageDispatcher;
    }

    @SuppressWarnings("unused")
    private TaskQueue() {
    }

    /**
     * Checks that the task consumer process is active and running, and starts the
     * TaskQueue {@link Task} polling process if it is not.
     */
    public synchronized void startup() {
        Logger.info("TaskQueue Startup");

        // Start the message producer and consumer queues.
        startProducerQueue();
        startConsumerQueue();

        // Check and re-queue existing pending messages.
        queuePendingTaskMessages();
    }

    private synchronized void startProducerQueue() {
        if (mQueueProducerThread == null || !mQueueProducerThread.isAlive()) {
            mQueueProducerThread = new MessageProducerThread(mMessageProcessQueue);
            Logger.debug("Starting the TaskQueue Producer process.");
            mQueueProducerThread.start();
        }
    }

    private synchronized void startConsumerQueue() {
        if (mQueueConsumerThread == null || !mQueueConsumerThread.isAlive()) {
            mQueueConsumerThread = new MessageConsumerThread();
            Logger.debug("Starting the TaskQueue Consumer process.");
            mQueueConsumerThread.start();
        }
    }

    /**
     * Checks that the task consumer process is active and running, and starts the
     * TaskQueue {@link Task} polling process if it is not.
     */
    public synchronized void shutdown() {
        if (mQueueProducerThread != null && mQueueProducerThread.isAlive()) {
            Logger.debug("Shutting down the TaskQueue Producer process.");
            mQueueProducerThread.interrupt();
        }

        if (mQueueConsumerThread != null && mQueueConsumerThread.isAlive()) {
            Logger.debug("Shutting down the TaskQueue Consumer process.");
            mQueueConsumerThread.interrupt();
        }
    }

    /**
     * Queue any tasks that have pending recipients. If you call this method, be sure that the tasks
     * in question are not being processed by the ConsumerThread, or are awaiting results
     * from the dispatcher. Ideally, the TaskQueue should not be started.
     */
    private void queuePendingTaskMessages() {
        Logger.info(String.format("Pending Message check. Active queue: %d.", mActiveMessages.size()));

        // Get outstanding incomplete message tasks and startup queue producer thread.
        List<Task> pendingTasks = mTasksDao.fetchPendingTasks();

        for (Task task : pendingTasks) {
            for (Message message : task.messages) {
                if (!isMessageInQueue(message)) {
                    addMessageToProducer(message);
                }
            }
        }
    }

    /**
     * Dispatch a Task {@link Message}
     *
     * @param message  The {@link Task} to dispatch.
     * @param callback The {@link TaskQueueCallback} callback which notifies on required actions.
     *                 <p>
     *                 If there was a problem with the task a TaskValidationException will be thrown.
     */
    private void dispatchMessage(@Nonnull Message message, @Nonnull SortedPlatformTaskCallback callback) {
        int messageRecipientCount = 0;

        try {
            if (TaskHelper.isMessageProcessReady(message)) {

                // Set recipient states to processing for ready recipients.
                for (Recipient recipient : message.recipients) {

                    // The recipient is out of the cooling off period.
                    if (TaskHelper.isRecipientPending(recipient) && !TaskHelper.isRecipientCoolingOff(recipient)) {
                        recipient.previousAttempt = new Date();
                        recipient.state = RecipientState.STATE_PROCESSING;
                        messageRecipientCount += 1;

                    } else {
                        Logger.info(String.format("Recipient %d still within cooling down period", recipient.id));
                    }
                }

                // If there are pendingRecipients, dispatch the entire task.
                if (messageRecipientCount > 0) {
                    // Update the task message in the database.
                    mTasksDao.saveMessage(message);

                    // Dispatch the message.
                    Logger.debug(String.format("Dispatching message %d", message.id));
                    mGcmMessageDispatcher.dispatchMessage(message, callback);

                } else {
                    // At least one recipient in the message is pending AND cooling off. Requeue message.
                    Logger.info(String.format("Only cooling down recipients ready in message %d. Requeuing", message.id));
                    addMessageToProducer(message);
                }

            } else {
                Logger.warn(String.format("Message %d has already finished and won't be dispatched.", message.id));
                mActiveMessages.remove(message.id);
                mListeners.remove(message.id);
            }

        } catch (TaskValidationException e) {
            Logger.error(String.format("Message %d invalid: %s", message.id, e.getMessage()));
            PlatformFailure failureDetails = new PlatformFailure(e.getMessage());
            failMessage(message, failureDetails);
        }
    }

    /**
     * Add a new, unsaved task to the TaskQueue.
     *
     * @param task     task to add to TaskQueue and dispatch.
     * @param callback TaskQueue callback to get processing updates..
     */
    public synchronized void queueTask(@Nonnull Task task, TaskQueueCallback callback) throws TaskValidationException {
        Logger.debug("Retrieved a task from the client to queue.");

        // Verify the Task has all required attributes.
        TaskHelper.verifyTask(task);

        // Clone the task avoiding overwriting races by client.
        Task clonedTask = TaskHelper.copyTask(task);

        if (!mTasksDao.saveTask(clonedTask)) {
            throw new TaskValidationException("Error saving Task. Check persistence settings.");
        }

        // Queue each message.
        for (Message message : clonedTask.messages) {

            // Queue the message if it is not active.
            if (!isMessageInQueue(message)) {

                // Hold the included client listener.
                if (callback != null) {
                    mListeners.put(message.id, callback);
                }
                addMessageToProducer(message);
            }
        }
    }

    /**
     * Add a message to the MessageQueue process.
     *
     * @param message Message to add to the queue.
     */
    private void addMessageToProducer(@Nonnull Message message) {
        // Add to processing messages collection.
        mActiveMessages.add(message.id);

        // Start the message queues if they are not already active.
        startProducerQueue();
        startConsumerQueue();

        // Add the message to the staging collection to be picked up by the consumer
        mQueueProducerThread.mStagedMessagesQueue.add(message);
    }

    /**
     * Fail all recipients in a message.
     *
     * @param message the {@link Message} to fail.
     */
    private void failMessage(@Nonnull Message message, PlatformFailure failureDetails) {
        if (message.recipients != null) {
            for (Recipient recipient : message.recipients) {
                recipient.state = RecipientState.STATE_FAILED;
                recipient.failure = failureDetails;
            }
        }
        mTasksDao.saveMessage(message);
    }

    /**
     * Ascertain whether the current message is being worked on or queued, and return true
     * if it is.
     *
     * @param message Message to check.
     * @return true if the message is currently active.
     */
    private boolean isMessageInQueue(@Nonnull Message message) {
        if (mActiveMessages.contains(message.id)) {
            Logger.debug(String.format("Message %d found in active process queue.", message.id));
            return true;

        } else {
            return false;
        }
    }

    /**
     * Response back from the push message push-services (APNS or GCM) for the sent message.
     * The message may have either succeeded or failed.
     */
    private class SortedPlatformTaskCallback extends SortedPlatformResponse {

        /**
         * A raw, unsorted callback for results returned for a message send from the push-services.
         *
         * @param message           The original message sent to the push-services
         * @param successRecipients The list of completed Recipients
         * @param failedRecipients  The map of failed Recipients with the Failure.
         * @param recipientsToRetry The list of recipients to exponentially retry.
         **/
        @Override
        public void messageSuccess(@Nonnull Message message, @Nonnull List<Recipient> successRecipients,
                                   @Nonnull List<FailedRecipient> failedRecipients,
                                   @Nonnull List<UpdatedRecipient> recipientsToUpdate,
                                   @Nonnull List<FailedRecipient> recipientsToRetry) {
            super.messageSuccess(message, successRecipients, failedRecipients, recipientsToUpdate, recipientsToRetry);
            Logger.debug("messageResult() invoked from push-services provider.");

            // Prematurely remove the active message internally to stop a race.
            mActiveMessages.remove(message.id);

            if (message.recipients != null) {
                TaskQueueCallback messageCallback = mListeners.get(message.id);

                // Save the message.
                if (!mTasksDao.saveMessage(message)) {
                    Logger.error("Failed to persist MessageResult message.");
                }

                try {
                    // Client responses:
                    if (messageCallback != null) {
                        Message returnMessage = (Message) message.clone();

                        // Invoke updatedRecipients() callback.
                        if (!recipientsToUpdate.isEmpty()) {
                            messageCallback.updatedRecipients(recipientsToUpdate);
                        }

                        // Invoke individual recipient failure callback.
                        if (!failedRecipients.isEmpty()) {
                            messageCallback.failedRecipients(failedRecipients);
                        }

                        // Invoke messageCompleted() callback.
                        if (!successRecipients.isEmpty() && recipientsToRetry.isEmpty()) {
                            messageCallback.messageCompleted(returnMessage);
                        }
                    }

                    // Re-queue the message to be dispatched if there are still retry recipients.
                    if (recipientsToRetry.isEmpty()) {
                        Logger.debug(String.format("Message %d has finished.", message.id));
                        mListeners.remove(message.id);

                    } else {
                        Logger.debug(String.format("Message %d has pending recipients - re-queueing message", message.id));
                        addMessageToProducer(message);
                    }

                } catch (CloneNotSupportedException e) {
                    Logger.error("Failed to clone pushservices model before invoking client listener.");
                }
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, PlatformFailure platformFailure) {
            super.messageFailure(message, platformFailure);
            Logger.error(String.format("Unrecoverable error response from provider for message %1$s - %2$s)",
                    message.id, platformFailure.failureMessage));

            // Update the message entry.
            if (!mTasksDao.saveMessage(message)) {
                Logger.error("Failed to persist MessageResult message.");
            }

            TaskQueueCallback messageCallback = mListeners.get(message.id);

            if (messageCallback != null) {
                messageCallback.messageFailed(message, platformFailure);
            }

            // Remove the listener if the task has not fully finished.
            mActiveMessages.remove(message.id);
            mListeners.remove(message.id);
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * tasks to the platform push provider. This will block if there are no tasks to take,
     * hence the Runnable.
     */
    private class MessageProducerThread extends Thread {
        private BlockingQueue<Message> mMessageDispatchQueue;
        BlockingQueue<Message> mStagedMessagesQueue = new ArrayBlockingQueue<>(2500);

        MessageProducerThread(BlockingQueue<Message> blockingQueue) {
            mMessageDispatchQueue = blockingQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Message waitingMessage = mStagedMessagesQueue.take();
                    Logger.info("Adding item from Producer into MessageQueue");

                    mMessageDispatchQueue.offer(waitingMessage);

                    // Sleep on while so often so the server isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                } catch (InterruptedException e) {
                    Logger.warn("InterruptedException was invoked in the MessageProducerThread");
                }
            }
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * tasks to the platform push provider. This will block if there are no tasks to take,
     * hence the Runnable.
     */
    private class MessageConsumerThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    // Take and remove the task from queue.
                    Message message = mMessageProcessQueue.take();
                    Logger.debug(String.format("TaskQueue processing queued message %d", message.id));

                    // Dispatch the queued message.
                    dispatchMessage(message, new SortedPlatformTaskCallback());

                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);
                }

            } catch (InterruptedException e) {
                Logger.warn("InterruptedException was invoked in the MessageConsumeThread");
            }
        }
    }
}
