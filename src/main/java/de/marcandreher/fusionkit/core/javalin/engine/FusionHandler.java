package de.marcandreher.fusionkit.core.javalin.engine;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Handler;

public class FusionHandler {

    public void handle(@NotNull FusionContext ctx) throws Exception {
        // Default implementation (can be overridden)
    }

    /**
     * Creates a Handler for this specific FusionHandler instance.
     * Use this for handlers that have state or need to be instantiated with parameters.
     */
    public Handler toHandler() {
        return ctx -> this.handle(FusionContextWrapper.create(ctx));
    }
}
