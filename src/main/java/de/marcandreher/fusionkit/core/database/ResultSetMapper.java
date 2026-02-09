package de.marcandreher.fusionkit.core.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ResultSetMapper {
    /**
     * Maps a ResultSet row to an instance of the specified class.
     * The class fields must
     * be annotated with @Column to specify the corresponding DB column.
     */
    public static <T> T map(ResultSet rs, Class<T> clazz) throws SQLException {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            
            // Get available columns from ResultSet
            Set<String> availableColumns = getAvailableColumns(rs);

            for (Field field : clazz.getDeclaredFields()) {
                Column col = field.getAnnotation(Column.class);
                if (col == null) {
                    continue; // Skip fields without @Column annotation
                }
                
                String columnName = col.value();
                
                // Check if column exists in ResultSet
                if (!availableColumns.contains(columnName.toLowerCase())) {
                    // Column doesn't exist - only throw error for primitive types that can't be null
                    if (field.getType().isPrimitive()) {
                        throw new IllegalStateException(
                            String.format("Required column '%s' for field '%s' (primitive type %s) not found in ResultSet. Available columns: %s",
                                columnName, field.getName(), field.getType().getSimpleName(), availableColumns)
                        );
                    }
                    // For nullable types (Integer, Boolean, etc.), leave as null and continue
                    continue;
                }
                
                try {
                    Object value = rs.getObject(columnName);
                    field.setAccessible(true);
                    
                    // Handle null values
                    if (value == null) {
                        if (field.getType().isPrimitive()) {
                            throw new IllegalStateException(
                                String.format("Column '%s' is NULL but field '%s' is primitive type %s (cannot be null)",
                                    columnName, field.getName(), field.getType().getSimpleName())
                            );
                        }
                        // For nullable types, set null explicitly
                        field.set(instance, null);
                    } else {
                        // Convert boolean values from numeric representation
                        if ((field.getType() == boolean.class || field.getType() == Boolean.class) && value instanceof Number) {
                            field.set(instance, ((Number) value).intValue() != 0);
                        } else {
                            field.set(instance, value);
                        }
                    }
                } catch (SQLException e) {
                    throw new IllegalStateException(
                        String.format("Failed to retrieve column '%s' for field '%s': %s",
                            columnName, field.getName(), e.getMessage()), e
                    );
                }
            }
            return instance;
        } catch (SQLException e) {
            throw e; // Re-throw SQLException as-is
        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to map ResultSet to class %s: %s",
                    clazz.getSimpleName(), e.getMessage()), e
            );
        }
    }
    
    /**
     * Retrieves the set of available column names from the ResultSet (in lowercase).
     */
    private static Set<String> getAvailableColumns(ResultSet rs) throws SQLException {
        Set<String> columns = new HashSet<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnName(i).toLowerCase());
        }
        
        return columns;
    }
}
