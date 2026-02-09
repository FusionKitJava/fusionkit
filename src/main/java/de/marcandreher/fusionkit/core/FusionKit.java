package de.marcandreher.fusionkit.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import de.marcandreher.fusionkit.core.app.FileStructureManager;
import de.marcandreher.fusionkit.core.app.Shutdown;
import de.marcandreher.fusionkit.core.app.FileStructureManager.DirectoryType;
import de.marcandreher.fusionkit.core.app.VersionInfo;
import de.marcandreher.fusionkit.core.cmd.Command;
import de.marcandreher.fusionkit.core.cmd.CommandService;
import de.marcandreher.fusionkit.core.logger.JLineConsoleAppender;
import de.marcandreher.fusionkit.core.cmd.implementations.AppCommand;
import de.marcandreher.fusionkit.core.config.AppConfiguration;
import de.marcandreher.fusionkit.core.cron.FusionCron;
import de.marcandreher.fusionkit.core.database.Database;
import okhttp3.OkHttpClient;

public class FusionKit {

    protected static final FileStructureManager dataDirectory = new FileStructureManager(DirectoryType.DATA);
    protected static final Logger logger = FusionKit.getLogger(FusionKit.class);
    protected static final Map<String, AppConfiguration> configurations = new HashMap<>();
    protected static final ArrayList<WebApp> webApps = new ArrayList<>();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected static FusionCron cron = new FusionCron();
    protected static OkHttpClient client = new OkHttpClient.Builder().build();
    protected static CommandService commandService;
    protected static ClassLoader classLoader;
    public static Database database;
    
    static {
        dataDirectory.persist();
        VersionInfo.loadVersionProperties();
        logger.info("Using FusionKit v{} <{}>", VersionInfo.getVersion(), VersionInfo.getBuildTimestamp());

        Shutdown shutdown = new Shutdown();
        Runtime.getRuntime().addShutdownHook(shutdown.getShutdownHook());
    }

    public static void enableCommandService() {
        configureJLineLogger();
        commandService = new CommandService();
        commandService.start();
        commandService.registerCommand(new AppCommand(webApps));    
    }
    
    /**
     * Programmatically configures logback to use JLine console appender.
     * This replaces the default CONSOLE appender with JLINE appender for interactive command support.
     */
    private static void configureJLineLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        
        // Remove the CONSOLE appender
        Appender<ILoggingEvent> consoleAppender = rootLogger.getAppender("CONSOLE");
        if (consoleAppender != null) {
            rootLogger.detachAppender("CONSOLE");
            consoleAppender.stop();
        }
        
        // Create and configure JLine appender
        JLineConsoleAppender jlineAppender = new JLineConsoleAppender();
        jlineAppender.setContext(loggerContext);
        jlineAppender.setName("JLINE");
        
        // Create and configure encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%highlight(%-5level) %yellow(%d{yyyy-MM-dd HH:mm:ss}) %magenta(%logger{36}) - %msg %red(%ex{short})");
        encoder.setCharset(java.nio.charset.StandardCharsets.UTF_8);
        encoder.start();
        
        jlineAppender.setEncoder(encoder);
        jlineAppender.start();
        
        // Attach JLine appender to root logger
        rootLogger.addAppender(jlineAppender);
        
        logger.debug("Switched to JLine console appender");
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

    public static void registerCommand(Class<? extends Command> commandClass) {
        Command commandInstance;
        try {
            commandInstance = commandClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Failed to instantiate command class: {}", e.getMessage(), e);
            return;
        }

        commandService.registerCommand(commandInstance);
    }

    public static Gson getGson() {
        return gson;
    }

    public static OkHttpClient getHttpClient() {
        return client;
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
