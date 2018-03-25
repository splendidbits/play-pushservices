package injection.pushservices.modules;

import annotations.pushservices.PushServicesEbeanServer;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import injection.pushservices.providers.PushServicesEbeanServerProvider;
import io.ebean.EbeanServer;
import main.pushservices.PushLifecycleListener;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 02/01/2017 Splendid Bits.
 */
public class PushServicesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EbeanServer.class)
                .annotatedWith(PushServicesEbeanServer.class)
                .toProvider(PushServicesEbeanServerProvider.class)
                .in(Singleton.class);

        bind(PushLifecycleListener.class)
                .asEagerSingleton();
    }
}
