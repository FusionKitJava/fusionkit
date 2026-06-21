package de.marcandreher.fusionkit.core.auth;

import java.util.concurrent.atomic.AtomicBoolean;

import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;
import io.javalin.config.JavalinConfig;

public final class AuthRouteRegistrar {

    private static final AtomicBoolean LOGOUT_REGISTERED = new AtomicBoolean(false);

    private AuthRouteRegistrar() {
    }

    public static void registerLogout(JavalinConfig javalinConfig, AuthSessionStore sessionStore) {
        if (!LOGOUT_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        javalinConfig.routes.get("/logout", ctx -> {
            sessionStore.clear(ctx);
            ctx.redirect("/");
        });
    }
}
