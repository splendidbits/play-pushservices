package interfaces.pushservices;

import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Response back from the push message push-services (APNS or GCM) for the sent message.
 * The message may have either succeeded or failed.
 * <p>
 * If the message failed, it was a "hard, unrecoverable" error, meaning that the message,
 * and all of it's recipients have been flagged as "FAILED'. If the message is the only message within
 * the task, then the task itself will also be flagged as failed.
 * <p>
 * If the message was processed by the push-services, then the message has somewhat succeeded. As you notice,
 * the interface method is not called "messageSuccess", as the Recipients returned may be a mixture of
 * success, failures, and retries.
 */
public interface PlatformResponse {

    /**
     * A callback for an unrecoverable Platform message dispatch failure.
     *
     * @param message         The original message send to the push-services
     * @param platformFailure The Message Failure.
     */
    void messageFailure(@Nonnull Message message, PlatformFailure platformFailure);

    /**
     * A raw, unsorted callback for results returned for a message send from the push-services.
     *
     * @param message             The original message sent to the push-services
     * @param completedRecipients The list of completed Recipients
     * @param failedRecipients    The map of failed Recipients with the Failure.
     * @param recipientsToRetry   The list of recipients to exponentially retry.
     **/
    void messageSuccess(@Nonnull Message message, @Nonnull List<Recipient> completedRecipients,
                        @Nonnull List<FailedRecipient> failedRecipients,
                        @Nonnull List<UpdatedRecipient> recipientsToUpdate,
                        @Nonnull List<FailedRecipient> recipientsToRetry);
}