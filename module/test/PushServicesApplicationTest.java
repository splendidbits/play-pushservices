import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.Mode;
import play.api.inject.Binding;
import play.api.inject.BindingKey;
import play.api.routing.Router;
import play.inject.guice.GuiceApplicationBuilder;
import play.routing.RoutingDsl;
import play.test.Helpers;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 28/12/2016 Splendid Bits.
 */
public abstract class PushServicesApplicationTest {
    static Application application;

    @BeforeClass
    public static void startApplicationTest() {
        // Mock the router binding.
        Binding<Router> routesBindingOverride = new BindingKey<>(Router.class)
                .toProvider(MockRouterProvider.class)
                .eagerly();

        application = new GuiceApplicationBuilder()
                .in(Mode.TEST)
                .configure("db.pushservices.name", "pushservices")
                .configure("db.pushservices.driver", "org.postgresql.Driver")
                .configure("db.pushservices.connectionTimeout", "10 seconds")
                .configure("db.pushservices.url", "jdbc:postgresql://localhost:5432/pushservices")
                .configure("db.pushservices.username", "YOUR_USERNAME")
                .configure("db.pushservices.password", "YOUR_PASSWORD")
                .overrides(routesBindingOverride)
                .build();
    }

    @AfterClass
    public static void stopApplicationTest() {
        Helpers.stop(application);
    }

    /**
     * Override the default routes.
     */
    static class MockRouterProvider implements Provider<Router> {
        private final RoutingDsl routingDsl;

        @Inject
        public MockRouterProvider(RoutingDsl routingDsl) {
            this.routingDsl = routingDsl;
        }

        @Override
        public play.api.routing.Router get() {
            return routingDsl.build().asScala();
        }
    }
}
