package injection;

import injection.pushservices.modules.PushServicesModule;
import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 01/01/2017 Splendid Bits.
 */
public class ApplicationClassLoader extends GuiceApplicationLoader {
    @Override
    public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
        return initialBuilder
                .in(context.environment())
                .bindings(new PushServicesModule());
    }
}
