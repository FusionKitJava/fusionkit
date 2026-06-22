package de.marcandreher.fusionkit.core.database;

import de.marcandreher.fusionkit.core.database.Database.ServerTimezone;
import lombok.Data;

@Data
public class DbConfig {
    private String host = "localhost";
    private int port = 3306;
    private String username = "root";
    private String database = "fusionkit";
    private String password = "password";
    private ServerTimezone serverTimezone = ServerTimezone.UTC;
}
