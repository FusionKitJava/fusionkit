package de.marcandreher.fusionkit.core.auth;

import java.lang.annotation.Retention;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface AuthHandler {
    AuthProvider value();
}
