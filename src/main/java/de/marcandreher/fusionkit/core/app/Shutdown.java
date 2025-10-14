package de.marcandreher.fusionkit.core.app;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;

public class Shutdown extends FusionKit {

    
    public Thread getShutdownHook() {
        return new Thread(() -> {
            try {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                Logger root = context.getLogger("ROOT");
                
                root.detachAndStopAllAppenders();
                
                // Check if CONSOLE appender exists before adding it
                Appender<ILoggingEvent> consoleAppender = (Appender<ILoggingEvent>) root.getAppender("CONSOLE");
                if (consoleAppender != null) {
                    root.addAppender(consoleAppender);
                    root.info("Switched to ConsoleAppender on shutdown.");
                } else {
                    System.out.println("[FK-Shutdown] Console appender not found, using System.out");
                }
            } catch (Exception e) {
                System.out.println("[FK-Shutdown] Logger configuration failed: " + e.getMessage());
            }

            // Use System.out as fallback in case logger doesn't work during shutdown
            System.out.println("[FK-Shutdown] Shutdown hook triggered, shutting down FusionKit...");
            FusionKit.logger.info("Shutdown hook triggered, shutting down FusionKit...");
            
            try {
                // Shutdown cron tasks
                if (FusionKit.cron != null) {
                    System.out.println("[FK-Shutdown] Shutting down cron scheduler...");
                    FusionKit.cron.shutdown();
                }
                
                // Shutdown web applications
                if (FusionKit.webApps != null && !FusionKit.webApps.isEmpty()) {
                    System.out.println("[FK-Shutdown] Shutting down " + FusionKit.webApps.size() + " web applications...");
                    for (WebApp app : FusionKit.webApps) {
                        app.stop();
                    }
                }
                
                // Close database connection if exists
                if (FusionKit.database != null) {
                    System.out.println("[FK-Shutdown] Closing database connections...");
                    FusionKit.database.shutdown();
                }

                FusionKit.commandService.interrupt();
                
                System.out.println("[FK-Shutdown] FusionKit shutdown complete.");
                FusionKit.logger.info("FusionKit shutdown complete.");
                
            } catch (Exception e) {
                System.err.println("[FK-Shutdown] Error during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }, "FK-Shutdown-Hook");
    }
    
}
