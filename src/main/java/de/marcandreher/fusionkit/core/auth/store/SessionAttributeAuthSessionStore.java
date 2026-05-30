package de.marcandreher.fusionkit.core.auth.store;

import de.marcandreher.fusionkit.core.auth.User;
import io.javalin.http.Context;

public class SessionAttributeAuthSessionStore implements AuthSessionStore {

    private static final String USER_SESSION_KEY = "user";

    @Override
    public User getUser(Context ctx) {
        return ctx.sessionAttribute(USER_SESSION_KEY);
    }

    @Override
    public void setUser(Context ctx, User user) {
        ctx.sessionAttribute(USER_SESSION_KEY, user);
    }

    @Override
    public void clear(Context ctx) {
        ctx.sessionAttribute(USER_SESSION_KEY, null);
    }
}
