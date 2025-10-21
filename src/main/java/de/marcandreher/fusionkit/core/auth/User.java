package de.marcandreher.fusionkit.core.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

@Data
public class User {
    private String id;
    private String username;
    private String avatar;
    private String email;

    private Map<String, Object> data = new ConcurrentHashMap<>();
}
