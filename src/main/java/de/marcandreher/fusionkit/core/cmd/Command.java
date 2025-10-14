package de.marcandreher.fusionkit.core.cmd;

import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;

public interface Command {
    void execute(String[] args);

    default Logger getLogger() {
        return FusionKit.getLogger(CommandService.class);
    }
}