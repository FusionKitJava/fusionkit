package de.marcandreher.fusionkit.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.marcandreher.fusionkit.core.app.FileStructureManager;
import de.marcandreher.fusionkit.core.app.FileStructureManager.DirectoryType;
import de.marcandreher.fusionkit.core.app.VersionInfo;
import de.marcandreher.fusionkit.core.config.AppConfiguration;
import de.marcandreher.fusionkit.core.cron.FusionCron;
import de.marcandreher.fusionkit.core.database.Database;

public class FusionKit {

    private static final FileStructureManager dataDirectory = new FileStructureManager(DirectoryType.DATA);
    private static final Logger logger = FusionKit.getLogger(FusionKit.class);
    private static final Map<String, AppConfiguration> configurations = new HashMap<>();
    private static final ArrayList<WebApp> webApps = new ArrayList<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static FusionCron cron = new FusionCron();
    private static ClassLoader classLoader;
    public static Database database;
    
    

    static {
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
        cron.shutdown();
        logger.info("Shutdown complete.");
    }

    public static void setConfig(String file, Class<?> config) {
        Object configInstance;
        try {
            configInstance = config.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Failed to instantiate config class: {}", e.getMessage(), e);
            return;
        }

        configurations.put(file, new AppConfiguration(file, configInstance, logger));
    }

    public static AppConfiguration getConfig(String file) {
        return configurations.get(file);
    }

    public static Gson getGson() {
        return gson;
    }

    public static FusionCron getCron() {
        return cron;
    }

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    public static void setApplication(Class<?> app) {
        classLoader = app.getClassLoader();
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
    }

    public static void registerWebApplication(WebApp app) {
        webApps.add(app);
        logger.debug("Registered web application: {}", app.getConfig().getName());
    }
    
}
