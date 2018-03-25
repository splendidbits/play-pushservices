package main.pushservices;

import annotations.pushservices.PushServicesEbeanServer;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class PushLifecycleListener {

    @Inject
    public PushLifecycleListener(@PushServicesEbeanServer EbeanServer ebeanServer, ApplicationLifecycle lifecycle, TaskQueue taskQueue) {
        taskQueue.startup();
        lifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
            ebeanServer.shutdown(true, false);
            taskQueue.shutdown();
        }));
    }
}
