package de.marcandreher.fusionkit.core.app;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.logger.ConsoleColor;

public class Shutdown extends FusionKit {

    
    public Thread getShutdownHook() {
        return new Thread(() -> {
            System.out.println();
            long startTime = System.currentTimeMillis();
            log("Shutting down FusionKit...");
            
            try {
                // Shutdown cron tasks
                if (FusionKit.cron != null) {
                    log("Shutting down cron scheduler...");
                    FusionKit.cron.shutdown();
                }
                
                // Shutdown web applications
                if (FusionKit.webApps != null && !FusionKit.webApps.isEmpty()) {
                    log("Shutting down " + FusionKit.webApps.size() + " web applications...");
                    for (WebApp app : FusionKit.webApps) {
                        app.stop();
                        log("Web application '" + app.getConfig().getName() + "' stopped.");
                    }
                }
                
                // Close database connection if exists
                if (FusionKit.database != null) {
                    log("Closing database connections...");
                    FusionKit.database.shutdown();
                }

                FusionKit.commandService.interrupt();

                log("FusionKit shutdown complete. in <" + (System.currentTimeMillis() - startTime) + " ms>");
                FusionKit.logger.info("FusionKit shutdown complete.");
                
            } catch (Exception e) {
                log("Error during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }, "FK-Shutdown-Hook");
    }

    public void log(String message) {
        System.out.println(ConsoleColor.RED_BOLD + "SHUTDOWN" + ConsoleColor.BLACK_BOLD_BRIGHT + " | " + message + ConsoleColor.RESET);
    }
    
}
