package main.pushservices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * CompletableFuture for when the application lifecycle has initiated the shutdown.
 */
@Singleton
public class ApplicationLifecycleListener implements Callable<CompletionStage<Boolean>> {
    private final TaskQueue mTaskQueue;
    private final EbeanServer mEbeanServer;

    @Inject
    public ApplicationLifecycleListener(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        mEbeanServer = ebeanServer;
        mTaskQueue = taskQueue;
        applicationLifecycle.addStopHook(this);
    }

    @Override
    public CompletionStage<Boolean> call() throws Exception {
        mEbeanServer.shutdown(true, true);
        mTaskQueue.shutdown();
        return CompletableFuture.completedFuture(true);
    }
}