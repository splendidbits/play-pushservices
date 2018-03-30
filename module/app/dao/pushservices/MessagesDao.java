package dao.pushservices;

import annotations.pushservices.PushServicesEbeanServer;
import enums.pushservices.RecipientState;
import io.ebean.EbeanServer;
import io.ebean.OrderBy;
import models.pushservices.db.Message;
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Message persistence.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/10/16 Splendid Bits.
 */
public class MessagesDao {
    private final EbeanServer mEbeanServer;

    @Inject
    public MessagesDao(@PushServicesEbeanServer EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    /**
     * Delete everything!
     * @return success boolean
     */
    public boolean wipeAll() {
        try {
            List<Message> messages = mEbeanServer.find(Message.class).findList();
            mEbeanServer.deleteAllPermanent(messages);

            return true;
        } catch (Exception e) {
            Logger.error(String.format("Error wiping all data rom database: %s.", e.getMessage()));
        }
        return false;
    }

    /**
     * Deletes a message and all child entities from the database
     *
     * @param messageId messageId to delete.
     * @return boolean of result.
     */
    public boolean deleteMessage(long messageId) {
        try {
            List<Message> messages = mEbeanServer.find(Message.class)
                    .fetch("recipients")
                    .fetch("recipients.failure")
                    .fetch("credentials")
                    .fetch("payloadData")
                    .where()
                    .idEq(messageId)
					.findList();

            if (!messages.isEmpty()) {
                mEbeanServer.deleteAllPermanent(messages);
                return true;
            }

        } catch (Exception e) {
            Logger.error(String.format("Error deleting messages %s.", e.getMessage()));
        }
        return false;
    }

    @Nullable
    public List<Message> fetchMessages() {
        try {
            return mEbeanServer.find(Message.class)
                    .fetch("recipients")
                    .fetch("recipients.failure")
                    .fetch("credentials")
                    .fetch("payloadData")
                    .findList();

        } catch (Exception e) {
            Logger.error(String.format("Error finding messages in the database: %s.", e.getMessage()));
        }
        return null;
    }

    /**
     * Saves or updates a message in the TaskQueue database.
     *
     * @param message the message to update.
     * @return true if the message was updated.
     */
    public boolean saveMessage(@Nonnull Message message) {
        try {
            mEbeanServer.save(message);
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error saving/updating messages into database: %s.", e.getMessage()));
        }
        return false;
    }

    /**
     * Get a list of all {@link Message}s from the database which contains recipients who
     * have not yet fully taskCompleted the push lifecycle. Do not filter out message properties,
     * as we want them to remain completely unadulterated, as a later .save()
     * will need to persist the entire bean children.
     */
    @Nonnull
    public List<Message> fetchPendingMessages() {
        List<Message> pendingMessages = new ArrayList<>();

        try {
            OrderBy<Message> order = new OrderBy<>("id");
            order.reverse();

            pendingMessages = mEbeanServer.createQuery(Message.class)
                    .setOrderBy(order)
                    .fetch("recipients")
                    .fetch("recipients.failure")
                    .fetch("credentials")
                    .fetch("payloadData")
                    .where()
                    .disjunction()
                    .eq("recipients.state", RecipientState.STATE_IDLE)
                    .eq("recipients.state", RecipientState.STATE_PROCESSING)
                    .eq("recipients.state", RecipientState.STATE_WAITING_RETRY)
                    .endJunction()
                    .findList();

        } catch (Exception e) {
            Logger.error(String.format("Error fetching pending messages %s.", e.getMessage()));
        }
        return pendingMessages;
    }

}
