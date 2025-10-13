package de.marcandreher.fusionkit.core.debug;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class FusionDebugHandler implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        Map<String, Object> safeAttributes = new HashMap<>();
        
        // Filter attributes to only include safe serializable types
        for (Map.Entry<String, Object> entry : ctx.attributeMap().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip debugHtml from serialization
            if ("debugHtml".equals(key)) {
                continue;
            }
            
            if (value != null) {
                Class<?> valueClass = value.getClass();
                // Only include if the class is public and safe to serialize
                if (Modifier.isPublic(valueClass.getModifiers()) && isSafeForSerialization(valueClass)) {
                    safeAttributes.put(entry.getKey(), value);
                } else {
                    // Include type information for non-serializable objects
                    safeAttributes.put(entry.getKey(), "[" + valueClass.getSimpleName() + ": non-serializable]");
                }
            } else {
                // Include null values
                safeAttributes.put(entry.getKey(), null);
            }
        }
        
        // Create Gson with custom serializers for common problematic types
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) 
                (src, typeOfSrc, context) -> context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .setPrettyPrinting()
            .create();
        try {
            FusionDebugAPIHandler.cache.put(ctx.attribute("debugKey"), gson.toJson(safeAttributes));
        } catch (Exception e) {
            // Fallback to simple string representation if JSON serialization fails
            ctx.attribute("debug", "Debug serialization failed: " + e.getMessage());
        }
    }
    
    private boolean isSafeForSerialization(Class<?> clazz) {
        String className = clazz.getName();
        
        // Specifically exclude known problematic types only
        if (className.contains("ResourceBundle")) {
            return false;
        }
        
        // Exclude internal JVM/reflection classes
        if (className.startsWith("sun.") || 
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.internal.")) {
            return false;
        }
        
        // Exclude problematic reflection/proxy classes
        if (className.contains("$Proxy") || 
            className.contains("$$Lambda") ||
            className.contains("CGLIB$$")) {
            return false;
        }
        
        // Allow everything else - let Gson handle it or fail gracefully
        return true;
    }
    
}
