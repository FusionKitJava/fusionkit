package de.marcandreher.fusionkit.core.auth;

import io.javalin.http.Context;

public interface AfterLoginHandler {
    void handle(User user, Context ctx);
}
