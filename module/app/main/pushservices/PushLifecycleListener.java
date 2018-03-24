package main.pushservices;

import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

import java.util.concurrent.CompletableFuture;

public class PushLifecycleListener {
    public PushLifecycleListener(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        applicationLifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
            ebeanServer.shutdown(true, false);
            taskQueue.shutdown();
        }));
    }
}
