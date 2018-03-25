package injection.pushservices.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import main.pushservices.Constants;
import models.pushservices.db.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
@Singleton
public class PushServicesEbeanServerProvider implements Provider<EbeanServer> {
    private final Config configuration;

    @Inject
    public PushServicesEbeanServerProvider(Config configuration) {
        this.configuration = configuration;
    }

    @Override
    public EbeanServer get() {
        if (configuration == null || configuration.isEmpty()) {
            throw new RuntimeException("No Play Framework configuration found.");
        }

        Config pushServicesConfig = configuration.withOnlyPath(Constants.CONFIG_PREFIX);
        if (pushServicesConfig == null) {
            throw new RuntimeException("No push services configuration found. Refer to documentation");
        }

        // Build custom properties from main configuration.
        Properties properties = new Properties();
        for (Map.Entry<String, ConfigValue> configEntry : pushServicesConfig.entrySet()) {
            String[] keyParts = configEntry.getKey().split("\\.");
            if (keyParts.length < 2) {
                continue;
            }

            String key = keyParts[keyParts.length-1];
            String value = configEntry.getValue().render();
            if (configEntry.getValue().valueType().equals(ConfigValueType.STRING)) {
                value = (String) configEntry.getValue().unwrapped();
            }
            properties.put(key, value);
        }

        ArrayList<Class<?>> models = new ArrayList<>();
        models.add(Credentials.class);
        models.add(Message.class);
        models.add(PayloadElement.class);
        models.add(Recipient.class);
        models.add(PlatformFailure.class);
        models.add(Task.class);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.loadFromProperties(properties);

        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(true);
        serverConfig.setUpdatesDeleteMissingChildren(false);
        serverConfig.setClasses(models);
        serverConfig.setDdlGenerate(false);
        serverConfig.setUpdateChangesOnly(false);

        serverConfig.setName(Constants.CONFIG_PREFIX);
        return EbeanServerFactory.create(serverConfig);
    }
}