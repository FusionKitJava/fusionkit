package de.marcandreher.fusionkit.core.app;

import java.io.File;

import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;

public class FileStructureManager {

    private Logger logger = FusionKit.getLogger(FileStructureManager.class);

    public static enum DirectoryType {
        DATA("data"),
        LOGS("logs"),
        CONFIG(".config"),
        TEMPLATES("templates"),
        PUBLIC("public");

        private String directoryName;

        DirectoryType(String directoryName) {
            this.directoryName = directoryName;
        }

        public String getDirectoryName() {
            return directoryName;
        }
    }

    private DirectoryType type;

    /**
     * Initializes the directory structure for the application.
     * Creates necessary directories if they do not exist.
     *
     * @param type The type of directory to manage.
     */

    public FileStructureManager(DirectoryType type) {
        this.type = type;
    }

    public void persist() {
        java.io.File dir = new java.io.File(type.getDirectoryName());
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.debug("Created directory: " + type.getDirectoryName());
            } else {
                logger.error("Failed to create directory: " + type.getDirectoryName());
            }
        }
    }

    public File getDirectory() {
        return new File(type.getDirectoryName());
    }



    
}
