package de.marcandreher.fusionkit.core.auth.config;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import de.marcandreher.fusionkit.core.FusionKit;

public final class AuthConfigLoader {

    private AuthConfigLoader() {
    }

    public static <T> T loadTomlConfig(Class<?> logClass, String fileName, Class<T> type, Supplier<T> defaultSupplier) {
        final Logger logger = FusionKit.getLogger(logClass);
        try {
            logger.debug("Loading auth config: {}", fileName);

            File configDir = new File(".config/auth");
            if (!configDir.exists()) {
                logger.debug("Config directory does not exist. Attempting to create: {}", configDir.getAbsolutePath());
                if (!configDir.mkdirs()) {
                    throw new IOException("Failed to create config directory: " + configDir.getAbsolutePath());
                }
            }

            File configFile = new File(configDir, fileName);
            if (configFile.exists()) {
                logger.debug("Config file found. Reading: {}", configFile.getAbsolutePath());
                Toml toml = new Toml().read(configFile);
                T config = toml.to(type);
                return config != null ? config : defaultSupplier.get();
            }

            logger.debug("Config file not found. Creating default config: {}", configFile.getAbsolutePath());
            T defaultConfig = defaultSupplier.get();
            TomlWriter writer = new TomlWriter();
            writer.write(defaultConfig, configFile);
            return defaultConfig;
        } catch (IOException e) {
            logger.error("IO error while loading auth config", e);
        } catch (Exception e) {
            logger.error("Unexpected error while loading auth config", e);
        }

        return defaultSupplier.get();
    }
}
