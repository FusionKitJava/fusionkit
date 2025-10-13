package de.marcandreher.fusionkit.lib.javalin;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import de.marcandreher.fusionkit.lib.helpers.WebAppConfig;
import io.javalin.http.Context;
import io.javalin.http.RequestLogger;

public class FusionRequestLogger implements RequestLogger {

    private final WebAppConfig config;
    private final Logger logger;

    public FusionRequestLogger(WebAppConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void handle(@NotNull Context ctx, @NotNull Float executionTimeMs) throws Exception {
        String logMessage = config.getLogFormat()
                .replace("{method}", ctx.method().toString().toUpperCase())
                .replace("{host}", ctx.host())
                .replace("{path}", ctx.path())
                .replace("{status}", String.valueOf(ctx.status()))
                .replace("{ms}", String.valueOf(executionTimeMs))
                .replace("{agent}", ctx.userAgent() != null ? ctx.userAgent() : "-");
        logger.info(logMessage);
    }

}
