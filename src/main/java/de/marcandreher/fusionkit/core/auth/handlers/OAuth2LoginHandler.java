package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.AuthRouteRegistrar;
import de.marcandreher.fusionkit.core.auth.LoginHandler;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.OAuth2ProviderConfig;
import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;
import de.marcandreher.fusionkit.core.config.WebAppConfig;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class OAuth2LoginHandler implements LoginHandler {

    private static final String DEFAULT_STATE = "state123";

    protected final WebApp app;
    protected final WebAppConfig config;
    protected final AuthSessionStore sessionStore;
    protected final OAuth2ProviderConfig providerConfig;
    protected final String redirectUri;

    private final String providerId;

    protected OAuth2LoginHandler(WebApp app, OAuth2ProviderConfig providerConfig, String providerId) {
        this.app = app;
        this.config = app.getConfig();
        this.sessionStore = config.getAuthSessionStore();
        this.providerConfig = providerConfig;
        this.providerId = providerId;
        this.redirectUri = buildRedirectUri(config, getCallbackPath());
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
    public void registerRoutes() {
        FusionKit.getLogger(getClass()).info("Registering {} OAuth2 login handler", getProviderName());
        app.getApp().before("/*", ctx -> {
            ctx.attribute("user", sessionStore.getUser(ctx));
            Map<String, String> authUrls = ctx.attribute("authUrls");
            if (authUrls == null) {
                authUrls = new LinkedHashMap<>();
                ctx.attribute("authUrls", authUrls);
            }
            authUrls.put(getProviderId(), getLoginUrl(redirectUri, DEFAULT_STATE));
            if (ctx.attribute("url") == null) {
                ctx.attribute("url", authUrls.get(getProviderId()));
            }
        });
        app.getApp().get(getCallbackPath(), ctx -> {
            String code = ctx.queryParam("code");
            if (code == null) {
                ctx.result("Missing code");
                return;
            }

            try {
                String accessToken = exchangeCodeForToken(code);
                if (accessToken == null) {
                    ctx.result("Failed to get access token");
                    return;
                }

                User user = fetchUser(accessToken);
                if (user == null) {
                    ctx.result("Failed to fetch user");
                    return;
                }

                if (config.getAuthHandler() != null) {
                    config.getAuthHandler().handle(user, ctx);
                }

                sessionStore.setUser(ctx, user);
                ctx.redirect("/");
            } catch (IOException e) {
                FusionKit.getLogger(getClass()).error("OAuth2 callback failed", e);
                ctx.result("OAuth2 callback failed");
            }
        });
        AuthRouteRegistrar.registerLogout(app, sessionStore);
    }

    @Override
    public String getLoginUrl(String redirectUri, String state) {
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

    private String exchangeCodeForToken(String code) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("client_id", providerConfig.getClientId())
                .add("client_secret", providerConfig.getClientSecret())
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
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
