package de.marcandreher.fusionkit.core.auth;

import io.javalin.http.Context;

public interface AuthProcessor {
    void handle(User user, Context ctx);
}
