package interfaces.pushservices;

import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Callbacks for actions that should be taken by the client on response from the
 * Google GCM server.
 * <p>
 * Those include deleting a stale recipient such as (when they are not registered),
 * or when the {@link Recipient} information has changed and the client should update it.
 */
public interface TaskQueueCallback {

    /**
     * Invoked with a collection of updated {@link Recipient} registrations.
     *
     * @param updatedRegistrations A an old and new {@link Recipient} in a Map of stale and updated
     *                             registrations s in the format:
     *                             <Registration staleRegistration, Registration updatedRegistration>
     */
    void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRegistrations);

    /**
     * Notification of a failure for a particular message recipient. This does not mean
     * the message as a whole has fully completed or failed yet.
     *
     * @param failedRecipients Recipients that a message was undeliverable to.
     */
    void failedRecipients(@Nonnull List<FailedRecipient> failedRecipients);

    /**
     * All recipients in a Message were processed and sent, but some recipients may
     * still have failures.
     *
     * @param originalMessage The taskCompleted Task.
     */
    void messageCompleted(@Nonnull Message originalMessage);

    /**
     * All recipients in a message failed due to an unrecoverable failure, and
     * will not be retried.
     *
     * @param originalMessage Message which failed.
     * @param failure         Details about the failure of all recipients.
     */
    void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure);
}