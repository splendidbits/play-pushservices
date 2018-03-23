package injection.pushservices.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import main.pushservices.PushLifecycleListener;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

/**
 * CompletableFuture for when the application lifecycle has initiated the shutdown.
 */
@Singleton
public class PushLifecycleListenerProvider implements Provider<PushLifecycleListener> {
    private final PushLifecycleListener mLifecycleListener;

    @Inject
    public PushLifecycleListenerProvider(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        mLifecycleListener = new PushLifecycleListener(ebeanServer, applicationLifecycle, taskQueue);
    }

    @Override
    public PushLifecycleListener get() {
        return mLifecycleListener;
    }
}
