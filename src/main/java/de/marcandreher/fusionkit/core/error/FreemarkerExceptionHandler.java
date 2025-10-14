package de.marcandreher.fusionkit.core.error;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.marcandreher.fusionkit.core.config.WebAppConfig;
import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class FreemarkerExceptionHandler implements TemplateExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(FreemarkerExceptionHandler.class);
    private final WebAppConfig config;

    public FreemarkerExceptionHandler(WebAppConfig config) {
        this.config = config;
    }

    @Override
    public void handleTemplateException(TemplateException te, Environment env, Writer out) 
            throws TemplateException {
        
        String templateName = env.getMainTemplate().getName();
        
        // Log the error with full details
        logger.error("Freemarker template error in template '{}': {}", templateName, te.getMessage(), te);
        
        // Write error message to output in development mode
        if (config.isTemplatesAutoReload()) {
            try {
                writeErrorToOutput(out, templateName, te.getMessage());
            } catch (IOException writeEx) {
                logger.error("Failed to write error message to output", writeEx);
                // Re-throw original exception if we can't write error message
                throw te;
            }
        } else {
            // In production, just throw the exception to be handled by global handler
            throw te;
        }
    }

    private void writeErrorToOutput(Writer writer, String templateName, String errorMessage) 
            throws IOException {
        
        writer.write("<!-- Template Error: " + errorMessage + " -->\n");
        writer.write("<div style='color: red; border: 1px solid red; padding: 10px; margin: 10px; background: #ffe6e6; font-family: monospace;'>\n");
        writer.write("<h3 style='margin-top: 0; color: #d32f2f;'>🚨 FusionKit Template Error</h3>\n");
        writer.write("<p><strong>Template:</strong> <code>" + escapeHtml(templateName) + "</code></p>\n");
        writer.write("<p><strong>Error:</strong></p>\n");
        writer.write("<pre style='background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;'>");
        writer.write(escapeHtml(errorMessage));
        writer.write("</pre>\n");
        writer.write("<p style='font-size: 0.9em; color: #666; margin-bottom: 0;'>");
        writer.write("💡 <strong>Tip:</strong> This error display is only shown in development mode when template auto-reload is enabled.");
        writer.write("</p>\n");
        writer.write("</div>\n");
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
     * Creates a new FreemarkerTemplateError handler for the given config
     */
    public static FreemarkerExceptionHandler create(WebAppConfig config) {
        return new FreemarkerExceptionHandler(config);
    }
}
