package helpers.pushservices;

import enums.pushservices.PlatformType;
import enums.pushservices.RecipientState;
import exceptions.pushservices.MessageValidationException;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.shaded.ahc.io.netty.util.internal.StringUtil;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Date;

/**
 * A set of general-purpose functions for {@link Message}s and Message children.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/6/16 Splendid Bits.
 */
public class MessageHelper {

    /**
     * Verifies a message and throws an exception on fatal errors with the message.
     *
     * @param message The message to validate.
     * @throws MessageValidationException fired when there are irrecoverable problems with the message.
     */
    public static void verifyMessage(Message message) throws MessageValidationException {
        if (message == null) {
            throw new MessageValidationException("message is null.");
        }

        if (message.getId() != null) {
            throw new MessageValidationException("message.id should not be set. A message and all children behave atomically.");
        }

        if (message.getCredentials() == null) {
            throw new MessageValidationException("message is missing a credentials model.");
        }

        if (message.getCredentials().getMessage() != null) {
            throw new MessageValidationException("credentials.message should not be set.");
        }

        if (message.getCredentials().getId() != null) {
            throw new MessageValidationException("credentials.id should not be set. A message and all children behave atomically.");
        }

        if (message.getCredentials().getPlatformType() == null) {
            throw new MessageValidationException("credentials is missing a PlatformType.");
        }

        if (message.getRecipients() == null || message.getRecipients().isEmpty()) {
            throw new MessageValidationException("message has no recipients. did you forget to add tokens using the builder?");
        }

        for (Recipient recipient : message.getRecipients()) {
            if (recipient.getId() != null) {
                throw new MessageValidationException("recipient.id should not be set. A message and all children behave atomically.");
            }

            if (recipient.getMessage() != null) {
                throw new MessageValidationException("recipient.message should not be set.");
            }

            if (StringUtil.isNullOrEmpty(recipient.getToken())) {
                throw new MessageValidationException(String.format("recipient %d has no device token.", recipient.getId()));
            }

            if (recipient.getPlatformFailure() != null) {
                throw new MessageValidationException("message must not have a PlatformFailure.");
            }
        }

        if (StringUtil.isNullOrEmpty(message.getCredentials().getAuthKey()) && StringUtil.isNullOrEmpty(message.getCredentials().getCertBody())) {
            throw new MessageValidationException("A Message's Credentials has no AuthorisationKey or CertificateBody.");
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
        return recipient.getState() == null ||
                (recipient.getState().equals(RecipientState.STATE_WAITING_RETRY) ||
                        recipient.getState().equals(RecipientState.STATE_PROCESSING) ||
                        recipient.getState().equals(RecipientState.STATE_IDLE));
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

        return recipient.getState() != null &&
                recipient.getState().equals(RecipientState.STATE_WAITING_RETRY) &&
                recipient.getNextAttempt() != null &&
                recipient.getNextAttempt().getTime() >= currentTime.getTime();
    }

    /**
     * Marks a recipient as cooling-off. The next cooling-off time will be set on a factor *2 compared
     * to the previous cooling off minutes wait.
     *
     * @param recipient recipient to mark as cooling off..
     */
    public static void setRecipientRetry(PlatformType platformType, @Nonnull Recipient recipient, int retryLimit) {
        if (recipient.getSendAttemptCount() >= retryLimit) {
            recipient.setState(RecipientState.STATE_FAILED);
            recipient.setNextAttempt(null);

        } else if (!recipient.getState().equals(RecipientState.STATE_WAITING_RETRY)) {
            int newRetryCount = recipient.getSendAttemptCount() + 1;
            Calendar nextSendDate = Calendar.getInstance();
            nextSendDate.add(Calendar.MINUTE, 2 * newRetryCount);

            recipient.setSendAttemptCount(newRetryCount);
            recipient.setState(RecipientState.STATE_WAITING_RETRY);
            recipient.setNextAttempt(nextSendDate.getTime());
        }
    }

    /**
     * Check to see if the message has completed (every recipient has failed or succeeded).
     */
    public static boolean hasMessageCompleted(@Nonnull Message message) {
        if (message.getRecipients() != null) {
            for (Recipient recipient : message.getRecipients()) {
                if (recipient.getState() != null &&
                        (recipient.getState().equals(RecipientState.STATE_PROCESSING) ||
                        recipient.getState().equals(RecipientState.STATE_WAITING_RETRY) ||
                        recipient.getState().equals(RecipientState.STATE_IDLE))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Fail all recipients in a message with a failure.
     *
     * @param message the {@link Message} to fail.
     */
    public static void failMessage(Message message, PlatformFailure failure) {
        if (message != null && message.getId() != null) {

            if (message.getRecipients() != null) {
                for (Recipient recipient : message.getRecipients()) {
                    recipient.setState(RecipientState.STATE_FAILED);
                    if (recipient.getPlatformFailure() == null && failure != null) {
                        recipient.setFailure(failure);
                    }
                }
            }
        }
    }


    /**
     * Returns true if any Message Recipients have states that have not failed or coompleted).
     *
     * @param message The message to check if ready to dispatch.
     * @return true if the message can be processed. false if it is invalid or has Completed..
     */
    public static boolean isMessageReady(@Nonnull Message message) throws MessageValidationException {
        int readyRecipients = 0;
        if (message.getRecipients() != null && !message.getRecipients().isEmpty()) {
            for (Recipient recipient : message.getRecipients()) {

                if (recipient.getToken() == null || recipient.getToken().isEmpty()) {
                    throw new MessageValidationException(String.format("No Recipient token for %d.", recipient.getId()));
                }

                if (isRecipientPending(recipient) && recipient.getSendAttemptCount() <= message.getMaximumRetries()) {
                    readyRecipients += 1;
                }
            }
        }
        return readyRecipients > 0;
    }

}
