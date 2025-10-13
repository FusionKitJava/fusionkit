package de.marcandreher.fusionkit.core.i18n;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class I18nSetHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String language = ctx.formParam("language");
        String returnUrl = ctx.formParam("returnUrl");

        // Set language cookie
        if (language != null) {
            ctx.cookie("preferred-language", language, 365 * 24 * 60 * 60); // 1 year
        }

        // Redirect back to the original page
        ctx.redirect(returnUrl != null ? returnUrl : "/");
    }

}
