package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.WebAppConfig;
import de.marcandreher.fusionkit.core.auth.AuthHandler;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.AuthRouteRegistrar;
import de.marcandreher.fusionkit.core.auth.LoginHandler;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.SteamConfig;
import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@AuthHandler(AuthProvider.STEAM)
public class SteamLoginHandler implements LoginHandler {

    private static final String STEAM_OPENID = "https://steamcommunity.com/openid/login";
    private static final String STEAM_API = "https://api.steampowered.com";

    private final SteamConfig steamConfig;
    private final WebAppConfig config;
    private final AuthSessionStore sessionStore;
    private final String redirectUri;
    private final String realm;

    public SteamLoginHandler(WebApp app, SteamConfig steamConfig) {
        this.config = app.getConfig();
        this.sessionStore = config.auth.getAuthSessionStore();
        this.steamConfig = steamConfig;
        this.realm = buildRealm(config);
        this.redirectUri = this.realm + "/auth/steam/callback";
    }

    @Override
    public void registerRoutes(JavalinConfig javalinConfig) {
        FusionKit.getLogger(SteamLoginHandler.class).info("Registering Steam OpenID login handler");
        javalinConfig.routes.before("/*", ctx -> {
            ctx.attribute("user", sessionStore.getUser(ctx));
            Map<String, String> authUrls = ctx.attribute("authUrls");
            if (authUrls == null) {
                authUrls = new LinkedHashMap<>();
                ctx.attribute("authUrls", authUrls);
            }
            authUrls.put("steam", getLoginUrl(redirectUri, "state123"));
            if (ctx.attribute("url") == null) {
                ctx.attribute("url", authUrls.get("steam"));
            }
        });
        javalinConfig.routes.get("/auth/steam/callback", ctx -> {
            if (!verifyOpenId(ctx)) {
                ctx.result("Steam OpenID verification failed");
                return;
            }

            String claimedId = ctx.queryParam("openid.claimed_id");
            if (claimedId == null) {
                ctx.result("Missing Steam OpenID claimed_id");
                return;
            }

            String steamId = extractSteamId(claimedId);
            if (steamId == null) {
                ctx.result("Invalid Steam OpenID claimed_id");
                return;
            }

            User user = fetchSteamUser(steamId);
            if (user == null) {
                ctx.result("Failed to fetch Steam user");
                return;
            }

            if (config.auth.getAuthProcessor() != null) {
                config.auth.getAuthProcessor().handle(user, ctx);
            }

            sessionStore.setUser(ctx, user);
            ctx.redirect("/");
        });
        AuthRouteRegistrar.registerLogout(javalinConfig, sessionStore);
    }

    @Override
    public String getLoginUrl(String redirectUri, String state) {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedRealm = URLEncoder.encode(realm, StandardCharsets.UTF_8);

        return STEAM_OPENID
                + "?openid.ns=" + URLEncoder.encode("http://specs.openid.net/auth/2.0", StandardCharsets.UTF_8)
                + "&openid.mode=checkid_setup"
                + "&openid.return_to=" + encodedRedirectUri
                + "&openid.realm=" + encodedRealm
                + "&openid.identity=" + URLEncoder.encode("http://specs.openid.net/auth/2.0/identifier_select", StandardCharsets.UTF_8)
                + "&openid.claimed_id=" + URLEncoder.encode("http://specs.openid.net/auth/2.0/identifier_select", StandardCharsets.UTF_8);
    }

    private boolean verifyOpenId(Context ctx) throws IOException {
        Map<String, List<String>> queryParams = ctx.queryParamMap();
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            if (!entry.getKey().startsWith("openid.")) {
                continue;
            }
            for (String value : entry.getValue()) {
                formBuilder.add(entry.getKey(), value);
            }
        }
        formBuilder.add("openid.mode", "check_authentication");

        RequestBody form = formBuilder.build();
        Request request = new Request.Builder()
                .url(STEAM_OPENID)
                .post(form)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return false;
            }
            String result = body.string();
            return result.contains("is_valid:true");
        }
    }

    private User fetchSteamUser(String steamId) throws IOException {
        User user = new User();
        user.setId(steamId);

        if (steamConfig.getApiKey() == null || steamConfig.getApiKey().isBlank() || "empty".equals(steamConfig.getApiKey())) {
            return user;
        }

        HttpUrl url = HttpUrl.parse(STEAM_API + "/ISteamUser/GetPlayerSummaries/v2/")
                .newBuilder()
                .addQueryParameter("key", steamConfig.getApiKey())
                .addQueryParameter("steamids", steamId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return user;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return user;
            }
            JsonObject root = JsonParser.parseString(body.string()).getAsJsonObject();
            JsonArray players = root.getAsJsonObject("response").getAsJsonArray("players");
            if (players == null || players.isEmpty()) {
                return user;
            }
            JsonObject player = players.get(0).getAsJsonObject();
            if (player.has("personaname")) {
                user.setUsername(player.get("personaname").getAsString());
            }
            if (player.has("avatarfull")) {
                user.setAvatar(player.get("avatarfull").getAsString());
            }
            return user;
        }
    }

    private static String extractSteamId(String claimedId) {
        int lastSlash = claimedId.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash + 1 >= claimedId.length()) {
            return null;
        }
        return claimedId.substring(lastSlash + 1);
    }

    private static String buildRealm(WebAppConfig config) {
        String baseUrl = config.getDomain();
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            baseUrl += ":" + config.getPort();
        }
        return baseUrl;
    }
}
