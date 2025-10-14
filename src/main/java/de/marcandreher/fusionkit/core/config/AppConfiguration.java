package de.marcandreher.fusionkit.core.config;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import de.marcandreher.fusionkit.core.app.FileStructureManager;

public class AppConfiguration {

    private Object config;

    public AppConfiguration(String file, Object config, Logger logger) {
        FileStructureManager configDirectory = new FileStructureManager(FileStructureManager.DirectoryType.CONFIG);
        configDirectory.persist();

        File configFile = new File(configDirectory.getDirectory(), file);
        if (!configFile.exists()) {
            TomlWriter writer = new TomlWriter();
            try {
                writer.write(config, configFile);
            } catch (IOException e) {
                logger.error("Failed to write config file", e);
            }
        }

        Toml toml = new Toml().read(configFile);
        this.config = toml.to(config.getClass());
        logger.info("Loaded configuration from /.config/{}", configFile.getName());
    }

    public Object getModel() {
        return config;
    }


}
