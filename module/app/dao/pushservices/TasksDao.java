package dao.pushservices;

import annotations.pushservices.PushServicesEbeanServer;
import enums.pushservices.RecipientState;
import io.ebean.EbeanServer;
import io.ebean.OrderBy;
import models.pushservices.db.Message;
import models.pushservices.db.Task;
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;

/**
 * Task Message and Queue persistence methods.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/10/16 Splendid Bits.
 */
public class TasksDao {
    public final EbeanServer mEbeanServer;

    @Inject
    public TasksDao(@PushServicesEbeanServer EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    /**
     * Inserts a task in the TaskQueue datastore, and cascades through
     * children.
     *
     * @param task the task to update.
     * @return true if the task was updated.
     */
    public boolean insertTask(@Nonnull Task task) {
        try {
            mEbeanServer.markAsDirty(task);
            mEbeanServer.insert(task);
            return true;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error inserting tasks model into database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error inserting new task", e);
        }
        return false;
    }

    /**
     * Deletes a task and all child entities from the database
     *
     * @param taskId taskId to delete.
     * @return boolean of result.
     */
    public boolean deleteTask(long taskId) {
        try {
            Task task = mEbeanServer.find(Task.class)
                    .fetch("messages")
                    .fetch("messages.recipients")
                    .fetch("messages.recipients.failure")
                    .fetch("messages.credentials")
                    .fetch("messages.payloadData")
                    .where()
                    .idEq(taskId)
                    .findOne();

            if (task != null) {
                mEbeanServer.deletePermanent(task);
                return true;
            }

        } catch (PersistenceException e) {
            Logger.error(String.format("Error deleting task model from database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error deleting task", e);
        }
        return false;
    }

    /**
     * Get a collection of tasks matching a task name.
     *
     * @param taskName name of task to find.
     * @return a collection of tasks, or null
     */
    @Nullable
    public List<Task> findTasks(@Nonnull String taskName) {
        try {
            return mEbeanServer.find(Task.class)
                    .fetch("messages")
                    .fetch("messages.recipients")
                    .fetch("messages.recipients.failure")
                    .fetch("messages.credentials")
                    .fetch("messages.payloadData")
                    .where()
                    .eq("name", taskName)
                    .findList();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error deleting task model from database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error deleting task", e);
        }
        return null;
    }

    /**
     * Saves or updates a message in the TaskQueue database.
     *
     * @param message the message to update.
     * @return true if the message was updated.
     */
    public boolean updateMessage(@Nonnull Message message) {
        mEbeanServer.markAsDirty(message);

        try {
            mEbeanServer.save(message);
            return true;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error updating tasks model into database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error updating task message", e);
        }
        return false;
    }

    /**
     * Get a list of all {@link Task}s from the database which contains recipients who
     * have not yet fully taskCompleted the push lifecycle. Do not filter out message properties,
     * as we want tasks and messages to remain completely unadulterated, as a later .save
     * will need to persist the entire bean children.
     */
    @Nonnull
    public List<Task> fetchPendingTasks() {
        List<Task> pendingTasks = new ArrayList<>();

        try {
            OrderBy<Task> priorityOrder = new OrderBy<>("priority");
            priorityOrder.reverse();

            pendingTasks = mEbeanServer.createQuery(Task.class)
                    .setOrderBy(priorityOrder)
                    .fetch("messages")
                    .fetch("messages.recipients")
                    .fetch("messages.recipients.failure")
                    .fetch("messages.credentials")
                    .fetch("messages.payloadData")
                    .where()
                    .disjunction()
                    .eq("messages.recipients.state", RecipientState.STATE_IDLE)
                    .eq("messages.recipients.state", RecipientState.STATE_PROCESSING)
                    .eq("messages.recipients.state", RecipientState.STATE_WAITING_RETRY)
                    .endJunction()
                    .findList();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching tasks models from database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error fetching message tasks.", e);
        }
        return pendingTasks;
    }

}
