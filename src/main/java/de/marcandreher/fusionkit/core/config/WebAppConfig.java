package de.marcandreher.fusionkit.core.config;

import java.util.Locale;

import de.marcandreher.fusionkit.core.auth.AfterLoginHandler;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.javalin.JavalinConfigurator;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
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

    // Localized configuration
    @Builder.Default
    private boolean i18n = false;

    @Builder.Default
    private String i18nDirectory = "i18n";

    @Builder.Default
    private Locale i18nDefaultLanguage = Locale.ENGLISH;

    @Builder.Default
    private boolean debugger = false;

    @Builder.Default
    private JavalinConfigurator javalinConfigurator = null;
    
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

    @Builder.Default
    private boolean auth = false;
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.NONE;
    @Builder.Default
    private long authSessionInterval = 24 * 60 * 60 * 1000; // 24 hours
    @Builder.Default
    private AfterLoginHandler authHandler = null;

    // Logging configuration
    @Builder.Default
    private boolean requestLogging = true;
    @Builder.Default
    private String logFormat = "[{method}] | <{host}{path}> | <{status}> | <{ms}ms> | <{agent}>";
    
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WebAppConfig [");
        boolean first = true;
        
        // Always show required fields (no defaults)
        if (name != null) {
            sb.append("name=").append(name);
            first = false;
        }
        if (port != 0) {
            if (!first) sb.append(", ");
            sb.append("port=").append(port);
            first = false;
        }
        if (domain != null) {
            if (!first) sb.append(", ");
            sb.append("domain=").append(domain);
            first = false;
        }
        if (productionLevel != null) {
            if (!first) sb.append(", ");
            sb.append("productionLevel=").append(productionLevel);
            first = false;
        }
        
        // Only show fields that differ from defaults
        if (i18n != false) {
            if (!first) sb.append(", ");
            sb.append("i18n=").append(i18n);
            first = false;
        }
        if (!"i18n".equals(i18nDirectory)) {
            if (!first) sb.append(", ");
            sb.append("i18nDirectory=").append(i18nDirectory);
            first = false;
        }
        if (!Locale.ENGLISH.equals(i18nDefaultLanguage)) {
            if (!first) sb.append(", ");
            sb.append("i18nDefaultLanguage=").append(i18nDefaultLanguage);
            first = false;
        }
        if (debugger != false) {
            if (!first) sb.append(", ");
            sb.append("debugger=").append(debugger);
            first = false;
        }
        if (staticfiles != false) {
            if (!first) sb.append(", ");
            sb.append("staticfiles=").append(staticfiles);
            first = false;
        }
        if (!"public".equals(staticFilesDirectory)) {
            if (!first) sb.append(", ");
            sb.append("staticFilesDirectory=").append(staticFilesDirectory);
            first = false;
        }
        if (!"/".equals(staticFilesPath)) {
            if (!first) sb.append(", ");
            sb.append("staticFilesPath=").append(staticFilesPath);
            first = false;
        }
        if (staticFilesExternal != true) {
            if (!first) sb.append(", ");
            sb.append("staticFilesExternal=").append(staticFilesExternal);
            first = false;
        }
        if (freemarker != false) {
            if (!first) sb.append(", ");
            sb.append("freemarker=").append(freemarker);
            first = false;
        }
        if (!"templates".equals(templatesDirectory)) {
            if (!first) sb.append(", ");
            sb.append("templatesDirectory=").append(templatesDirectory);
            first = false;
        }
        if (!"UTF-8".equals(templatesEncoding)) {
            if (!first) sb.append(", ");
            sb.append("templatesEncoding=").append(templatesEncoding);
            first = false;
        }
        if (templatesAutoReload != true) {
            if (!first) sb.append(", ");
            sb.append("templatesAutoReload=").append(templatesAutoReload);
            first = false;
        }
        if (requestLogging != true) {
            if (!first) sb.append(", ");
            sb.append("requestLogging=").append(requestLogging);
            first = false;
        }
        if (!"[{method}] | <{host}{path}> | <{status}> | <{ms}ms> | <{agent}>".equals(logFormat)) {
            if (!first) sb.append(", ");
            sb.append("logFormat=").append(logFormat);
            first = false;
        }
        if (corsEnabled != false) {
            if (!first) sb.append(", ");
            sb.append("corsEnabled=").append(corsEnabled);
            first = false;
        }
        if (!"*".equals(corsOrigins)) {
            if (!first) sb.append(", ");
            sb.append("corsOrigins=").append(corsOrigins);
            first = false;
        }
        if (!"GET,POST,PUT,DELETE,OPTIONS".equals(corsMethods)) {
            if (!first) sb.append(", ");
            sb.append("corsMethods=").append(corsMethods);
            first = false;
        }
        if (!"Content-Type,Authorization".equals(corsHeaders)) {
            if (!first) sb.append(", ");
            sb.append("corsHeaders=").append(corsHeaders);
            first = false;
        }
        if (showBanner != true) {
            if (!first) sb.append(", ");
            sb.append("showBanner=").append(showBanner);
            first = false;
        }
        if (maxRequestSize != 1024 * 1024) {
            if (!first) sb.append(", ");
            sb.append("maxRequestSize=").append(maxRequestSize);
            first = false;
        }
        if (sslEnabled != false) {
            if (!first) sb.append(", ");
            sb.append("sslEnabled=").append(sslEnabled);
            first = false;
        }
        if (keystorePath != null) {
            if (!first) sb.append(", ");
            sb.append("keystorePath=****");
            first = false;
        }
        if (keystorePassword != null) {
            if (!first) sb.append(", ");
            sb.append("keystorePassword=****");
            first = false;
        }
        if (sessionsEnabled != false) {
            if (!first) sb.append(", ");
            sb.append("sessionsEnabled=").append(sessionsEnabled);
            first = false;
        }
        if (sessionTimeoutMinutes != 30) {
            if (!first) sb.append(", ");
            sb.append("sessionTimeoutMinutes=").append(sessionTimeoutMinutes);
            first = false;
        }
        if (sessionCookieName != null) {
            if (!first) sb.append(", ");
            sb.append("sessionCookieName=").append(sessionCookieName);
            first = false;
        }
        
        sb.append("]");
        return sb.toString();
    }

}
