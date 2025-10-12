package de.marcandreher.fusionkit.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to access application version information.
 */
public class VersionInfo {

    private static final Logger logger = LoggerFactory.getLogger(VersionInfo.class);
    
    private static final Properties properties = new Properties();
    // Fixed spelling of the properties file name
    private static final String VERSION_PROPERTIES_FILE = "version.properties";
    
    private static String version = "unknown";
    private static String name = "unknown";
    private static String buildTimestamp = "unknown";
    private static String javalinVersion = "unknown";
    private static String freemarkerVersion = "unknown";
    private static String okhttpVersion = "unknown";
    private static String mysqlConnectorVersion = "unknown";
    private static String hikariVersion = "unknown";
    private static String logbackVersion = "unknown";
    private static String gsonVersion = "unknown";
    private static String lombokVersion = "unknown";
    private static String tomljVersion = "unknown";
    
    public static void loadVersionProperties() {
        try (InputStream inputStream = VersionInfo.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                version = properties.getProperty("application.version", "unknown");
                name = properties.getProperty("application.name", "unknown");
                buildTimestamp = properties.getProperty("application.build.timestamp", "unknown");
                javalinVersion = properties.getProperty("javalin.version", "unknown");
                freemarkerVersion = properties.getProperty("freemarker.version", "unknown");
                okhttpVersion = properties.getProperty("okhttp.version", "unknown");
                mysqlConnectorVersion = properties.getProperty("mysql.connector.version", "unknown");
                hikariVersion = properties.getProperty("hikari.version", "unknown");
                logbackVersion = properties.getProperty("logback.version", "unknown");
                gsonVersion = properties.getProperty("gson.version", "unknown");
                lombokVersion = properties.getProperty("lombok.version", "unknown");
                tomljVersion = properties.getProperty("tomlj.version", "unknown");
            } else {
                logger.error("Version properties file not found");
            }
        } catch (IOException e) {
            logger.error("Error loading version properties: " + e.getMessage());
        }
    }
    
    /**
     * Gets the application version.
     * 
     * @return The application version from Maven's project.version
     */
    public static String getVersion() {
        return version;
    }
    
    /**
     * Gets the application name.
     * 
     * @return The application name from Maven's project.name
     */
    public static String getName() {
        return name;
    }
    
    /**
     * Gets the build timestamp.
     * 
     * @return The timestamp when the application was built
     */
    public static String getBuildTimestamp() {
        return buildTimestamp;
    }

    /**
     * Gets the Javalin version.
     * 
     * @return The Javalin version used by the application
     */
    public static String getJavalinVersion() {
        return javalinVersion;      
    }

    /**
     * Gets the Freemarker version.
     * 
     * @return The Freemarker version used by the application
     */
    public static String getFreemarkerVersion() {
        return freemarkerVersion;
    }

    /**
     * Gets the OkHttp version.
     * 
     * @return The OkHttp version used by the application
     */
    public static String getOkhttpVersion() {
        return okhttpVersion;
    }

    /**
     * Gets the MySQL Connector version.
     * 
     * @return The MySQL Connector version used by the application
     */
    public static String getMysqlConnectorVersion() {
        return mysqlConnectorVersion;
    }

    /**
     * Gets the HikariCP version.
     * 
     * @return The HikariCP version used by the application
     */
    public static String getHikariVersion() {
        return hikariVersion;
    }

    /**
     * Gets the Logback version.
     * 
     * @return The Logback version used by the application
     */
    public static String getLogbackVersion() {
        return logbackVersion;
    }

    /**
     * Gets the Gson version.
     * 
     * @return The Gson version used by the application
     */
    public static String getGsonVersion() {
        return gsonVersion;
    }

    /**
     * Gets the Lombok version.
     * 
     * @return The Lombok version used by the application
     */
    public static String getLombokVersion() {
        return lombokVersion;
    }

    /**
     * Gets the TOML-J version.
     * 
     * @return The TOML-J version used by the application
     */
    public static String getTomljVersion() {
        return tomljVersion;
    }

    /**
     * Gets all dependency versions as a formatted string.
     * 
     * @return A formatted string with all dependency versions
     */
    public static String getAllDependencyVersions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dependencies:\n");
        sb.append("  Javalin: ").append(javalinVersion).append("\n");
        sb.append("  Freemarker: ").append(freemarkerVersion).append("\n");
        sb.append("  OkHttp: ").append(okhttpVersion).append("\n");
        sb.append("  MySQL Connector: ").append(mysqlConnectorVersion).append("\n");
        sb.append("  HikariCP: ").append(hikariVersion).append("\n");
        sb.append("  Logback: ").append(logbackVersion).append("\n");
        sb.append("  Gson: ").append(gsonVersion).append("\n");
        sb.append("  Lombok: ").append(lombokVersion).append("\n");
        sb.append("  TOML-J: ").append(tomljVersion);
        return sb.toString();
    }

    /**
     * Checks if version information has been loaded successfully.
     * 
     * @return true if version info is loaded, false if still unknown
     */
    public static boolean isVersionLoaded() {
        return !version.equals("unknown") && !name.equals("unknown");
    }

    /**
     * Gets a formatted version string.
     * 
     * @return A formatted version string containing name, version and build timestamp
     */
    public static String getFormattedVersion() {
        return String.format("%s version %s (built on %s)", name, version, buildTimestamp);
    }
}