package de.marcandreher.fusionkit.core.cmd.implementations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.cmd.Command;
import de.marcandreher.fusionkit.core.cmd.CommandInfo;

/**
 * Database command that displays comprehensive database connection pool statistics
 * and database information.
 */
@CommandInfo(name = "database", description = "Displays database connection pool statistics and information")
public class DatabaseCommand implements Command {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final String SEPARATOR = "─".repeat(80);

    @Override
    public void execute(String[] args) {
        if (FusionKit.database == null) {
            getLogger().error("Database is not initialized");
            return;
        }

        HikariDataSource dataSource = FusionKit.database.getDataSource();
        if (dataSource == null) {
            getLogger().error("DataSource is not available");
            return;
        }

        // Parse command arguments
        boolean showHelp = args.length > 0 && ("help".equals(args[0]) || "-h".equals(args[0]));
        boolean showDetailed = args.length > 0 && ("detailed".equals(args[0]) || "-d".equals(args[0]));

        if (showHelp) {
            showHelpMessage();
            return;
        }

        getLogger().info("[*] Database Status Report");
        getLogger().info(SEPARATOR);

        try {
            showConnectionPoolStats(dataSource);
            showDatabaseInfo(dataSource);
            
            if (showDetailed) {
                showDetailedMetrics(dataSource);
            }
            
        } catch (Exception e) {
            getLogger().error("Failed to retrieve database information: " + e.getMessage(), e);
        }

        getLogger().info(SEPARATOR);
        getLogger().info("(i) Use 'database detailed' for more metrics or 'database help' for usage info");
    }

    private void showHelpMessage() {
        getLogger().info("[?] Database Command Usage:");
        getLogger().info("  database           - Show basic database status and pool statistics");
        getLogger().info("  database detailed  - Show detailed metrics and configuration");
        getLogger().info("  database help      - Show this help message");
    }

    private void showConnectionPoolStats(HikariDataSource dataSource) {
        try {
            HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
            
            getLogger().info("[P] Connection Pool Statistics:");
            getLogger().info(String.format("  ├─ Active Connections:     %d", poolBean.getActiveConnections()));
            getLogger().info(String.format("  ├─ Idle Connections:       %d", poolBean.getIdleConnections()));
            getLogger().info(String.format("  ├─ Total Connections:      %d", poolBean.getTotalConnections()));
            getLogger().info(String.format("  ├─ Threads Awaiting:       %d", poolBean.getThreadsAwaitingConnection()));
            getLogger().info(String.format("  └─ Pool Name:              %s", dataSource.getPoolName()));

            // Calculate pool utilization percentage
            int maxPoolSize = dataSource.getMaximumPoolSize();
            int activeConnections = poolBean.getActiveConnections();
            double utilization = (double) activeConnections / maxPoolSize * 100;
            
            String utilizationStatus;
            if (utilization < 50) {
                utilizationStatus = "[OK] Low";
            } else if (utilization < 80) {
                utilizationStatus = "[!] Medium";
            } else {
                utilizationStatus = "[!!] High";
            }
            
            getLogger().info(String.format("  └─ Pool Utilization:       %.1f%% %s", utilization, utilizationStatus));
            
        } catch (Exception e) {
            getLogger().warn("Could not retrieve pool statistics: " + e.getMessage());
        }
    }

    private void showDatabaseInfo(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            getLogger().info("");
            getLogger().info("[D] Database Information:");
            getLogger().info(String.format("  ├─ Database Product:       %s", metaData.getDatabaseProductName()));
            getLogger().info(String.format("  ├─ Database Version:       %s", metaData.getDatabaseProductVersion()));
            getLogger().info(String.format("  ├─ JDBC Driver:           %s", metaData.getDriverName()));
            getLogger().info(String.format("  ├─ Driver Version:         %s", metaData.getDriverVersion()));
            getLogger().info(String.format("  ├─ JDBC URL:              %s", maskUrl(metaData.getURL())));
            getLogger().info(String.format("  ├─ Username:              %s", metaData.getUserName()));
            getLogger().info(String.format("  ├─ Auto Commit:           %s", connection.getAutoCommit() ? "[Y] Enabled" : "[N] Disabled"));
            getLogger().info(String.format("  ├─ Read Only:             %s", connection.isReadOnly() ? "[Y] Yes" : "[N] No"));
            getLogger().info(String.format("  └─ Transaction Isolation:  %s", getTransactionIsolationName(connection.getTransactionIsolation())));
            
        } catch (SQLException e) {
            getLogger().warn("Could not retrieve database information: " + e.getMessage());
        }
    }

    private void showDetailedMetrics(HikariDataSource dataSource) {
        getLogger().info("");
        getLogger().info("[C] Configuration Details:");
        getLogger().info(String.format("  ├─ Minimum Pool Size:      %d", dataSource.getMinimumIdle()));
        getLogger().info(String.format("  ├─ Maximum Pool Size:      %d", dataSource.getMaximumPoolSize()));
        getLogger().info(String.format("  ├─ Connection Timeout:     %d ms", dataSource.getConnectionTimeout()));
        getLogger().info(String.format("  ├─ Idle Timeout:          %d ms", dataSource.getIdleTimeout()));
        getLogger().info(String.format("  ├─ Max Lifetime:          %d ms", dataSource.getMaxLifetime()));
        getLogger().info(String.format("  ├─ Leak Detection:        %d ms", dataSource.getLeakDetectionThreshold()));
        getLogger().info(String.format("  └─ Validation Timeout:     %d ms", dataSource.getValidationTimeout()));

        try {
            HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
            
            getLogger().info("");
            getLogger().info("[A] Advanced Pool Metrics:");
            getLogger().info(String.format("  ├─ Total Connections:      %d", poolBean.getTotalConnections()));
            getLogger().info(String.format("  ├─ Active Connections:     %d", poolBean.getActiveConnections()));
            getLogger().info(String.format("  ├─ Idle Connections:       %d", poolBean.getIdleConnections()));
            getLogger().info(String.format("  ├─ Awaiting Connections:   %d", poolBean.getThreadsAwaitingConnection()));
            
            // Show connection creation and close rates
            getLogger().info("  ├─ Pool State:             " + (dataSource.isClosed() ? "[X] Closed" : "[Y] Running"));
            getLogger().info("  └─ Health Check:           " + (dataSource.isRunning() ? "[Y] Healthy" : "[X] Unhealthy"));
            
        } catch (Exception e) {
            getLogger().warn("Could not retrieve detailed metrics: " + e.getMessage());
        }

        // Show JVM memory info related to database
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        getLogger().info("");
        getLogger().info("[M] JVM Memory (relevant to DB connections):");
        getLogger().info(String.format("  ├─ Used Memory:           %s MB", DECIMAL_FORMAT.format(usedMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("  ├─ Free Memory:           %s MB", DECIMAL_FORMAT.format(freeMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("  ├─ Total Memory:          %s MB", DECIMAL_FORMAT.format(totalMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("  └─ Max Memory:            %s MB", DECIMAL_FORMAT.format(maxMemory / 1024.0 / 1024.0)));
    }

    private String maskUrl(String url) {
        if (url == null) return "N/A";
        // Mask sensitive information in the URL
        return url.replaceAll("password=([^&\\s]+)", "password=***");
    }

    private String getTransactionIsolationName(int isolationLevel) {
        return switch (isolationLevel) {
            case Connection.TRANSACTION_NONE -> "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN (" + isolationLevel + ")";
        };
    }
}
