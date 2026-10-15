package de.marcandreher.fusionkit.core.i18n;

import java.net.URI;
import java.net.URISyntaxException;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class I18nSetHandler implements Handler {

    private static final String DEFAULT_RETURN_URL = "/";

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String language = ctx.formParam("language");
        String returnUrl = ctx.formParam("returnUrl");

        if (language != null) {
            ctx.cookie("preferred-language", language, 365 * 24 * 60 * 60);
        }

        ctx.redirect(isSafeRelativePath(returnUrl) ? returnUrl : DEFAULT_RETURN_URL);
    }

    private static boolean isSafeRelativePath(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (url.charAt(0) != '/' || url.startsWith("//")) {
            return false;
        }
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '\\' || c <= 0x20 || c == 0x7F) {
                return false;
            }
        }
        try {
            URI uri = new URI(url);
            return uri.getScheme() == null && uri.getRawAuthority() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}