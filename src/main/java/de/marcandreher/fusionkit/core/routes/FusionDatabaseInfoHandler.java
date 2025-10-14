package de.marcandreher.fusionkit.core.routes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import com.zaxxer.hikari.HikariPoolMXBean;

import de.marcandreher.fusionkit.core.config.WebAppConfig;
import de.marcandreher.fusionkit.core.database.Database;
import de.marcandreher.fusionkit.core.database.MySQL;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import lombok.AllArgsConstructor;
import lombok.Data;

public class FusionDatabaseInfoHandler implements Handler {
    
    private WebAppConfig config;

    public FusionDatabaseInfoHandler(WebAppConfig config) {
        this.config = config;
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.contentType("text/html");
        String debugHtml = buildDatabaseInfoPage();
        if(config.isDebugger()) {
            debugHtml += ctx.attribute("debugHtml");
        }

        ctx.result(debugHtml);
    }

    private String buildDatabaseInfoPage() {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>")
            .append("<html lang='en'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>FusionKit Database Info</title>")
            .append("<style>")
            .append(getDatabasePageStyles())
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<h1>FusionKit Database Information</h1>")
            .append("<a href='/fusion' class='nav-link'>← Back to System Info</a>")
            .append(buildConnectionPoolInfo())
            .append(buildActiveConnectionsInfo())
            .append(buildDatabaseConfigInfo())
            .append(buildConnectionHistoryInfo())
            .append("</body>")
            .append("</html>");
            
        return html.toString();
    }

    private String getDatabasePageStyles() {
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
            .status-active {
                color: #28a745;
                font-weight: bold;
            }
            .status-inactive {
                color: #dc3545;
                font-weight: bold;
            }
            .status-warning {
                color: #ffc107;
                font-weight: bold;
            }
            .connection-id {
                color: #007bff;
                font-weight: bold;
            }
            .timestamp {
                color: #6c757d;
                font-family: monospace;
                font-size: 12px;
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

    private String buildConnectionPoolInfo() {
        if (Database.dataSource == null) {
            return """
                <table>
                    <thead>
                        <tr class="section-header">
                            <td colspan="2">Connection Pool Status</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><span class="label">Status:</span></td>
                            <td><span class="value status-inactive">Not Connected</span></td>
                        </tr>
                    </tbody>
                </table>
            """;
        }

        HikariPoolMXBean poolBean = Database.dataSource.getHikariPoolMXBean();
        
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Connection Pool Status</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">Pool Status:</span></td>
                        <td><span class="value status-active">Active</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">JDBC URL:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Active Connections:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Idle Connections:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Total Connections:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Threads Awaiting Connection:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Maximum Pool Size:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Minimum Pool Size:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            Database.dataSource.getJdbcUrl(),
            poolBean.getActiveConnections(),
            poolBean.getIdleConnections(),
            poolBean.getTotalConnections(),
            poolBean.getThreadsAwaitingConnection(),
            Database.dataSource.getMaximumPoolSize(),
            Database.dataSource.getMinimumIdle()
        );
    }

    private String buildActiveConnectionsInfo() {
        ArrayList<MySQLConnectionInfo> connections = new ArrayList<>();
        for (MySQL sql : Database.runningConnections) {
            connections.add(new MySQLConnectionInfo(sql.getCaller(), sql.getConnectionCreated()));
        }

        StringBuilder html = new StringBuilder();
        html.append("""
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="4">Active Connections</td>
                    </tr>
                    <tr>
                        <td>ID</td>
                        <td>Caller Class</td>
                        <td>Created</td>
                        <td>Duration</td>
                    </tr>
                </thead>
                <tbody>
        """);

        if (connections.isEmpty()) {
            html.append("""
                    <tr>
                        <td colspan="4" style="text-align: center; font-style: italic; color: #6c757d;">
                            No active connections
                        </td>
                    </tr>
            """);
        } else {
            for (int i = 0; i < connections.size(); i++) {
                MySQLConnectionInfo conn = connections.get(i);
                long duration = System.currentTimeMillis() - conn.getCreated();
                html.append("""
                        <tr>
                            <td><span class="connection-id">%d</span></td>
                            <td><span class="value">%s</span></td>
                            <td><span class="timestamp">%s</span></td>
                            <td><span class="value">%s</span></td>
                        </tr>
                """.formatted(
                    i + 1,
                    conn.getName(),
                    formatTimestamp(conn.getCreated()),
                    formatDuration(duration)
                ));
            }
        }

        html.append("""
                </tbody>
            </table>
        """);
        return html.toString();
    }

    private String buildDatabaseConfigInfo() {
        if (Database.config == null) {
            return """
                <table>
                    <thead>
                        <tr class="section-header">
                            <td colspan="2">Database Configuration</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td colspan="2" style="text-align: center; font-style: italic; color: #6c757d;">
                                Configuration not loaded
                            </td>
                        </tr>
                    </tbody>
                </table>
            """;
        }

        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Database Configuration</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">SQL Logging:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Auto Commit:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Cache Prepared Statements:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Prepared Statement Cache Size:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Connection Timeout:</span></td>
                        <td><span class="value">%d ms</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Idle Timeout:</span></td>
                        <td><span class="value">%d ms</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Max Lifetime:</span></td>
                        <td><span class="value">%d ms</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Character Encoding:</span></td>
                        <td><span class="value">%s</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Use SSL:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            Database.config.isLogSql() ? "status-active" : "status-inactive",
            Database.config.isLogSql() ? "Enabled" : "Disabled",
            Database.config.isAutoCommit() ? "status-active" : "status-inactive",
            Database.config.isAutoCommit() ? "Enabled" : "Disabled",
            Database.config.isCachePreparedStatements() ? "status-active" : "status-inactive",
            Database.config.isCachePreparedStatements() ? "Enabled" : "Disabled",
            Database.config.getPreparedStatementCacheSize(),
            Database.config.getConnectionTimeout(),
            Database.config.getIdleTimeout(),
            Database.config.getMaxLifetime(),
            Database.config.getCharacterEncoding(),
            Database.config.isUseSSL() ? "status-active" : "status-inactive",
            Database.config.isUseSSL() ? "Enabled" : "Disabled"
        );
    }

    private String buildConnectionHistoryInfo() {
        return """
            <table>
                <thead>
                    <tr class="section-header">
                        <td colspan="2">Connection Statistics</td>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="label">Current Active Connections:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Total Connections Created:</span></td>
                        <td><span class="value">%d</span></td>
                    </tr>
                    <tr>
                        <td><span class="label">Connection Pool Available:</span></td>
                        <td><span class="value %s">%s</span></td>
                    </tr>
                </tbody>
            </table>
        """.formatted(
            Database.runningConnections.size(),
            Database.currentConnections,
            Database.dataSource != null ? "status-active" : "status-inactive",
            Database.dataSource != null ? "Yes" : "No"
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    @Data
    @AllArgsConstructor
    public static class MySQLConnectionInfo {
        private String name;
        private long created;
    }
}
