package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.AuthHandler;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.LoginHandler;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.DiscordConfig;
import de.marcandreher.fusionkit.core.config.WebAppConfig;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@AuthHandler(AuthProvider.DISCORD)
public class DiscordLoginHandler implements LoginHandler {

    private static final String DISCORD_API = "https://discord.com/api";

    private DiscordConfig discordConfig;
    private WebAppConfig config;
    private WebApp app;
    private String redirectUri;

    public DiscordLoginHandler(WebApp app, DiscordConfig discordConfig) {
        this.config = app.getConfig();
        this.discordConfig = discordConfig;

        if(ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            this.redirectUri = config.getDomain() + ":" + config.getPort() + "/auth/discord/callback";
        }else {
            this.redirectUri = config.getDomain() + "/auth/discord/callback";
        }
        
        this.app = app;
    }

    @Override
    public void registerRoutes() {
        FusionKit.getLogger(DiscordLoginHandler.class).info("Registering Discord OAuth2 login handler");
        app.getApp().before("/*", ctx -> {
            // Make user available in all templates
            ctx.attribute("user", ctx.sessionAttribute("user"));
            ctx.attribute("url", getLoginUrl(redirectUri, "state123"));
        });
        app.getApp().get("/auth/discord/callback", ctx -> {
            String code = ctx.queryParam("code");
            if (code == null) {
                ctx.result("Missing code");
                return;
            }

            String accessToken = exchangeCodeForToken(code);
            if (accessToken == null) {
                ctx.result("Failed to get access token");
                return;
            }

            User user = fetchDiscordUser(accessToken);
            if (user == null) {
                ctx.result("Failed to fetch user");
                return;
            }

            config.getAuthHandler().handle(user, ctx);

            // Save user in session
            ctx.sessionAttribute("user", user);

            ctx.redirect("/");
        });
        app.getApp().get("/logout", ctx -> {
            ctx.sessionAttribute("user", null);
            ctx.redirect("/");
        });
    }

    @Override
    public String getLoginUrl(String redirectUri, String state) {
        String url = "https://discord.com/oauth2/authorize"
                + "?client_id=" + discordConfig.getClientId()
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify email";
        return url;
    }

    private String exchangeCodeForToken(String code) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("client_id", discordConfig.getClientId())
                .add("client_secret", discordConfig.getClientSecret())
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build();

        Request request = new Request.Builder()
                .url(DISCORD_API + "/oauth2/token")
                .post(form)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                return null;
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return json.get("access_token").getAsString();
        }
    }

    private static User fetchDiscordUser(String token) throws IOException {
        Request request = new Request.Builder()
                .url(DISCORD_API + "/users/@me")
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                return null;
            return FusionKit.getGson().fromJson(response.body().string(), User.class);
        }
    }

}
