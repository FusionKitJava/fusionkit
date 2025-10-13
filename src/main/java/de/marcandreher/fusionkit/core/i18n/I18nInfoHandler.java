package de.marcandreher.fusionkit.core.i18n;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class I18nInfoHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.json(Map.of(
                "currentLanguage", ctx.attribute("locale").toString(),
                "autoDetected", ctx.attribute("acceptLanguage")));
    }
    
}
