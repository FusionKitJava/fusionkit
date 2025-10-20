package de.marcandreher.fusionkit.core.auth.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonSyntaxException;

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

            File configFile = new File(configDir, "discord.json");
            if (configFile.exists()) {
                FusionKit.getLogger(DiscordConfig.class).debug("Config file found. Reading: " + configFile.getAbsolutePath());
                return FusionKit.getGson().fromJson(Files.readString(configFile.toPath()), DiscordConfig.class);
            } else {
                FusionKit.getLogger(DiscordConfig.class).debug("Config file not found. Creating default config: " + configFile.getAbsolutePath());
                DiscordConfig defaultConfig = createDefaultConfig();
                Files.writeString(configFile.toPath(), FusionKit.getGson().toJson(defaultConfig));
                return defaultConfig;
            }
        } catch (JsonSyntaxException e) {
            FusionKit.getLogger(DiscordConfig.class).error("JSON syntax error while loading DiscordConfig", e);
        } catch (IOException e) {
            FusionKit.getLogger(DiscordConfig.class).error("IO error while loading DiscordConfig", e);
        } catch (Exception e) {
            FusionKit.getLogger(DiscordConfig.class).error("Unexpected error while loading DiscordConfig", e);
        }

        // Return a default instance in case of failure
        return createDefaultConfig();
    }
}
