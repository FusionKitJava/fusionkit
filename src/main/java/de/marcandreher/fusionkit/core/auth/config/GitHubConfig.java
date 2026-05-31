package de.marcandreher.fusionkit.core.auth.config;

import lombok.Data;

@Data
public class GitHubConfig implements OAuth2ProviderConfig {
    private String clientId = "empty";
    private String clientSecret = "empty";

    private static GitHubConfig createDefaultConfig() {
        GitHubConfig defaultConfig = new GitHubConfig();
        defaultConfig.setClientId("default-client-id");
        defaultConfig.setClientSecret("default-client-secret");
        return defaultConfig;
    }

    public static GitHubConfig loadConfig() {
        return AuthConfigLoader.loadTomlConfig(GitHubConfig.class, "github.toml", GitHubConfig.class, GitHubConfig::createDefaultConfig);
    }
}
