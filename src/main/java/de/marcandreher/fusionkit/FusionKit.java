package de.marcandreher.fusionkit;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.cron.Cron;
import de.marcandreher.fusionkit.lib.database.Database;
import de.marcandreher.fusionkit.lib.helpers.AppConfiguration;
import de.marcandreher.fusionkit.lib.helpers.DataDirectory;
import de.marcandreher.fusionkit.util.VersionInfo;

public class FusionKit {

    private static final Logger logger = LoggerFactory.getLogger(FusionKit.class);
    private static final ArrayList<WebApp> webApps = new ArrayList<>();
    private static Cron cron = new Cron();
    private static final Gson gson;
    private static AppConfiguration configuration;
    public static Database database;
    public static DataDirectory data;

    static {
        data = new DataDirectory();
        gson = new GsonBuilder().setPrettyPrinting().create();
        VersionInfo.loadVersionProperties();
        logger.info("Using FusionKit v{} <{}>", VersionInfo.getVersion(), VersionInfo.getBuildTimestamp());

        Runtime.getRuntime().addShutdownHook(new Thread(FusionKit::shutdown));
    }

    private static void shutdown() {
        logger.info("Shutting down FusionKit...");
        for (WebApp app : webApps) {
            app.stop();
        }
        logger.info("Shutdown complete.");
    }

    public static void setConfig(Object config) {
        configuration = new AppConfiguration(config, logger);
    }

    public static AppConfiguration getConfig() {
        return configuration;
    }

    public static Gson getGson() {
        return gson;
    }

    public static Cron getCron() {
        return cron;
    }

    public static void setLogLevel(String level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.toLevel(level));
        logger.debug("Log level set to {}", level);
    }

    public static void registerWebApplication(WebApp app) {
        webApps.add(app);
        logger.debug("Registered web application: {}", app.getConfig().getName());
    }
    
}
