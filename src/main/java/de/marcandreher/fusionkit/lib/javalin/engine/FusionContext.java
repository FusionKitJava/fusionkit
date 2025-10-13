package de.marcandreher.fusionkit.lib.javalin.engine;

import io.javalin.http.Context;

public interface FusionContext extends Context {

    default String realIp() {
        String xForwardedFor = header("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header (alternative header)
        String xRealIP = header("X-Real-IP");
        if (xRealIP != null && !xRealIP.trim().isEmpty()) {
            return xRealIP.trim();
        }
        
        // Fallback to direct IP (for local development)
        return ip();
    }
}