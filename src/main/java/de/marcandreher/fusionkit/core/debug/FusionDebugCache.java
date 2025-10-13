package de.marcandreher.fusionkit.core.debug;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class FusionDebugCache implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Generate random string
        String key = java.util.UUID.randomUUID().toString();

        ctx.attribute("debugKey", key);

        // Load debugger.html file from resources
        try (java.io.InputStream inputStream = FusionDebugCache.class.getClassLoader().getResourceAsStream("debugger.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("debugger.html not found in resources");
            }
            String htmlContent = new String(inputStream.readAllBytes()).replaceAll("%key%", key);
            ctx.attribute("debugHtml", htmlContent);
        }
    }

}
