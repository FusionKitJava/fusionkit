package de.marcandreher.fusionkit.core.javalin.engine;

import org.jetbrains.annotations.NotNull;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.database.Database;
import io.javalin.config.Key;
import io.javalin.http.ExceptionHandler;
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

    public Database getDatabase(FusionContext ctx) {
        var myKey = new Key<Database>("database");
        return (Database) ctx.appData(myKey);
    }

    public ExceptionHandler<Exception> toExceptionHandler() {
        return (e, ctx) -> {
            try {
                this.handle(FusionContextWrapper.create(ctx));
            } catch (Exception e1) {
                FusionKit.getLogger(FusionHandler.class).error("Error occurred while handling request", e1);
            }
        };
    }
}
