package main.pushservices;

import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import scala.concurrent.duration.Duration;
import services.pushservices.TaskQueue;

import java.util.concurrent.TimeUnit;


/**
 * Starts scheduled tasks such as agency alert feed downloads, and periodic taskqueue
 * consumer verification. Runs on application startup.
 */
@Singleton
public class TaskQueueScheduler {
    private static final int TASKQUEUE_START_WARM_UP_SECONDS = 5;
    private static final int TASKQUEUE_CHECK_RUNNING_SECONDS = 60;

    @Inject
    public TaskQueueScheduler(ActorSystem actorSystem, TaskQueue taskQueue) {
        // Periodically verify that the TaskQueue Queue consumer is up and running, if not, restart it.
        actorSystem.scheduler().schedule(Duration.create(TASKQUEUE_START_WARM_UP_SECONDS, TimeUnit.SECONDS),
                Duration.create(TASKQUEUE_CHECK_RUNNING_SECONDS, TimeUnit.SECONDS),
                taskQueue::startup,
                actorSystem.dispatchers().defaultGlobalDispatcher());
    }
}
