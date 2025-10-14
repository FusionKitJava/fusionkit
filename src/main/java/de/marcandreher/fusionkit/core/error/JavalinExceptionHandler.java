package de.marcandreher.fusionkit.core.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.marcandreher.fusionkit.core.config.WebAppConfig;
import io.javalin.http.Context;

public class JavalinExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(JavalinExceptionHandler.class);
    private final WebAppConfig config;

    public JavalinExceptionHandler(WebAppConfig config) {
        this.config = config;
    }

    /**
     * Handles all types of exceptions with a unified approach
     */
    public void handleException(Exception e, Context ctx) {
        logger.info("GlobalExceptionHandler triggered for path '{}' with exception: {}", 
                   ctx.path(), e.getClass().getSimpleName());
        logger.error("Exception details for path '{}': {}", ctx.path(), e.getMessage(), e);
        
        // Check if response is already committed
        if (isResponseCommitted(ctx)) {
            logger.warn("Cannot send error response - response already committed for path '{}'", ctx.path());
            return;
        }
        
        if (config.isTemplatesAutoReload()) {
            String errorHtml = buildErrorHtml(ctx.path(), e);
            ctx.status(500).html(errorHtml);
        } else {
            ctx.status(500).result("Internal Server Error");
        }
    }

    private boolean isResponseCommitted(Context ctx) {
        try {
            // Try to access the response - if it throws an exception, it might be committed
            ctx.res().getStatus();
            return false;
        } catch (Exception e) {
            logger.debug("Response appears to be committed: {}", e.getMessage());
            return true;
        }
    }

    private String buildErrorHtml(String path, Exception exception) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='")
            .append("color: #d32f2f; ")
            .append("border: 2px solid #d32f2f; ")
            .append("padding: 20px; ")
            .append("margin: 20px; ")
            .append("background: #ffe6e6; ")
            .append("border-radius: 6px; ")
            .append("font-family: Arial, sans-serif;")
            .append("'>")
            .append("<h2 style='margin-top: 0; color: #d32f2f;'>")
            .append("ERROR - FusionKit Application")
            .append("</h2>")
            .append("<p><strong>Request Path:</strong> ")
            .append("<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px;'>")
            .append(escapeHtml(path))
            .append("</code></p>")
            .append("<p><strong>Exception Type:</strong> ")
            .append("<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px;'>")
            .append(escapeHtml(exception.getClass().getSimpleName()))
            .append("</code></p>")
            .append("<p><strong>Error Message:</strong></p>")
            .append("<div style='")
            .append("background: #f5f5f5; ")
            .append("padding: 12px; ")
            .append("border-radius: 4px; ")
            .append("border-left: 4px solid #d32f2f; ")
            .append("margin: 10px 0; ")
            .append("font-family: monospace;")
            .append("'>")
            .append(escapeHtml(exception.getMessage() != null ? exception.getMessage() : "No message available"))
            .append("</div>");
        
        // Add collapsible stack trace
        html.append("<details style='margin-top: 15px;'>")
            .append("<summary style='cursor: pointer; font-weight: bold; color: #d32f2f;'>")
            .append("Stack Trace (Click to show/hide)")
            .append("</summary>")
            .append("<pre style='")
            .append("background: #f8f8f8; ")
            .append("padding: 10px; ")
            .append("border-radius: 4px; ")
            .append("overflow-x: auto; ")
            .append("font-size: 0.85em; ")
            .append("margin-top: 10px; ")
            .append("border: 1px solid #ddd; ")
            .append("font-family: monospace;")
            .append("'>");
        
        // Build stack trace
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(exception.getClass().getName());
        if (exception.getMessage() != null) {
            stackTrace.append(": ").append(exception.getMessage());
        }
        stackTrace.append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append("    at ").append(element.toString()).append("\n");
        }
        
        html.append(escapeHtml(stackTrace.toString()));
        html.append("</pre></details>");
        
        html.append("<p style='font-size: 0.9em; color: #666; margin: 15px 0 0 0; padding-top: 12px; border-top: 1px solid #ddd;'>")
            .append("<strong>Note:</strong> This detailed error is only shown in development mode. ")
            .append("In production, users will see a generic error message.")
            .append("</p>")
            .append("</div>");
        
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    /**
     * Creates a new GlobalExceptionHandler for the given config
     */
    public static JavalinExceptionHandler create(WebAppConfig config) {
        return new JavalinExceptionHandler(config);
    }
}