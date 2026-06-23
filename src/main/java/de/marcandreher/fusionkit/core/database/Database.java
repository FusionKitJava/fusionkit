package de.marcandreher.fusionkit.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.cmd.implementations.DatabaseCommand;
import de.marcandreher.fusionkit.core.config.DatabaseConfiguration;

public class Database {
    private static final Logger logger = FusionKit.getLogger(Database.class);
    public List<MySQL> runningConnections = new ArrayList<MySQL>();
    public HikariDataSource dataSource;
    public int currentConnections;
    public DatabaseConfiguration config;

    private DbConfig dbConfig = new DbConfig();
    private HikariConfig hikariConfig;

    /**
     * Constructs a new Database object with default settings.
     */
    public Database(Consumer<DbConfig> config) {
        config.accept(dbConfig);
        if(FusionKit.database != null) {
            logger.warn("A Database instance already exists.");
            return;
        }
        this.hikariConfig = new HikariConfig();
        FusionKit.database = this;
        
        FusionKit.registerCommand(DatabaseCommand.class);
    }

    /**
     * Represents the server timezone for the MySQL connection.
     */
    public enum ServerTimezone {
        UTC("UTC"), GMT("GMT");

        private final String code;

        /**
         * Constructs a new ServerTimezone enum with the specified code.
         *
         * @param code The code representing the server timezone.
         */
        ServerTimezone(String code) {
            this.code = code;
        }

        /**
         * Returns the code representing the server timezone.
         *
         * @return The code representing the server timezone.
         */
        @Override
        public String toString() {
            return code;
        }
    }

    /**
     * Connects to a MySQL database using the specified connection parameters.
     *
     * @param host           The host of the MySQL server.
     * @param user           The username for the database connection.
     * @param password       The password for the database connection.
     * @param database       The name of the database to connect to.
     * @param serverTimezone The server timezone for the MySQL connection.
     */
    public void connect() {
        config = DatabaseConfiguration.load();
        config.apply(hikariConfig);
        String url = "jdbc:mysql://" + dbConfig.getHost() + ":3306/" + dbConfig.getDatabase() + "?serverTimezone=" + dbConfig.getServerTimezone() + "&allowPublicKeyRetrieval=true";
        hikariConfig
                .setJdbcUrl(url);
        hikariConfig.setUsername(dbConfig.getUsername());
        hikariConfig.setPassword(dbConfig.getPassword());

        try {
            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            logger.error("Error while setting up the connection pool: ", e);
            System.exit(0);
        }

        try (Connection _ = dataSource.getConnection()) {
            logger.info("Connected to Database (" + url + ")");
        } catch (SQLException e) {
            logger.error("Error while connecting to MySQL database " + e.getMessage());
        }
    }

    public HikariConfig getConfig() {
        return hikariConfig;
    }

    public DatabaseConfiguration getDatabaseConfig() {
        return config;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool shut down.");
        }
    }

    /**
     * Get a connection to the MySQL database.
     *
     * @return A connection to the MySQL database.
     */
    public MySQL getConnection() {
        MySQL connection = null;
        connection = new MySQL(this);
        this.currentConnections++;
        return connection;
    }
}