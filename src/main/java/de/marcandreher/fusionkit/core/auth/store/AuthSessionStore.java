package de.marcandreher.fusionkit.core.auth.store;

import de.marcandreher.fusionkit.core.auth.User;
import io.javalin.http.Context;

public interface AuthSessionStore {

    User getUser(Context ctx);

    void setUser(Context ctx, User user);

    void clear(Context ctx);
}
