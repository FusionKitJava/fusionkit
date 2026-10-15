package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern CLAIMED_ID_PATTERN = Pattern.compile("^https://steamcommunity\\.com/openid/id/(\\d{17})$");
    private static final Set<String> REQUIRED_SIGNED_FIELDS = Set.of(
            "op_endpoint", "claimed_id", "identity", "return_to", "response_nonce", "assoc_handle");
    private static final int NONCE_TIMESTAMP_LENGTH = 20;
    private static final int NONCE_MAX_LENGTH = 255;
    private static final Duration NONCE_MAX_AGE = Duration.ofMinutes(5);
    private static final Duration NONCE_MAX_SKEW = Duration.ofMinutes(1);

    private final SteamConfig steamConfig;
    private final WebAppConfig config;
    private final AuthSessionStore sessionStore;
    private final String redirectUri;
    private final String realm;
    private final Map<String, Long> usedNonces = new ConcurrentHashMap<>();

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
            authUrls.put("steam", getLoginUrl(redirectUri, null));
            if (ctx.attribute("url") == null) {
                ctx.attribute("url", authUrls.get("steam"));
            }
        });
        javalinConfig.routes.get("/auth/steam/callback", ctx -> {
            String steamId;
            try {
                steamId = authenticate(ctx);
            } catch (IOException e) {
                FusionKit.getLogger(SteamLoginHandler.class).error("Steam OpenID verification failed", e);
                ctx.status(502).result("Steam OpenID verification failed");
                return;
            }
            if (steamId == null) {
                ctx.status(403).result("Steam OpenID verification failed");
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
        String encodedSelect = URLEncoder.encode(IDENTIFIER_SELECT, StandardCharsets.UTF_8);

        return STEAM_OPENID
                + "?openid.ns=" + URLEncoder.encode(OPENID_NS, StandardCharsets.UTF_8)
                + "&openid.mode=checkid_setup"
                + "&openid.return_to=" + encodedRedirectUri
                + "&openid.realm=" + encodedRealm
                + "&openid.identity=" + encodedSelect
                + "&openid.claimed_id=" + encodedSelect;
    }

    private String authenticate(Context ctx) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ctx.queryParamMap().entrySet()) {
            if (!entry.getKey().startsWith("openid.")) {
                continue;
            }
            if (entry.getValue().size() != 1) {
                return null;
            }
            params.put(entry.getKey(), entry.getValue().get(0));
        }

        if (!OPENID_NS.equals(params.get("openid.ns"))) {
            return null;
        }
        if (!"id_res".equals(params.get("openid.mode"))) {
            return null;
        }
        if (!redirectUri.equals(params.get("openid.return_to"))) {
            return null;
        }
        if (!STEAM_OPENID.equals(params.get("openid.op_endpoint"))) {
            return null;
        }

        String claimedId = params.get("openid.claimed_id");
        if (claimedId == null) {
            return null;
        }
        Matcher matcher = CLAIMED_ID_PATTERN.matcher(claimedId);
        if (!matcher.matches()) {
            return null;
        }
        if (!claimedId.equals(params.get("openid.identity"))) {
            return null;
        }
        if (!signsRequiredFields(params.get("openid.signed"))) {
            return null;
        }
        if (params.get("openid.sig") == null) {
            return null;
        }

        String nonce = params.get("openid.response_nonce");
        if (!claimNonce(nonce)) {
            return null;
        }

        boolean valid;
        try {
            valid = verifyWithSteam(params);
        } catch (IOException e) {
            usedNonces.remove(nonce);
            throw e;
        }
        if (!valid) {
            usedNonces.remove(nonce);
            return null;
        }

        return matcher.group(1);
    }

    private static boolean signsRequiredFields(String signed) {
        if (signed == null || signed.isBlank()) {
            return false;
        }
        Set<String> fields = new HashSet<>(Arrays.asList(signed.split(",")));
        return fields.containsAll(REQUIRED_SIGNED_FIELDS);
    }

    private boolean claimNonce(String nonce) {
        if (nonce == null || nonce.length() <= NONCE_TIMESTAMP_LENGTH || nonce.length() > NONCE_MAX_LENGTH) {
            return false;
        }

        Instant issuedAt;
        try {
            issuedAt = Instant.parse(nonce.substring(0, NONCE_TIMESTAMP_LENGTH));
        } catch (DateTimeParseException e) {
            return false;
        }

        Instant now = Instant.now();
        if (issuedAt.isBefore(now.minus(NONCE_MAX_AGE)) || issuedAt.isAfter(now.plus(NONCE_MAX_SKEW))) {
            return false;
        }

        long expiryCutoff = now.minus(NONCE_MAX_AGE).minus(NONCE_MAX_AGE).toEpochMilli();
        usedNonces.values().removeIf(storedAt -> storedAt < expiryCutoff);

        return usedNonces.putIfAbsent(nonce, now.toEpochMilli()) == null;
    }

    private boolean verifyWithSteam(Map<String, String> params) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("openid.mode".equals(entry.getKey())) {
                continue;
            }
            formBuilder.add(entry.getKey(), entry.getValue());
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
            for (String line : body.string().split("\\R")) {
                if ("is_valid:true".equals(line)) {
                    return true;
                }
            }
            return false;
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

    private static String buildRealm(WebAppConfig config) {
        String baseUrl = config.getDomain();
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            baseUrl += ":" + config.getPort();
        }
        return baseUrl;
    }
}