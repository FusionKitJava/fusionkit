package de.marcandreher.fusionkit.core.auth.handlers;

import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.auth.AuthHandler;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.User;
import de.marcandreher.fusionkit.core.auth.config.GitHubConfig;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@AuthHandler(AuthProvider.GITHUB)
public class GitHubLoginHandler extends OAuth2LoginHandler {

    private static final String GITHUB_API = "https://api.github.com";

    public GitHubLoginHandler(WebApp app, GitHubConfig config) {
        super(app, config, "github");
    }

    @Override
    protected String getAuthorizeUrl() {
        return "https://github.com/login/oauth/authorize";
    }

    @Override
    protected String getTokenUrl() {
        return "https://github.com/login/oauth/access_token";
    }

    @Override
    protected String getUserUrl() {
        return GITHUB_API + "/user";
    }

    @Override
    protected String getScope() {
        return "read:user user:email";
    }

    @Override
    protected String getProviderName() {
        return "GitHub";
    }

    @Override
    protected void addTokenRequestHeaders(Request.Builder builder) {
        builder.header("Accept", "application/json");
    }

    @Override
    protected void addUserRequestHeaders(Request.Builder builder) {
        builder.header("Accept", "application/vnd.github+json");
    }

    @Override
    protected User mapUser(JsonObject userJson) {
        User user = new User();
        user.setId(getString(userJson, "id"));
        user.setUsername(getString(userJson, "login"));
        user.setAvatar(getString(userJson, "avatar_url"));
        user.setEmail(getString(userJson, "email"));
        return user;
    }

    @Override
    protected User fetchUser(String accessToken) throws IOException {
        User user = super.fetchUser(accessToken);
        if (user == null || user.getEmail() != null && !user.getEmail().isBlank()) {
            return user;
        }

        String email = fetchPrimaryEmail(accessToken);
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }

        return user;
    }

    private String fetchPrimaryEmail(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(GITHUB_API + "/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            JsonArray emails = com.google.gson.JsonParser.parseString(body.string()).getAsJsonArray();
            String fallback = null;
            for (JsonElement element : emails) {
                JsonObject email = element.getAsJsonObject();
                String value = getString(email, "email");
                if (value == null) {
                    continue;
                }
                if (fallback == null) {
                    fallback = value;
                }
                boolean primary = email.has("primary") && email.get("primary").getAsBoolean();
                boolean verified = email.has("verified") && email.get("verified").getAsBoolean();
                if (primary && verified) {
                    return value;
                }
            }
            return fallback;
        }
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }
}
