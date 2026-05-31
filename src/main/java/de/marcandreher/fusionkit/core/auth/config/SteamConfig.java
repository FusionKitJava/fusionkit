package de.marcandreher.fusionkit.core.auth.config;

import lombok.Data;

@Data
public class SteamConfig {
    private String apiKey = "empty";

    private static SteamConfig createDefaultConfig() {
        SteamConfig defaultConfig = new SteamConfig();
        defaultConfig.setApiKey("default-api-key");
        return defaultConfig;
    }

    public static SteamConfig loadConfig() {
        return AuthConfigLoader.loadTomlConfig(SteamConfig.class, "steam.toml", SteamConfig.class, SteamConfig::createDefaultConfig);
    }
}
