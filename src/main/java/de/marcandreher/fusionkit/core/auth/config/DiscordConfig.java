package de.marcandreher.fusionkit.core.auth.config;

import lombok.Data;

@Data
public class DiscordConfig implements OAuth2ProviderConfig {
    private String clientId = "empty";
    private String clientSecret = "empty";

    private static DiscordConfig createDefaultConfig() {
        DiscordConfig defaultConfig = new DiscordConfig();
        defaultConfig.setClientId("default-client-id");
        defaultConfig.setClientSecret("default-client-secret");
        return defaultConfig;
    }

    public static DiscordConfig loadConfig() {
        return AuthConfigLoader.loadTomlConfig(DiscordConfig.class, "discord.toml", DiscordConfig.class, DiscordConfig::createDefaultConfig);
    }
}
