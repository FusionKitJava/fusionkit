package de.marcandreher.fusionkit.core.javalin.engine;

import java.util.List;

import de.marcandreher.fusionkit.core.auth.User;
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

    default Context render(String templatePath) {
        return render(templatePath, attributeMap());
    }

    default User getUser() {
        return sessionAttribute("user");
    }

    default boolean isLoggedIn() {
        return getUser() != null;
    }

    default String param(String paramName) {
        // check query params
        List<String> q = queryParams(paramName);
        if (q != null && !q.isEmpty()) {
            return q.get(0);
        }

        // check form params directly from map (avoid broken proxy call)
        List<String> f = formParamMap().get(paramName);
        if (f != null && !f.isEmpty()) {
            return f.get(0);
        }

        return null;
    }

    default Context context() {
        return this;
    }
}