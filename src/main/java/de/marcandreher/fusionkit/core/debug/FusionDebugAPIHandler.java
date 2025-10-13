package de.marcandreher.fusionkit.core.debug;

import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class FusionDebugAPIHandler implements Handler {
    public static Cache<String, String> cache = Caffeine.newBuilder()
    .expireAfterWrite(1, java.util.concurrent.TimeUnit.MINUTES)
    .maximumSize(1000)
    .build();

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String key = ctx.queryParam("key");
        if (key != null) {
            String data = cache.getIfPresent(key);
            if (data != null) {
                ctx.contentType("application/json");
                ctx.result(data);
            }
        }
    }


}
