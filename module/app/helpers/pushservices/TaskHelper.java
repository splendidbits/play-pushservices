package helpers.pushservices;

import enums.pushservices.RecipientState;
import exceptions.pushservices.TaskValidationException;
import models.pushservices.db.Message;
import models.pushservices.db.Recipient;
import models.pushservices.db.Task;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Date;

/**
 * A set of general-purpose functions for {@link Task}s and Task children.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/6/16 Splendid Bits.
 */
public class TaskHelper {

    /**
     * Copies a task and throws an exception on fatal errors with the task.
     *
     * @param task The task to validate.
     * @throws TaskValidationException fired when there are irrecoverable problems with the task.
     */
    public static Task copyTask(@Nonnull Task task) throws TaskValidationException {
        try {
            return (Task) task.clone();

        } catch (CloneNotSupportedException e) {
            throw new TaskValidationException(e.getMessage());
        }
    }

    /**
     * Verifies a task and throws an exception on fatal errors with the task.
     *
     * @param task The task to validate.
     * @throws TaskValidationException fired when there are irrecoverable problems with the task.
     */
    public static void verifyTask(Task task) throws TaskValidationException {

        if (task == null) {
            throw new TaskValidationException("Task is null.");
        }

        // Check that the task has pending recipients in all messages.
        doesTaskContainPendingRecipients(task);

        for (Message message : task.messages) {
            if (message.credentials == null) {
                throw new TaskValidationException("A Task Message is missing a Credentials model.");

            } else if (message.credentials.platformType == null) {
                throw new TaskValidationException("A Message's Credentials is missing a PlatformType.");
            }

            boolean containsAuthorisationKey = message.credentials.authKey != null
                    && !message.credentials.authKey.isEmpty();

            boolean containsCertificateBody = message.credentials.certBody != null
                    && !message.credentials.certBody.isEmpty();

            for (Recipient recipient : message.recipients) {
                if (recipient.token == null || recipient.token.isEmpty()) {
                    throw new TaskValidationException(String.format("Recipient %d has no provider token.", recipient.id));
                }
            }

            if (task.id != null) {
                // Throw exception if the task already contains an primary id.
                throw new RuntimeException("Task.id must contain a null value.");
            }

            if (!containsAuthorisationKey && !containsCertificateBody) {
                throw new TaskValidationException("A Message's Credentials has no AuthorisationKey or CertificateBody.");
            }

            // Ensure  push_services models do not have client-created ids.
            if (message.id != null) {
                throw new RuntimeException("Message.id must contain a null value.");
            }

            if (message.credentials.id != null) {
                throw new RuntimeException("Credentials.id must contain a null value.");

            }
            for (Recipient recipient : message.recipients) {
                if (recipient.id != null) {
                    throw new RuntimeException("Recipient.id must contain a null value.");
                }

                if (recipient.failure != null && recipient.failure.id != null) {
                    throw new RuntimeException("RecipientFailure.id must contain a null value.");
                }
            }
        }
    }

    /**
     * Returns true if a recipient is ready to send a message. (not in backoff and is in
     * a non-complete send state).
     *
     * @param recipient recipient to check.
     * @return true if the recipient is ready to be included in a platform message.
     */
    public static boolean isRecipientPending(@Nonnull Recipient recipient) {
        return (recipient.state == null ||
                (recipient.state.equals(RecipientState.STATE_WAITING_RETRY) ||
                        recipient.state.equals(RecipientState.STATE_IDLE) ||
                        recipient.state.equals(RecipientState.STATE_PROCESSING)));
    }

    /**
     * Returns true if the recipient is within the cool-down backoff period, and is
     * not yet ready to be included in a message dispatch.
     *
     * @param recipient recipient to check.
     * @return true if the recipient is still within the backoff period. False if the
     * recipient is ready to be included in message.
     */
    public static boolean isRecipientCoolingOff(@Nonnull Recipient recipient) {
        Date currentTime = new Date();

        return recipient.state != null &&
                recipient.state == RecipientState.STATE_WAITING_RETRY &&
                recipient.nextAttempt != null &&
                recipient.nextAttempt.getTime() > currentTime.getTime();
    }

    /**
     * Marks a recipient as cooling-off. The next cooling-off time will be logarithmically
     * set compared to the current cooling off time.
     *
     * @param recipient recipient to mark as cooling off..
     */
    public static void setRecipientCoolingOff(@Nonnull Recipient recipient, @Nonnull Calendar retryAfterDate) {
        recipient.sendAttemptCount += 1;
        recipient.nextAttempt = retryAfterDate.getTime();
    }

    /**
     * Returns true if all {@link Task} Messages are ready to be dispatched (All Message
     * Recipients have states that have not failed or taskCompleted).
     *
     * @param task The task to check if ready to dispatch.
     * @return true if the task can be processed. false if it is invalid or has taskCompleted..
     */
    public static boolean doesTaskContainPendingRecipients(@Nonnull Task task) throws TaskValidationException {
        if (task.messages != null && !task.messages.isEmpty()) {
            for (Message message : task.messages) {

                if (message.recipients == null || message.recipients.isEmpty()) {
                    throw new TaskValidationException("Message does not contain any recipients.");
                }

                // Ensure there is at least one pending recipient.
                isMessageProcessReady(message);
            }
        } else {
            throw new TaskValidationException("Task does not contain any messages.");
        }
        return false;
    }

    /**
     * Returns true if the {@link Message} is ready to be dispatched (All Message Recipients have
     * states that have not failed or taskCompleted).
     *
     * @param message The message to check if ready to dispatch.
     * @return true if the task can be processed. false if it is invalid or has taskCompleted..
     */
    public static boolean isMessageProcessReady(@Nonnull Message message) throws TaskValidationException {
        if (message.recipients != null && !message.recipients.isEmpty()) {
            for (Recipient recipient : message.recipients) {

                if (recipient.token == null || recipient.token.isEmpty()) {
                    throw new TaskValidationException(String.format("No Recipient token for %d.", recipient.id));
                }
                if (isRecipientPending(recipient)) {
                    return true;
                }
            }
        }
        return false;
    }

}
