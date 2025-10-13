package de.marcandreher.fusionkit.core.routes;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.jetbrains.annotations.NotNull;

import de.marcandreher.fusionkit.FusionKit;
import de.marcandreher.fusionkit.lib.helpers.WebAppConfig;
import de.marcandreher.fusionkit.util.VersionInfo;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class FusionInfoHandler implements Handler {
    
    private final WebAppConfig config;

    public FusionInfoHandler(WebAppConfig config) {
        this.config = config;
    }
    
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.contentType("text/html");
        ctx.result(buildInfoPage() + ctx.attribute("debugHtml"));
    }
    
    private String buildInfoPage() {
        StringBuilder html = new StringBuilder();

        boolean hasDb = (FusionKit.database != null);

        html.append("<!DOCTYPE html>")
            .append("<html lang='en'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>FusionKit Info</title>")
            .append("<style>")
            .append(getInfoPageStyles())
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<h1>FusionKit Application Info</h1>")
            .append(hasDb ? "<a style='margin-right: 10px;' href='/fusion/database' class='nav-link'>View Database</a>" : "")
            .append("<a href='/routes' class='nav-link'>View Routes</a>")
            .append(buildSystemInfo())
            .append(buildDependencyInfo())
            .append(buildConfigInfo())
            .append(buildRuntimeInfo())
            .append("</body>")
            .append("</html>");
            
        return html.toString();
    }
    
    private String getInfoPageStyles() {
        return """
            * {
                box-sizing: border-box;
            }
            b, thead {
                font-weight: 700;
            }
            html {
                background: #363e4c;
            }
            body {
                font-family: monospace;
                padding: 25px;
                color: #fff;
            }
            h1 {
                color: #fff;
                text-align: center;
                margin-bottom: 30px;
                font-size: 28px;
            }
            .nav-link {
                display: inline-block;
                color: #5a76ff;
                text-decoration: none;
                margin-bottom: 20px;
                padding: 8px 16px;
                background: rgba(90, 118, 255, 0.1);
                border-radius: 4px;
                border: 1px solid #5a76ff;
            }
            .nav-link:hover {
                background: rgba(90, 118, 255, 0.2);
            }
            table {
                background: #fff;
                border-spacing: 0;
                font-size: 14px;
                width: 100%;
                white-space: pre;
                box-shadow: 0 5px 25px rgba(0,0,0,0.25);
                margin-bottom: 30px;
            }
            thead {
                background: #1a202b;
                color: #fff;
            }
            thead td {
                border-bottom: 2px solid #000;
                padding: 15px;
                font-weight: bold;
            }
            tr + tr td {
                border-top: 1px solid rgba(0, 0, 0, 0.25);
            }
            td {
                padding: 12px 15px;
            }
            tbody td {
                background-color: rgba(255,255,255,0.925);
                color: #333;
            }
            tbody tr:hover td {
                background-color: rgba(255,255,255,0.85);
            }
            .section-header {
                background: #1a202b;
                color: #fff;
                font-weight: bold;
                font-size: 16px;
            }
            .section-header td {
                padding: 15px;
                border-bottom: 2px solid #000;
            }
            .label {
                font-weight: bold;
                color: #495057;
                min-width: 200px;
                display: inline-block;
            }
            .value {
                color: #6c757d;
                font-family: monospace;
            }
            .version {
                color: #28a745;
                font-weight: bold;
            }
            .status-true {
                color: #28a745;
                font-weight: bold;
            }
            .status-false {
                color: #dc3545;
                font-weight: bold;
            }
            @media (max-width: 768px) {
                body {
                    padding: 15px;
                }
                table {
                    font-size: 12px;
                }
                .label {
                    min-width: 120px;
                }
            }
        """;
    }
    
    private String buildSystemInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">System Information</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">FusionKit Version:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">FusionKit Build Timestamp:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Java Version:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Java Vendor:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">JVM Name:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Operating System:</span></td>
                        <td><span class="value">%s %s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Architecture:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Uptime:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Start Time:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            VersionInfo.getVersion(),
            VersionInfo.getBuildTimestamp(),
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("java.vm.name"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            formatUptime(runtime.getUptime()),
            formatTimestamp(runtime.getStartTime())
        );
    }
    
    private String buildDependencyInfo() {
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Dependencies</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">Javalin:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Freemarker:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">OkHttp:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">MySQL Connector:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">HikariCP:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Logback:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Gson:</span></td>
                        <td><span class="value version">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            VersionInfo.getJavalinVersion(),
            VersionInfo.getFreemarkerVersion(),
            VersionInfo.getOkhttpVersion(),
            VersionInfo.getMysqlConnectorVersion(),
            VersionInfo.getHikariVersion(),
            VersionInfo.getLogbackVersion(),
            VersionInfo.getGsonVersion()
        );
    }
    
    private String buildConfigInfo() {
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Application Configuration</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">App Name:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Domain:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Port:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Production Level:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Show Banner:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Request Logging:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">CORS Enabled:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Static Files:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Freemarker:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            config.getName(),
            config.getDomain(),
            config.getPort(),
            config.getProductionLevel(),
            config.isShowBanner() ? "status-true" : "status-false",
            config.isShowBanner() ? "Enabled" : "Disabled",
            config.isRequestLogging() ? "status-true" : "status-false", 
            config.isRequestLogging() ? "Enabled" : "Disabled",
            config.isCorsEnabled() ? "status-true" : "status-false",
            config.isCorsEnabled() ? "Enabled" : "Disabled",
            config.isStaticfiles() ? "status-true" : "status-false",
            config.isStaticfiles() ? "Enabled" : "Disabled",
            config.isFreemarker() ? "status-true" : "status-false",
            config.isFreemarker() ? "Enabled" : "Disabled"
        );
    }
    
    private String buildRuntimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Runtime Information</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">Used Memory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Free Memory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Total Memory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Max Memory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Available Processors:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">User Directory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Working Directory:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Current Time:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            formatBytes(usedMemory),
            formatBytes(freeMemory),
            formatBytes(totalMemory),
            formatBytes(maxMemory),
            runtime.availableProcessors(),
            System.getProperty("user.home"),
            System.getProperty("user.dir"),
            formatTimestamp(System.currentTimeMillis())
        );
    }
    
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        }
    }
    
    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}