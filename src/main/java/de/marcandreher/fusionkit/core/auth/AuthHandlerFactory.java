package de.marcandreher.fusionkit.core.auth;

import de.marcandreher.fusionkit.core.WebApp;

@FunctionalInterface
public interface AuthHandlerFactory {

    LoginHandler create(WebApp app);
}
