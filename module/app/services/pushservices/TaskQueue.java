package services.pushservices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.pushservices.MessagesDao;
import enums.pushservices.RecipientState;
import exceptions.pushservices.MessageValidationException;
import helpers.pushservices.MessageHelper;
import interfaces.pushservices.PlatformResponse;
import interfaces.pushservices.TaskQueueListener;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A singleton class that handles all Push Service message jobs.
 */
@Singleton
public class TaskQueue {
    // Delay each message polling interval to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 500;

    // Collection containing all messages that have not returned from the provider with a success or fail.
    private Set<Long> mActiveMessages = new HashSet<>();
    private Map<Long, PlatformResponseCallback> mInternalListeners = new HashMap<>();
    private Map<Long, TaskQueueListener> mExternalListeners = new HashMap<>();

    // TaskQueue thread and queue related members.
    private BlockingQueue<Message> mMessageProcessQueue = new ArrayBlockingQueue<>(5000);
    private MessageProducerThread mQueueProducerThread;
    private MessageConsumerThread mQueueConsumerThread;

    private GcmMessageDispatcher mGcmMessageDispatcher;
    private MessagesDao mMessagesDao;

    /**
     * Privately instantiate the TaskQueue with required Dependencies.
     *
     * @param messagesDao          Message persistence.
     * @param gcmMessageDispatcher GCM Google message dispatcher.
     */
    @Inject
    protected TaskQueue(MessagesDao messagesDao, GcmMessageDispatcher gcmMessageDispatcher) {
        mMessagesDao = messagesDao;
        mGcmMessageDispatcher = gcmMessageDispatcher;
    }

    @SuppressWarnings("unused")
    private TaskQueue() {
    }

    /**
     * Checks that the message consumer process is active and running, and starts the
     * TaskQueue {@link Message} polling process if it is not.
     */
    public synchronized void startup() {
        Logger.info("TaskQueue Startup");

        // Start the message producer and consumer queues.
        startProducerQueue();
        startConsumerQueue();

        // Check and re-queue existing pending messages.
        queuePendingMessages();
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
     * Checks that the message ask consumer process is active and running, and starts the
     * TaskQueue {@link Message} polling process if it is not.
     */
    public synchronized void shutdown() {
        if (mQueueProducerThread != null) {
            Logger.debug("Shutting down the TaskQueue Producer process.");
            mQueueProducerThread.interrupt();
        }

        if (mQueueConsumerThread != null) {
            Logger.debug("Shutting down the TaskQueue Consumer process.");
            mQueueConsumerThread.interrupt();
        }
    }

    /**
     * Queue any messages that have pending recipients. If you call this method, be sure that the messages
     * in question are not being processed by the ConsumerThread, or are awaiting results
     * from the dispatcher. Ideally, the TaskQueue should not be started.
     */
    private void queuePendingMessages() {
        // Get outstanding incomplete message messages and startup queue producer thread.
        List<Message> pendingMessages = mMessagesDao.fetchPendingMessages();
        Logger.info(String.format("Pending Message check. Active queue: %d.", pendingMessages.size()));

        for (Message message : pendingMessages) {
            if (!isMessageInQueue(message)) {
                queueMessage(message);
            }
        }
    }

    /**
     * Add new messages to the TaskQueue.
     *
     * @param messages message to add to TaskQueue and dispatch.
     * @param callback TaskQueue callback to get processing updates..
     */
    public synchronized void queueMessages(@Nonnull List<Message> messages, TaskQueueListener callback) throws MessageValidationException {
        Logger.debug("Retrieved a message from the client to queue.");

        for (Message message : messages) {
            // Verify the Message has all required attributes.
            MessageHelper.verifyMessage(message);

            if (!mMessagesDao.saveMessage(message)) {
                throw new MessageValidationException("Error saving message. Check persistence settings.");
            }

            // Add client TaskQueue listener.
            if (mExternalListeners.get(message.getId()) == null && callback != null) {
                mExternalListeners.put(message.getId(), callback);
            }

            // Queue the message if it is not active.
            if (!isMessageInQueue(message)) {
                queueMessage(message);
            }
        }
    }

    /**
     * Add a message to the MessageQueue process.
     *
     * @param message Message to add to the queue.
     */
    private void queueMessage(Message message) {
        if (message != null && message.getId() != null) {
            // Add to processing messages collection.
            mActiveMessages.add(message.getId());

            //  Add the internal provider dispatcher listener.
            if (mInternalListeners.get(message.getId()) == null) {
                mInternalListeners.put(message.getId(), new PlatformResponseCallback());
            }

            // Start the message queues if they are not already active and add to staging collection
            // to be picked up by the consumer
            startProducerQueue();
            startConsumerQueue();
            mQueueProducerThread.dispatchQueue.add(message);
        }
    }

    private void removeMessageFromQueue(Message message) {
        if (message != null && message.getId() != null) {
            mActiveMessages.remove(message.getId());
            mMessageProcessQueue.remove(message);

            mInternalListeners.remove(message.getId());
            mExternalListeners.remove(message.getId());
        }
    }

    /**
     * Fail all recipients in a message.
     *
     * @param message the {@link Message} to fail.
     */
    private void failMessage(Message message, PlatformFailure failureDetails) {
        if (message != null && message.getId() != null) {

            if (message.getRecipients() != null) {
                for (Recipient recipient : message.getRecipients()) {
                    recipient.setState(RecipientState.STATE_FAILED);
                    recipient.setFailure(failureDetails);
                }
            }
            mMessagesDao.saveMessage(message);
            removeMessageFromQueue(message);
        }
    }

    /**
     * Ascertain whether the current message is being worked on or queued, and return true
     * if it is.
     *
     * @param message Message to check.
     * @return true if the message is currently active.
     */
    private boolean isMessageInQueue(@Nonnull Message message) {
        if (mActiveMessages.contains(message.getId())) {
            Logger.debug(String.format("Message %d found in active process queue.", message.getId()));
            return true;

        } else {
            return false;
        }
    }

    /**
     * Dispatch a Message {@link Message}
     *
     * @param message The {@link Message} to dispatch.
     */
    private void dispatchMessage(@Nonnull Message message) {
        int messageRecipientCount = 0;

        try {
            if (!MessageHelper.isMessageReady(message)) {
                Logger.warn(String.format("Message %d has already finished and won't be dispatched.", message.getId()));
                failMessage(message, new PlatformFailure("Message failed on dispatch"));
                return;
            }

        } catch (MessageValidationException e) {
            Logger.error(String.format("Message %d invalid: %s", message.getId(), e.getMessage()));
            failMessage(message, new PlatformFailure(e.getMessage()));
            return;
        }

        // Set recipient states to processing for ready recipients.
        for (Recipient recipient : message.getRecipients()) {
            // The recipient is out of the cooling off period.
            if (!MessageHelper.isRecipientCoolingOff(recipient)) {
                recipient.setState(RecipientState.STATE_PROCESSING);
                recipient.setLastSendAttempt(new Date());
                messageRecipientCount += 1;
            } else {
                Logger.debug(String.format("Recipient %d still within cooling down period", recipient.getId()));
            }
        }

        // If there are pendingRecipients, dispatch the message.
        if (messageRecipientCount > 0) {
            if (!mMessagesDao.saveMessage(message)) {
                removeMessageFromQueue(message);
                return;
            }

            PlatformResponseCallback platformResponse = mInternalListeners.get(message.getId());
            if (platformResponse == null) {
                platformResponse = new PlatformResponseCallback();
                mInternalListeners.put(message.getId(), platformResponse);
            }

            // Dispatch the message.
            Logger.debug(String.format("Dispatching message %d", message.getId()));
            mGcmMessageDispatcher.dispatchMessage(message, platformResponse);

        } else {
            // At least one recipient in the message is pending AND cooling off. Requeue message.
            Logger.debug(String.format("Only cooling down recipients ready in message %d. Requeuing", message.getId()));
            queueMessage(message);
        }
    }

    /*
     * Response back from the push message push-services (APNS or GCM) for the sent message.
     * The message may have either succeeded or failed.
     */
    private class PlatformResponseCallback implements PlatformResponse {
        /**
         * A raw, unsorted callback for results returned for a message send from the push-services.
         *
         * @param message           The original message sent to the push-services
         * @param successRecipients The list of completed Recipients
         * @param failedRecipients  The map of failed Recipients with the Failure.
         * @param recipientsToRetry The list of recipients to exponentially retry.
         **/
        @Override
        public void messageSuccess(@Nonnull Message message, @Nonnull List<Recipient> successRecipients, @Nonnull List<Recipient> failedRecipients,
                                   @Nonnull List<UpdatedRecipient> recipientsToUpdate, @Nonnull List<Recipient> recipientsToRetry) {
            if (message.getRecipients() != null) {
                // Save the message.
                if (!mMessagesDao.saveMessage(message)) {
                    Logger.error("Failed to persist MessageResult message.");
                    return;
                }

                // Client responses:
                TaskQueueListener messageCallback = mExternalListeners.get(message.getId());
                if (messageCallback != null) {
                    // Invoke updatedRecipients() callback.
                    if (!recipientsToUpdate.isEmpty()) {
                        Logger.debug(String.format("[%d] recipients requiring token change", recipientsToUpdate.size()));
                        messageCallback.updatedRecipients(recipientsToUpdate);
                    }

                    // Invoke individual recipient failure callback.
                    if (!failedRecipients.isEmpty()) {
                        Logger.debug(String.format("[%d] failed recipients", failedRecipients.size()));
                        messageCallback.failedRecipients(failedRecipients);
                    }

                    // Invoke messageCompleted() callback.
                    if (!successRecipients.isEmpty() && recipientsToRetry.isEmpty()) {
                        Logger.debug(String.format("[%d] successful recipients", successRecipients.size()));
                        messageCallback.messageCompleted(message);
                    }
                }

                // Re-queue the message to be dispatched if there are still retry recipients.
                if (recipientsToRetry.isEmpty()) {
                    Logger.info(String.format("Message %d completed.", message.getId()));
                    removeMessageFromQueue(message);

                } else {
                    Logger.debug(String.format("Message %d has pending recipients - Requeueing message", message.getId()));
                    mActiveMessages.remove(message.getId());
                    queueMessage(message);
                }
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, @Nonnull PlatformFailure failure) {
            Logger.error(String.format("Platform error '%1$s' from provider for message %2$d",
                    failure.getFailureType().name(), message.getId()));

            // Update the message entry.
            if (!mMessagesDao.saveMessage(message)) {
                Logger.error("Failed to persist MessageResult message.");
                return;
            }

            TaskQueueListener messageCallback = mExternalListeners.get(message.getId());
            if (messageCallback != null) {
                messageCallback.messageFailed(message, failure);
            }

            removeMessageFromQueue(message);
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * messages to the platform push provider. This will block if there are no messages to take,
     * hence the Runnable.
     */
    private class MessageProducerThread extends Thread {
        private BlockingQueue<Message> dispatchQueue;

        MessageProducerThread(BlockingQueue<Message> blockingQueue) {
            dispatchQueue = blockingQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Message waitingMessage = dispatchQueue.take();
                    Logger.debug("Adding item from Producer into MessageQueue");

                    mMessageProcessQueue.offer(waitingMessage);

                    // Sleep on while so often so the server isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                } catch (InterruptedException e) {
                    Logger.debug("InterruptedException was invoked in the MessageProducerThread");
                }
            }
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * messages to the platform push provider. This will block if there are no messages to take,
     * hence the Runnable.
     */
    private class MessageConsumerThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    // Take and remove the task from queue.
                    Message message = mMessageProcessQueue.take();
                    Logger.debug(String.format("TaskQueue processing queued message %d", message.getId()));

                    // Dispatch the queued message.
                    dispatchMessage(message);
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);
                }

            } catch (InterruptedException e) {
                Logger.debug("InterruptedException was invoked in the MessageConsumeThread");
            }
        }
    }
}
