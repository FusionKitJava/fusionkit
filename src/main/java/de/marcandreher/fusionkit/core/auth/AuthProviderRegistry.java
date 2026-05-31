package de.marcandreher.fusionkit.core.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.config.DiscordConfig;
import de.marcandreher.fusionkit.core.auth.config.GitHubConfig;
import de.marcandreher.fusionkit.core.auth.config.SteamConfig;
import de.marcandreher.fusionkit.core.auth.handlers.DiscordLoginHandler;
import de.marcandreher.fusionkit.core.auth.handlers.GitHubLoginHandler;
import de.marcandreher.fusionkit.core.auth.handlers.SteamLoginHandler;

public final class AuthProviderRegistry {

    private static final Map<AuthProvider, AuthHandlerFactory> FACTORIES = new ConcurrentHashMap<>();

    static {
        register(AuthProvider.DISCORD, app -> new DiscordLoginHandler(app, DiscordConfig.loadConfig()));
        register(AuthProvider.GITHUB, app -> new GitHubLoginHandler(app, GitHubConfig.loadConfig()));
        register(AuthProvider.STEAM, app -> new SteamLoginHandler(app, SteamConfig.loadConfig()));
    }

    private AuthProviderRegistry() {
    }

    public static void register(AuthProvider provider, AuthHandlerFactory factory) {
        if (provider == null || factory == null) {
            return;
        }
        FACTORIES.put(provider, factory);
    }

    public static LoginHandler createHandler(AuthProvider provider, WebApp app) {
        AuthHandlerFactory factory = FACTORIES.get(provider);
        if (factory == null) {
            return null;
        }
        return factory.create(app);
    }
}
