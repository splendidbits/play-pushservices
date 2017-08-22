package injection.pushservices.providers;

import annotations.pushservices.PushServicesEbeanServer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import main.pushservices.ApplicationLifecycleListener;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 03/01/2017 Splendid Bits.
 */
@Singleton
public class PushApplicationLifecycleProvider implements Provider<ApplicationLifecycleListener> {
    private ApplicationLifecycleListener mApplicationLifecycleListener;

    @Inject
    public PushApplicationLifecycleProvider(@PushServicesEbeanServer EbeanServer ebeanServer,
                                            ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        mApplicationLifecycleListener = new ApplicationLifecycleListener(ebeanServer, applicationLifecycle, taskQueue);
    }

    @Override
    public ApplicationLifecycleListener get() {
        return mApplicationLifecycleListener;
    }
}
