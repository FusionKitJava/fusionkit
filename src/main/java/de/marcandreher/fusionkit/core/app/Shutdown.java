package de.marcandreher.fusionkit.core.app;

import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.WebApp;

public class Shutdown extends FusionKit {

    private static final Logger logger = FusionKit.getLogger(Shutdown.class);

    public Thread getShutdownHook() {
        return new Thread(() -> {
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
                if(FusionKit.commandService != null) {
                    log("Shutting down command service...");
                    FusionKit.commandService.interrupt();
                }

                log("FusionKit shutdown complete. in <" + (System.currentTimeMillis() - startTime) + " ms>");
                
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }, "FK-Shutdown-Hook");
    }

    public void log(String message) {
        logger.info("{}", message);
    }
    
}
