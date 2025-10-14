package de.marcandreher.fusionkit.core.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetMapper {
    /**
     * Maps a ResultSet row to an instance of the specified class.
     * The class fields must
     * be annotated with @Column to specify the corresponding DB column.
     */
    public static <T> T map(ResultSet rs, Class<T> clazz) throws SQLException {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                Column col = field.getAnnotation(Column.class);
                if (col == null) continue;

                Object value = rs.getObject(col.value());
                if (value != null) {
                    field.setAccessible(true);
                    if ((field.getType() == boolean.class || field.getType() == Boolean.class) && value instanceof Number) {
                        field.set(instance, ((Number) value).intValue() != 0);
                    } else {
                        field.set(instance, value);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Mapping failed", e);
        }
    }
}
