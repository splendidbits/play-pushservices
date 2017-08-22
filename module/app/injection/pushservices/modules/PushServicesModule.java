package injection.pushservices.modules;

import annotations.pushservices.PushServicesEbeanServer;
import injection.pushservices.providers.PushApplicationLifecycleProvider;
import injection.pushservices.providers.PushServicesEbeanServerProvider;
import io.ebean.EbeanServer;
import main.pushservices.ApplicationLifecycleListener;
import main.pushservices.TaskQueueScheduler;
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 02/01/2017 Splendid Bits.
 */
public class PushServicesModule extends Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {

        Binding<EbeanServer> ebeanBinding = bind(EbeanServer.class)
                .qualifiedWith(PushServicesEbeanServer.class)
                .toProvider(PushServicesEbeanServerProvider.class)
                .eagerly();

        Binding<ApplicationLifecycleListener> lifecycleBinding = bind(ApplicationLifecycleListener.class)
                .toProvider(PushApplicationLifecycleProvider.class)
                .eagerly();

        Binding<TaskQueueScheduler> taskqueueBinding = bind(TaskQueueScheduler.class)
                .toSelf()
                .eagerly();

        return seq(ebeanBinding, lifecycleBinding, taskqueueBinding);
    }
}
