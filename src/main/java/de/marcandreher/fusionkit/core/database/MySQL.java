package de.marcandreher.fusionkit.core.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;

@Data
public final class MySQL implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MySQL.class);

    public long connectionCreated = System.currentTimeMillis();
    public StackTraceElement[] caller = Thread.currentThread().getStackTrace();
    private Connection connection;
    private Database database;

    private final int COLUMN_WIDTH = 20;

    public MySQL(Database database) {
        this.database = database;   
        try {
            open(database.getDataSource().getConnection());
        } catch (SQLException e) {
            log.error("Failed to obtain a connection from the pool.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    public synchronized void open(Connection currentCon) {
        if (currentCon == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        this.connection = currentCon;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                database.currentConnections--;
                database.runningConnections.remove(this);
                connection.close();
            }
        } catch (Exception ex) {
            log.warn("Failed to close connection: Active count was {}", database.currentConnections);
        } finally {
            connection = null;
        }
    }

    /**
     * Returns {@code true} if the underlying connection is open and usable.
     */
    public synchronized boolean isOpen() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Schema helpers
    // -------------------------------------------------------------------------

    public boolean tableExists(String tableName) {
        requireOpen();
        try (ResultSet rs = connection.getMetaData().getTables(
                connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        } catch (SQLException e) {
            log.error("Error checking if table exists: {}", tableName, e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Debug / print helpers
    // -------------------------------------------------------------------------

    public void printQuery(String sql, Object... args) {
        PreparedStatement stmt = query(sql, args);
        if (stmt != null) {
            printResultSet(stmt);
        }
    }

    private void printResultSet(PreparedStatement stmt) {
        try (ResultSet resultSet = stmt.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-" + COLUMN_WIDTH + "s", metaData.getColumnName(i));
            }
            System.out.println();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-" + COLUMN_WIDTH + "s", resultSet.getString(i));
                }
                System.out.println();
            }
        } catch (SQLException e) {
            log.error("printResultSet error: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Query (returns PreparedStatement, caller reads the ResultSet)
    // -------------------------------------------------------------------------

    /**
     * Builds and returns a {@link PreparedStatement} for a SELECT query.
     * The caller is responsible for closing the statement (and its ResultSet).
     *
     * <p>Prefer {@link #queryResult(String, Object...)} which returns a
     * ready-to-read {@link ResultSet} and handles resource management for you.
     */
    public PreparedStatement query(String sql, Object... args) {
        requireOpen();
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            bindParameters(stmt, args);
            logSQL(stmt.toString());
            return stmt;
        } catch (Exception ex) {
            log.error("MySQL query error: {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Convenience overload that accepts a {@link List} of String parameters.
     */
    public PreparedStatement query(String sql, List<String> args) {
        return query(sql, args.toArray());
    }

    /**
     * Executes a SELECT query and returns the {@link ResultSet} wrapped in an
     * {@link Optional}. The returned ResultSet <strong>must</strong> be closed
     * by the caller (closing it also closes the underlying statement).
     */
    public Optional<ResultSet> queryResult(String sql, Object... args) {
        requireOpen();
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            bindParameters(stmt, args);
            logSQL(stmt.toString());
            return Optional.of(stmt.executeQuery());
        } catch (Exception ex) {
            log.error("MySQL queryResult error: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Execute (DML / DDL – no return value needed)
    // -------------------------------------------------------------------------

    public void exec(String sql, Object... args) {
        requireOpen();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, args);
            logSQL(stmt.toString());
            stmt.execute();
        } catch (Exception ex) {
            log.error("MySQL exec error: {} | called from {}", ex.getMessage(), getCaller(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Execute with generated-key return
    // -------------------------------------------------------------------------

    /**
     * Executes an INSERT and returns the generated primary key, or
     * {@code -1} on failure.
     */
    public int execKeys(String sql, Object... args) {
        requireOpen();
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(stmt, args);
            logSQL(stmt.toString());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        } catch (Exception ex) {
            log.error("MySQL execKeys error: {}", ex.getMessage(), ex);
            return -1;
        }
    }

    /**
     * Like {@link #execKeys(String, Object...)} but returns a {@code long},
     * safe for tables whose AUTO_INCREMENT can exceed {@link Integer#MAX_VALUE}.
     */
    public long execKeysLong(String sql, Object... args) {
        requireOpen();
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(stmt, args);
            logSQL(stmt.toString());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return -1L;
        } catch (Exception ex) {
            log.error("MySQL execKeysLong error: {}", ex.getMessage(), ex);
            return -1L;
        }
    }

    // -------------------------------------------------------------------------
    // Transaction helpers
    // -------------------------------------------------------------------------

    /**
     * Begins a manual transaction (disables auto-commit).
     */
    public void beginTransaction() {
        requireOpen();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("Failed to begin transaction", e);
        }
    }

    /**
     * Commits the current transaction and re-enables auto-commit.
     */
    public void commit() {
        requireOpen();
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to commit transaction", e);
        }
    }

    /**
     * Rolls back the current transaction and re-enables auto-commit.
     */
    public void rollback() {
        if (!isOpen()) return;
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to rollback transaction", e);
        }
    }

    // -------------------------------------------------------------------------
    // Parameter binding – central, exhaustive, type-safe
    // -------------------------------------------------------------------------

    /**
     * Binds {@code args} to {@code stmt} in order, supporting all common Java
     * types. {@code null} values are bound as SQL NULL ({@link Types#NULL}).
     *
     * <p>Supported types:
     * <ul>
     *   <li>{@code null}               → SQL NULL</li>
     *   <li>{@link String}             → VARCHAR</li>
     *   <li>{@link Integer} / {@code int} → INT</li>
     *   <li>{@link Long} / {@code long}   → BIGINT</li>
     *   <li>{@link Short} / {@code short} → SMALLINT</li>
     *   <li>{@link Byte} / {@code byte}   → TINYINT</li>
     *   <li>{@link Double} / {@code double} → DOUBLE</li>
     *   <li>{@link Float} / {@code float}  → FLOAT</li>
     *   <li>{@link BigDecimal}         → DECIMAL</li>
     *   <li>{@link Boolean} / {@code boolean} → BIT / TINYINT(1)</li>
     *   <li>{@link Timestamp}          → TIMESTAMP</li>
     *   <li>{@link Date}               → DATE</li>
     *   <li>{@link LocalDateTime}      → TIMESTAMP (via {@link Timestamp})</li>
     *   <li>{@link LocalDate}          → DATE (via {@link Date})</li>
     *   <li>{@link Instant}            → TIMESTAMP (via {@link Timestamp})</li>
     *   <li>{@link UUID}               → VARCHAR(36)</li>
     *   <li>{@code byte[]}             → BLOB / BINARY</li>
     *   <li>Any other {@link Enum}     → VARCHAR (name())</li>
     * </ul>
     */
    private void bindParameters(PreparedStatement stmt, Object[] args) throws SQLException {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            bindParameter(stmt, i + 1, args[i]);
        }
    }

    private void bindParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Short) {
            stmt.setShort(index, (Short) value);
        } else if (value instanceof Byte) {
            stmt.setByte(index, (Byte) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            stmt.setFloat(index, (Float) value);
        } else if (value instanceof BigDecimal) {
            stmt.setBigDecimal(index, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else if (value instanceof Timestamp) {
            stmt.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Date) {
            stmt.setDate(index, (Date) value);
        } else if (value instanceof LocalDateTime) {
            stmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof LocalDate) {
            stmt.setDate(index, Date.valueOf((LocalDate) value));
        } else if (value instanceof Instant) {
            stmt.setTimestamp(index, Timestamp.from((Instant) value));
        } else if (value instanceof UUID) {
            stmt.setString(index, value.toString());
        } else if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[]) value);
        } else if (value instanceof Enum) {
            stmt.setString(index, ((Enum<?>) value).name());
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported parameter type at index %d: %s", index, value.getClass().getName()));
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Throws {@link IllegalStateException} if the connection is not open.
     * Keeps every public method from repeating the same null/closed check.
     */
    private void requireOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("MySQL connection is closed or invalid – cannot execute query.");
        }
    }

    private void logSQL(String message) {
        if (database.config.isLogSql()) {
            log.debug(message.replaceAll("[\r\n]+$", ""));
        }
    }

    private String getCaller() {
        if (caller != null && caller.length > 2) {
            return caller[2].toString();
        }
        return "unknown";
    }
}