package de.marcandreher.fusionkit.core.debug;

import java.io.File;

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
        File fusionDebugHtml = new File(FusionDebugCache.class.getClassLoader().getResource("debugger.html").toURI());
        String htmlContent = new String(java.nio.file.Files.readAllBytes(fusionDebugHtml.toPath())).replaceAll("%key%", key);
        ctx.attribute("debugHtml", htmlContent);
    }

}
