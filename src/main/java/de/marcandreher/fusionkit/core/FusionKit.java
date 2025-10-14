package de.marcandreher.fusionkit.core;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.marcandreher.fusionkit.core.app.FileStructureManager;
import de.marcandreher.fusionkit.core.app.FileStructureManager.DirectoryType;
import de.marcandreher.fusionkit.core.app.VersionInfo;
import de.marcandreher.fusionkit.core.config.AppConfiguration;
import de.marcandreher.fusionkit.core.cron.Cron;
import de.marcandreher.fusionkit.core.database.Database;

public class FusionKit {

    private static final Logger logger = FusionKit.getLogger(FusionKit.class);
    private static final ArrayList<WebApp> webApps = new ArrayList<>();
    private static Cron cron = new Cron();
    private static final Gson gson;
    private static AppConfiguration configuration;
    private static ClassLoader classLoader;
    public static Database database;
    
    private static FileStructureManager dataDirectory = new FileStructureManager(DirectoryType.DATA);

    static {

        gson = new GsonBuilder().setPrettyPrinting().create();
        dataDirectory.persist();
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

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    public static void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    public static Logger getLogger(Class<?> cls) {
        return getLogger(cls, null);
    }

    public static Logger getLogger(Class<?> cls, String name) {
        return LoggerFactory.getLogger(cls.getSimpleName() + (name != null ? " [" + name + "]" : ""));
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
