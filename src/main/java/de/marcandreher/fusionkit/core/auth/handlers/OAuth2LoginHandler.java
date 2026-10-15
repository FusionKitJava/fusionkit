package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.WebAppConfig;
import de.marcandreher.fusionkit.core.auth.AuthRouteRegistrar;
import de.marcandreher.fusionkit.core.auth.LoginHandler;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.OAuth2ProviderConfig;
import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import io.javalin.config.JavalinConfig;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class OAuth2LoginHandler implements LoginHandler {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    protected final WebApp app;
    protected final WebAppConfig config;
    protected final AuthSessionStore sessionStore;
    protected final OAuth2ProviderConfig providerConfig;
    protected final String redirectUri;

    private final String providerId;
    private final String stateSessionKey;
    private final String verifierSessionKey;

    protected OAuth2LoginHandler(WebApp app, OAuth2ProviderConfig providerConfig, String providerId) {
        this.app = app;
        this.config = app.getConfig();
        this.sessionStore = config.auth.getAuthSessionStore();
        this.providerConfig = providerConfig;
        this.providerId = providerId;
        this.redirectUri = buildRedirectUri(config, getCallbackPath());
        this.stateSessionKey = "oauth2_state_" + providerId;
        this.verifierSessionKey = "oauth2_verifier_" + providerId;
    }

    protected abstract String getAuthorizeUrl();

    protected abstract String getTokenUrl();

    protected abstract String getUserUrl();

    protected abstract String getScope();

    protected abstract User mapUser(JsonObject userJson);

    protected String getProviderName() {
        return providerId;
    }

    protected String getProviderId() {
        return providerId;
    }

    protected void addTokenRequestHeaders(Request.Builder builder) {
    }

    protected void addUserRequestHeaders(Request.Builder builder) {
    }

    @Override
    public void registerRoutes(JavalinConfig javalinConfig) {
        FusionKit.getLogger(getClass()).info("Registering {} OAuth2 login handler", getProviderName());
        javalinConfig.routes.before("/*", ctx -> {
            ctx.attribute("user", sessionStore.getUser(ctx));
            Map<String, String> authUrls = ctx.attribute("authUrls");
            if (authUrls == null) {
                authUrls = new LinkedHashMap<>();
                ctx.attribute("authUrls", authUrls);
            }
            authUrls.put(getProviderId(), getLoginPath());
            if (ctx.attribute("url") == null) {
                ctx.attribute("url", authUrls.get(getProviderId()));
            }
        });
        javalinConfig.routes.get(getLoginPath(), ctx -> {
            String state = randomToken();
            String verifier = randomToken();
            ctx.sessionAttribute(stateSessionKey, state);
            ctx.sessionAttribute(verifierSessionKey, verifier);
            ctx.redirect(getLoginUrl(redirectUri, state, codeChallenge(verifier)));
        });
        javalinConfig.routes.get(getCallbackPath(), ctx -> {
            String expectedState = ctx.consumeSessionAttribute(stateSessionKey);
            String codeVerifier = ctx.consumeSessionAttribute(verifierSessionKey);
            String receivedState = ctx.queryParam("state");

            if (expectedState == null || receivedState == null || codeVerifier == null
                    || !constantTimeEquals(expectedState, receivedState)) {
                ctx.status(403).result("Invalid state");
                return;
            }

            String code = ctx.queryParam("code");
            if (code == null) {
                ctx.status(400).result("Missing code");
                return;
            }

            try {
                String accessToken = exchangeCodeForToken(code, codeVerifier);
                if (accessToken == null) {
                    ctx.result("Failed to get access token");
                    return;
                }

                User user = fetchUser(accessToken);
                if (user == null) {
                    ctx.result("Failed to fetch user");
                    return;
                }

                if (config.auth.getAuthProcessor() != null) {
                    config.auth.getAuthProcessor().handle(user, ctx);
                }

                sessionStore.setUser(ctx, user);
                ctx.redirect("/");
            } catch (IOException e) {
                FusionKit.getLogger(getClass()).error("OAuth2 callback failed", e);
                ctx.result("OAuth2 callback failed");
            }
        });
        AuthRouteRegistrar.registerLogout(javalinConfig, sessionStore);
    }

    @Override
    public String getLoginUrl(String redirectUri, String state) {
        return getLoginUrl(redirectUri, state, null);
    }

    protected String getLoginUrl(String redirectUri, String state, String codeChallenge) {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(getScope(), StandardCharsets.UTF_8);

        StringBuilder url = new StringBuilder(getAuthorizeUrl())
                .append("?client_id=").append(providerConfig.getClientId())
                .append("&redirect_uri=").append(encodedRedirectUri)
                .append("&response_type=code");

        if (!getScope().isBlank()) {
            url.append("&scope=").append(encodedScope);
        }
        if (state != null && !state.isBlank()) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        if (codeChallenge != null && !codeChallenge.isBlank()) {
            url.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8));
            url.append("&code_challenge_method=S256");
        }

        return url.toString();
    }

    protected User fetchUser(String accessToken) throws IOException {
        JsonObject userJson = fetchUserJson(accessToken);
        if (userJson == null) {
            return null;
        }

        return mapUser(userJson);
    }

    protected JsonObject fetchUserJson(String accessToken) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(getUserUrl())
                .header("Authorization", "Bearer " + accessToken);
        addUserRequestHeaders(requestBuilder);

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            return JsonParser.parseString(body.string()).getAsJsonObject();
        }
    }

    private String exchangeCodeForToken(String code, String codeVerifier) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("client_id", providerConfig.getClientId())
                .add("client_secret", providerConfig.getClientSecret())
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("code_verifier", codeVerifier)
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(getTokenUrl())
                .post(form)
                .header("Content-Type", "application/x-www-form-urlencoded");
        addTokenRequestHeaders(requestBuilder);

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
            if (!json.has("access_token")) {
                return null;
            }
            return json.get("access_token").getAsString();
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String codeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private String getLoginPath() {
        return "/auth/" + providerId + "/login";
    }

    private String getCallbackPath() {
        return "/auth/" + providerId + "/callback";
    }

    private static String buildRedirectUri(WebAppConfig config, String callbackPath) {
        String baseUrl = config.getDomain();
        if (ProductionLevel.isInDevelopment(config.getProductionLevel())) {
            baseUrl += ":" + config.getPort();
        }
        return baseUrl + callbackPath;
    }
}