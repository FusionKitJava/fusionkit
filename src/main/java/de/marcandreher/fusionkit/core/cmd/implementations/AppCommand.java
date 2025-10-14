package de.marcandreher.fusionkit.core.cmd.implementations;

import java.util.List;

import de.marcandreher.fusionkit.core.WebApp;
import de.marcandreher.fusionkit.core.cmd.Command;
import de.marcandreher.fusionkit.core.cmd.CommandInfo;

@CommandInfo(name = "app", description = "Manages and lists registered web applications")
public class AppCommand implements Command {

    private final List<WebApp> webApps;

    public AppCommand(List<WebApp> webApps) {
        this.webApps = webApps;
    }

    @Override
    public void execute(String[] args) {
        if(args.length == 0 || args[0].equalsIgnoreCase("list")) {
            getLogger().info("[W] Registered Web Applications:");
            for(WebApp app : webApps) {
                getLogger().info(String.format("  ├─ %s (%s)", app.getSafeName(), app.getConfig().getDomain()));
            }
        } else if(args[0].equalsIgnoreCase("info") && args.length == 2) {
            String appName = args[1];
            for (WebApp app : webApps) {
                if (app.getSafeName().equalsIgnoreCase(appName) || app.getConfig().getName().equalsIgnoreCase(appName)) {
                    getLogger().info("[W] Web Application Information:");
                    getLogger().info(String.format("  ├─ Config:        %s", app.getConfig().toString()));

                    return;
                }
                
            }
        } else if(args[0].equalsIgnoreCase("help")) {
            getLogger().info("App Command Usage:");
            getLogger().info("  app list          # List all registered web applications");
            getLogger().info("  app info <name>   # Show information about a specific web application");
            getLogger().info("  app help         # Show this help message");
        } else {
            getLogger().warn("Unknown subcommand '{}'. Use 'app help' for usage information.", args[0]);
        }
    }

}
