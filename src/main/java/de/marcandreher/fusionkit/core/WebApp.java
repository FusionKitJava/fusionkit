package de.marcandreher.fusionkit.core;

import java.util.Map;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.app.FileStructureManager;
import de.marcandreher.fusionkit.core.config.FreemarkerConfiguration;
import de.marcandreher.fusionkit.core.config.WebAppConfig;
import de.marcandreher.fusionkit.core.debug.FusionDebugAPIHandler;
import de.marcandreher.fusionkit.core.debug.FusionDebugCache;
import de.marcandreher.fusionkit.core.debug.FusionDebugHandler;
import de.marcandreher.fusionkit.core.debug.FusionDebugRequestAPIHandler;
import de.marcandreher.fusionkit.core.error.JavalinExceptionHandler;
import de.marcandreher.fusionkit.core.i18n.I18nHandler;
import de.marcandreher.fusionkit.core.i18n.I18nInfoHandler;
import de.marcandreher.fusionkit.core.i18n.I18nSetHandler;
import de.marcandreher.fusionkit.core.javalin.FusionJsonMapper;
import de.marcandreher.fusionkit.core.javalin.FusionRequestLogger;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import de.marcandreher.fusionkit.core.routes.FusionDatabaseInfoHandler;
import de.marcandreher.fusionkit.core.routes.FusionInfoHandler;
import freemarker.template.Configuration;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinFreemarker;

public class WebApp {

    private Logger logger;
    private WebAppConfig config;
    private Javalin app;

    public WebApp(WebAppConfig config) {

        long startTime = System.currentTimeMillis();
        this.logger = FusionKit.getLogger(WebApp.class, config.getName());
        this.config = config;
        try {
            app = Javalin.create(javalinConfig -> {
                // Configure custom thread pool name for Jetty
                javalinConfig.jetty.modifyServer(server -> {
                    
                    if (server.getThreadPool() instanceof QueuedThreadPool queuedThreadPool) {
                        queuedThreadPool.setName("FK-WebApp-" + config.getName());
                    }
                });
                // Apply other configurations
                configureJavalin(javalinConfig);
            });

            if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
                // Configure global exception handler
                JavalinExceptionHandler exceptionHandler = JavalinExceptionHandler.create(config);
                app.exception(Exception.class, exceptionHandler::handleException);
                app.get("/fusion", new FusionInfoHandler(config));
                
                if(FusionKit.database != null) {
                    app.get("/fusion/database", new FusionDatabaseInfoHandler(config));
                }

                if(config.isDebugger()) {
                    app.before("/*", new FusionDebugCache());
                    app.after("/*", new FusionDebugHandler());
                    app.get("/fusion/debug/", new FusionDebugAPIHandler());
                    app.get("/fusion/request/", new FusionDebugRequestAPIHandler());
                }
            }

            if(config.isI18n()) {
                if(FusionKit.getClassLoader() == null) {
                    logger.error("ClassLoader not set in FusionKit. Please set it before using i18n in WebApp.");
                    System.exit(1);
                }

                this.app.before("/*", new I18nHandler(FusionKit.getClassLoader(), config));
                this.app.get("/i18n/info", new I18nInfoHandler());
                this.app.post("/i18n/set", new I18nSetHandler());
            }

            this.app.start(config.getPort());
        } catch (Exception e) {
            logger.error("Failed to start WebApp: " + e.getMessage());
            System.exit(1);
        }

        String fullUrl = config.getDomain();
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            fullUrl += ":" + config.getPort();
        }

        logger.info(">> WebApp '{}' running on {} in {}ms", config.getName(), fullUrl, System.currentTimeMillis() - startTime);
    }

    public WebAppConfig getConfig() {
        return config;
    }

    private void configureJavalin(JavalinConfig javalinConfig) {
        // Configure server settings
        javalinConfig.showJavalinBanner = config.isShowBanner();
        javalinConfig.http.maxRequestSize = config.getMaxRequestSize();

        javalinConfig.jsonMapper(new FusionJsonMapper());

        // Configure request logging
        if (config.isRequestLogging()) {
            javalinConfig.requestLogger.http(new FusionRequestLogger(config, logger));
        }

        // Configure CORS
        if (config.isCorsEnabled()) {
            javalinConfig.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsRule -> {
                    for (String origin : config.getCorsOrigins().split(",")) {
                        corsRule.allowHost(origin.trim());
                    }
                    corsRule.allowCredentials = true;
                });
            });
        }

        // Configure static files if enabled
        if (config.isStaticfiles()) {
            if (config.isStaticFilesExternal()) {
                FileStructureManager staticDir = new FileStructureManager(FileStructureManager.DirectoryType.PUBLIC);
                staticDir.persist();

                javalinConfig.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = config.getStaticFilesPath();
                    staticFiles.directory = config.getStaticFilesDirectory();
                    staticFiles.location = Location.EXTERNAL;
                    staticFiles.headers = Map.of(
                        "Cache-Control", "public, max-age=31536000" // 1 year cache
                    );

                });

            } else {
                javalinConfig.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = config.getStaticFilesPath();
                    staticFiles.directory = config.getStaticFilesDirectory();
                    staticFiles.location = Location.CLASSPATH;
                    staticFiles.headers = Map.of(
                        "Cache-Control", "public, max-age=31536000" // 1 year cache
                    );
                });
            }
        }
        // Configure Freemarker if enabled
        if (config.isFreemarker()) {
            try {
                FileStructureManager templateDir = new FileStructureManager(FileStructureManager.DirectoryType.TEMPLATES);
                templateDir.persist();

                FreemarkerConfiguration fmConfigFile = new FreemarkerConfiguration();
                Configuration fmConfig = fmConfigFile.applyConfig(config, templateDir.getDirectory());

                javalinConfig.fileRenderer(new JavalinFreemarker(fmConfig));
            } catch (Exception e) {
                logger.error("Error configuring Freemarker: " + e.getMessage(), e);
            }
        }
        // Configure route overview if enabled
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            javalinConfig.bundledPlugins.enableRouteOverview("/routes");
        }

        if(config.getJavalinConfigurator() != null) {
            config.getJavalinConfigurator().configure(javalinConfig);
        }

        logger.debug("WebApp Configuration: <{}>", config);
    }

    public String getSafeName() {
        return config.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    public Javalin getApp() {
        return app;
    }

    public void stop() {
        if (app != null) {
            logger.info("Stopping WebApp '{}'", config.getName());
            app.stop();
        }
    }
}
