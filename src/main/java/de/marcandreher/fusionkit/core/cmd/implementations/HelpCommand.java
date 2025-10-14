package de.marcandreher.fusionkit.core.cmd.implementations;

import de.marcandreher.fusionkit.core.cmd.Command;
import de.marcandreher.fusionkit.core.cmd.CommandInfo;
import de.marcandreher.fusionkit.core.cmd.CommandService;

@CommandInfo(name="help", description="Displays help information about available commands.")

public class HelpCommand implements Command{

    private final CommandService commandService;

    public HelpCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void execute(String[] args) {
        getLogger().info("Displaying help information");
        for (Command cmd : commandService.getCommands().values()) {
            CommandInfo info = cmd.getClass().getAnnotation(CommandInfo.class);
            if (info != null) {
                getLogger().info(">> {}: {}", info.name(), info.description());
            }
        }
    }
    
}
