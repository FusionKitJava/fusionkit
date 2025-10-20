package de.marcandreher.fusionkit.core.auth;

import lombok.Data;

@Data
public class User {
    public String id;
    public String username;
    public String avatar;
    public String email;
}
