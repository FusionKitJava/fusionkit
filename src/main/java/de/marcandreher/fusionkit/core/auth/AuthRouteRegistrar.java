package de.marcandreher.fusionkit.core.auth;

import java.util.concurrent.atomic.AtomicBoolean;

import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;

public final class AuthRouteRegistrar {

    private static final AtomicBoolean LOGOUT_REGISTERED = new AtomicBoolean(false);

    private AuthRouteRegistrar() {
    }

    public static void registerLogout(WebApp app, AuthSessionStore sessionStore) {
        if (!LOGOUT_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        app.getApp().get("/logout", ctx -> {
            sessionStore.clear(ctx);
            ctx.redirect("/");
        });
    }
}
