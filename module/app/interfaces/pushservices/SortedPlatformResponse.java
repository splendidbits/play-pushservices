package interfaces.pushservices;

import enums.pushservices.Failure;
import enums.pushservices.RecipientState;
import helpers.pushservices.PlatformHelper;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 6/23/16 Splendid Bits.
 */
public abstract class SortedPlatformResponse implements PlatformResponse {

    @Override
    public void messageFailure(@Nonnull Message message, PlatformFailure platformFailure) {
        if (message.recipients != null) {
            for (Recipient recipient : message.recipients) {
                recipient.failure = platformFailure;
                recipient.state = RecipientState.STATE_FAILED;
            }
        }
    }

    @Override
    public void messageSuccess(@Nonnull Message message, @Nonnull List<Recipient> completedRecipients,
                                @Nonnull List<FailedRecipient> failedRecipients,
                                @Nonnull List<UpdatedRecipient> recipientsToUpdate,
                                @Nonnull List<FailedRecipient> recipientsToRetry) {

        // Set the sent time for the message
        message.sentTime = new Date();

        // Remove all message recipients;
        message.recipients = new CopyOnWriteArrayList<>();

        // Mark successful recipients as complete.
        for (Recipient completedRecipient : completedRecipients) {
            completedRecipient.state = RecipientState.STATE_COMPLETE;

            // Add recipient back to message.
            message.recipients.add(completedRecipient);
        }

        // If GCM is asking us to cool-off, mark those recipients as STATE_WAITING_RETRY.
        for (FailedRecipient retryRecipient : recipientsToRetry) {
            Recipient recipient = retryRecipient.getRecipient();
            recipient.state = RecipientState.STATE_WAITING_RETRY;

            // Set to failed if max count reached.
            if (recipient.sendAttemptCount >= message.maximumRetries) {
                recipient.state = RecipientState.STATE_FAILED;
                recipient.failure = new PlatformFailure(Failure.MESSAGE_REGISTRATIONS_MISSING,
                        PlatformHelper.getGcmFailureName(Failure.MESSAGE_REGISTRATIONS_MISSING), new Date());

            }

            // Add recipient back to message.
            message.recipients.add(recipient);
        }

        // Perform actions on failed recipients.
        for (FailedRecipient failedRecipient : failedRecipients) {
            Recipient recipient = failedRecipient.getRecipient();
            recipient.state = RecipientState.STATE_WAITING_RETRY;

            PlatformFailure platformFailure = failedRecipient.getFailure();
            platformFailure.recipient = recipient;

            // Bad Token Recipients missing, not installed, no such token, etc.
            if (Failure.RECIPIENT_NOT_REGISTERED.equals(platformFailure.failure) ||
                    (Failure.RECIPIENT_REGISTRATION_INVALID.equals(platformFailure.failure))) {
                recipient.state = RecipientState.STATE_FAILED;
                recipient.failure = platformFailure;

            } else {
                // Any other recipient error:
                recipient.state = RecipientState.STATE_FAILED;
                recipient.failure = platformFailure;
            }

            // Add recipient back to message.
            message.recipients.add(recipient);
        }
    }
}
