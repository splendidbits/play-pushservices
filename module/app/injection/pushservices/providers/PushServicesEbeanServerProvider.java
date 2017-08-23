package injection.pushservices.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import main.pushservices.Constants;
import models.pushservices.db.*;
import org.avaje.datasource.DataSourceConfig;

import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
@Singleton
public class PushServicesEbeanServerProvider implements Provider<EbeanServer> {
    private final static String SERVER_CONFIG_PREFIX = Constants.CONFIG_PREFIX + ".";
    private Config mConfiguration;

    @Inject
    public PushServicesEbeanServerProvider(Config configuration) {
        mConfiguration = configuration;
    }

    @Override
    public EbeanServer get() {
        String datasourceUrl = mConfiguration.getString(SERVER_CONFIG_PREFIX + "url");
        String datasourceUsername = mConfiguration.getString(SERVER_CONFIG_PREFIX + "username");
        String datasourcePassword = mConfiguration.getString(SERVER_CONFIG_PREFIX + "password");
        String datasourceDriver = mConfiguration.getString(SERVER_CONFIG_PREFIX + "driver");
        String databaseName = mConfiguration.getString(SERVER_CONFIG_PREFIX + "name");
        String platformName = mConfiguration.getString(SERVER_CONFIG_PREFIX + "platformName");

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUrl(datasourceUrl);
        dataSourceConfig.setDriver(datasourceDriver);
        dataSourceConfig.setUsername(datasourceUsername);
        dataSourceConfig.setPassword(datasourcePassword);

        dataSourceConfig.setHeartbeatFreqSecs(60);
        dataSourceConfig.setHeartbeatTimeoutSeconds(30);
        dataSourceConfig.setMinConnections(3);
        dataSourceConfig.setMaxConnections(30);
        dataSourceConfig.setLeakTimeMinutes(1);
        dataSourceConfig.setMaxInactiveTimeSecs(30);
        dataSourceConfig.setWaitTimeoutMillis(1000 * 60);
        dataSourceConfig.setTrimPoolFreqSecs(60);
        dataSourceConfig.setCaptureStackTrace(true);

        ArrayList<Class<?>> models = new ArrayList<>();
        models.add(Credentials.class);
        models.add(Message.class);
        models.add(PayloadElement.class);
        models.add(Recipient.class);
        models.add(PlatformFailure.class);
        models.add(Task.class);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setName(databaseName);
        serverConfig.setDatabasePlatformName(platformName);
        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(true);
        serverConfig.setUpdatesDeleteMissingChildren(false);
        serverConfig.setClasses(models);
        serverConfig.setDdlGenerate(false);
        serverConfig.setUpdateChangesOnly(false);

        return EbeanServerFactory.create(serverConfig);
    }
}