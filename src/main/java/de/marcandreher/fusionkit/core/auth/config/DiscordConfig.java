package de.marcandreher.fusionkit.core.auth.config;

import java.io.File;
import java.io.IOException;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import de.marcandreher.fusionkit.core.FusionKit;
import lombok.Data;

@Data
public class DiscordConfig {
    private String clientId = "empty";
    private String clientSecret = "empty";

    private static DiscordConfig createDefaultConfig() {
        DiscordConfig defaultConfig = new DiscordConfig();
        defaultConfig.setClientId("default-client-id");
        defaultConfig.setClientSecret("default-client-secret");
        return defaultConfig;
    }

    public static DiscordConfig loadConfig() {
        try {
            FusionKit.getLogger(DiscordConfig.class).debug("Loading DiscordConfig...");

            File configDir = new File(".config/auth");
            if (!configDir.exists()) {
                FusionKit.getLogger(DiscordConfig.class).debug("Config directory does not exist. Attempting to create: " + configDir.getAbsolutePath());
                if (!configDir.mkdirs()) {
                    throw new IOException("Failed to create config directory: " + configDir.getAbsolutePath());
                }
            }

            File configFile = new File(configDir, "discord.toml");
            if (configFile.exists()) {
                FusionKit.getLogger(DiscordConfig.class).debug("Config file found. Reading: " + configFile.getAbsolutePath());
                Toml toml = new Toml().read(configFile);
                DiscordConfig config = toml.to(DiscordConfig.class);
                return config != null ? config : createDefaultConfig();
            } else {
                FusionKit.getLogger(DiscordConfig.class).debug("Config file not found. Creating default config: " + configFile.getAbsolutePath());
                DiscordConfig defaultConfig = createDefaultConfig();
                TomlWriter writer = new TomlWriter();
                writer.write(defaultConfig, configFile);
                return defaultConfig;
            }
        } catch (IOException e) {
            FusionKit.getLogger(DiscordConfig.class).error("IO error while loading DiscordConfig", e);
        } catch (Exception e) {
            FusionKit.getLogger(DiscordConfig.class).error("Unexpected error while loading DiscordConfig", e);
        }

        // Return a default instance in case of failure
        return createDefaultConfig();
    }
}
