package de.marcandreher.fusionkit.core.auth.handlers;

import com.google.gson.JsonObject;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.AuthHandler;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.DiscordConfig;

@AuthHandler(AuthProvider.DISCORD)
public class DiscordLoginHandler extends OAuth2LoginHandler {

    private static final String DISCORD_API = "https://discord.com/api";

    public DiscordLoginHandler(WebApp app, DiscordConfig config) {
        super(app, config, "discord");
    }

    @Override
    protected String getAuthorizeUrl() {
        return "https://discord.com/oauth2/authorize";
    }

    @Override
    protected String getTokenUrl() {
        return DISCORD_API + "/oauth2/token";
    }

    @Override
    protected String getUserUrl() {
        return DISCORD_API + "/users/@me";
    }

    @Override
    protected String getScope() {
        return "identify email";
    }

    @Override
    protected String getProviderName() {
        return "Discord";
    }

    @Override
    protected User mapUser(JsonObject userJson) {
        return FusionKit.getGson().fromJson(userJson, User.class);
    }
}
