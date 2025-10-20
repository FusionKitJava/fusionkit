package de.marcandreher.fusionkit.core.auth;

import de.marcandreher.fusionkit.core.FusionKit;
import okhttp3.OkHttpClient;

public interface LoginHandler {
    
    public static final OkHttpClient httpClient = FusionKit.getHttpClient();

    public void registerRoutes();

    public String getLoginUrl(String redirectUri, String state);

}
