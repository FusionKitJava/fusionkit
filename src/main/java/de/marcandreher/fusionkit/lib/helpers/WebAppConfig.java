package de.marcandreher.fusionkit.lib.helpers;

import de.marcandreher.fusionkit.lib.javalin.ProductionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebAppConfig {
    private String name;
    private int port;
    private String domain;
    private ProductionLevel productionLevel;
    
    // Static files configuration
    @Builder.Default
    private boolean staticfiles = false;
    @Builder.Default
    private String staticFilesDirectory = "public";
    @Builder.Default
    private String staticFilesPath = "/";
    @Builder.Default
    private boolean staticFilesExternal = true; // true = external directory, false = classpath
    
    // Freemarker configuration
    @Builder.Default
    private boolean freemarker = false;
    @Builder.Default
    private String templatesDirectory = "templates";
    @Builder.Default
    private String templatesEncoding = "UTF-8";
    @Builder.Default
    private boolean templatesAutoReload = true;
    
    // Logging configuration
    @Builder.Default
    private boolean requestLogging = true;
    @Builder.Default
    private String logFormat = "[{method}] | <{host}{path}> | <{status}> | <{ms}ms>";
    
    // CORS configuration
    @Builder.Default
    private boolean corsEnabled = false;
    @Builder.Default
    private String corsOrigins = "*";
    @Builder.Default
    private String corsMethods = "GET,POST,PUT,DELETE,OPTIONS";
    @Builder.Default
    private String corsHeaders = "Content-Type,Authorization";
    
    // Server configuration
    @Builder.Default
    private boolean showBanner = true;
    @Builder.Default
    private int maxRequestSize = 1024 * 1024; // 1MB
    
    // SSL/TLS configuration
    @Builder.Default
    private boolean sslEnabled = false;
    private String keystorePath;
    private String keystorePassword;
    
    // Session configuration
    @Builder.Default
    private boolean sessionsEnabled = false;
    @Builder.Default
    private int sessionTimeoutMinutes = 30;
    private String sessionCookieName;

}
