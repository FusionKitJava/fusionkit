package de.marcandreher.fusionkit.core;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.app.FileStructureManager;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.AuthProviderRegistry;
import de.marcandreher.fusionkit.core.auth.LoginHandler;
import de.marcandreher.fusionkit.core.config.FreemarkerConfiguration;
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
import io.javalin.config.RoutesConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinFreemarker;

public class WebApp {

    private Logger logger;
    private WebAppConfig config;
    private Javalin app;

    public WebApp(WebAppConfig config, Consumer<RoutesConfig> appRoutes) {
        this.config = config;
        long startTime = System.currentTimeMillis();
        this.logger = FusionKit.getLogger(WebApp.class, config.getName());
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

                if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
                    setupDevEnv(javalinConfig);
                }

                if (config.i18n.isEnabled()) {
                    setupLocalization(javalinConfig);
                }

                if (config.auth.isEnabled()) {
                    setupAuth(javalinConfig);
                }

                appRoutes.accept(javalinConfig.routes);

            });

            this.app.start(config.getPort());
        } catch (Exception e) {
            logger.error("Failed to start WebApp: " + e.getMessage());
            System.exit(1);
        }

        String fullUrl = config.getDomain();
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            fullUrl += ":" + config.getPort();
        }

        logger.info(">> WebApp '{}' running on {} in <{}ms>", config.getName(), fullUrl,
                System.currentTimeMillis() - startTime);
    }

    private void setupAuth(JavalinConfig javalinConfig) {
        if (config.auth.getEnabledProviders() != null && !config.auth.getEnabledProviders().isEmpty()) {
            for (AuthProvider provider : config.auth.getEnabledProviders()) {
                if (provider == null || provider == AuthProvider.NONE) {
                    continue;
                }
                LoginHandler loginHandler = AuthProviderRegistry.createHandler(provider, this);
                if (loginHandler == null) {
                    logger.error("Unsupported AuthProvider: {}", provider);
                    System.exit(1);
                    return;
                }
                loginHandler.registerRoutes(javalinConfig);
            }
        } else if (config.auth.getAuthProvider() != AuthProvider.NONE) {
            LoginHandler loginHandler = AuthProviderRegistry.createHandler(config.auth.getAuthProvider(), this);
            if (loginHandler == null) {
                logger.error("Unsupported AuthProvider: {}", config.auth.getAuthProvider());
                System.exit(1);
                return;
            }
            loginHandler.registerRoutes(javalinConfig);
        }
    }

    private void setupLocalization(JavalinConfig javalinConfig) {
        if (FusionKit.getClassLoader() == null) {
            logger.error("ClassLoader not set in FusionKit. Please set it before using i18n in WebApp.");
            System.exit(1);
        }

        javalinConfig.routes.before("/*", new I18nHandler(FusionKit.getClassLoader(), config));
        javalinConfig.routes.get("/i18n/info", new I18nInfoHandler());
        javalinConfig.routes.post("/i18n/set", new I18nSetHandler());
    }

    private void setupDevEnv(JavalinConfig javalinConfig) {
        // Configure global exception handler
        JavalinExceptionHandler exceptionHandler = JavalinExceptionHandler.create(config);
        javalinConfig.routes.exception(Exception.class, exceptionHandler::handleException);
        javalinConfig.routes.get("/fusion", new FusionInfoHandler(config));

        if (FusionKit.database != null) {
            javalinConfig.routes.get("/fusion/database", new FusionDatabaseInfoHandler(config));
        }

        if (config.isDebugger()) {
            javalinConfig.routes.before("/*", new FusionDebugCache());
            javalinConfig.routes.after("/*", new FusionDebugHandler());
            javalinConfig.routes.get("/fusion/debug/", new FusionDebugAPIHandler());
            javalinConfig.routes.get("/fusion/request/", new FusionDebugRequestAPIHandler());
        }
    }

    public WebAppConfig getConfig() {
        return config;
    }

    private void configureJavalin(JavalinConfig javalinConfig) {
        // Configure server settings
        javalinConfig.startup.showJavalinBanner = config.server.isShowBanner();
        javalinConfig.http.maxRequestSize = config.server.getMaxRequestSize();

        javalinConfig.jsonMapper(new FusionJsonMapper());

        // Configure request logging
        if (config.logging.isRequestLogging()) {
            javalinConfig.requestLogger.http(new FusionRequestLogger(config, logger));
        }

        // Configure CORS
        if (config.cors.isEnabled()) {
            javalinConfig.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsRule -> {

                    String origins = config.cors.getOrigins();

                    if (origins != null && origins.trim().equals("*")) {
                        corsRule.anyHost();
                    } else if (origins != null && !origins.isBlank()) {
                        for (String origin : origins.split(",")) {
                            String host = origin.trim();
                            if (!host.isEmpty()) {
                                corsRule.allowHost(host);
                            }
                        }
                    }

                    // Credentials cannot be used with wildcard origins
                    corsRule.allowCredentials =
                        origins != null && !origins.trim().equals("*");
                });
            });
        }
        // Configure static files if enabled
        if (config.staticFiles.isEnabled()) {
            if (config.staticFiles.isExternal()) {
                FileStructureManager staticDir = new FileStructureManager(FileStructureManager.DirectoryType.PUBLIC);
                staticDir.persist();

                javalinConfig.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = config.staticFiles.getPath();
                    staticFiles.directory = config.staticFiles.getDirectory();
                    staticFiles.location = Location.EXTERNAL;
                    staticFiles.headers = Map.of(
                            "Cache-Control", "public, max-age=31536000" // 1 year cache
                    );

                });

            } else {
                javalinConfig.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = config.staticFiles.getPath();
                    staticFiles.directory = config.staticFiles.getDirectory();
                    staticFiles.location = Location.CLASSPATH;
                    staticFiles.headers = Map.of(
                            "Cache-Control", "public, max-age=31536000" // 1 year cache
                    );
                });
            }
        }
        // Configure Freemarker if enabled
        if (config.freemarker.isEnabled()) {
            try {
                FileStructureManager templateDir = new FileStructureManager(
                        FileStructureManager.DirectoryType.TEMPLATES);
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

        if (config.getJavalinConfigurator() != null) {
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
