package de.marcandreher.fusionkit.core.cmd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.cmd.implementations.HelpCommand;
import de.marcandreher.fusionkit.core.cmd.implementations.JVMCommand;
import de.marcandreher.fusionkit.core.logger.JLineConsoleAppender;

public class CommandService extends Thread {

    static final Logger logger = FusionKit.getLogger(CommandService.class);

    static final Terminal terminal;
    static LineReader reader;

    private Map<String, Command> commands = new HashMap<>();

    static {
        try {
            terminal = TerminalBuilder.builder().system(true).build();
            reader = LineReaderBuilder.builder().terminal(terminal).build();
            JLineConsoleAppender.setReader(reader);
        } catch (IOException e) {
            logger.error("Failed to start command service", e);
            throw new RuntimeException(e);
        }
    }

    public void registerCommand(Command command) {
        CommandInfo info = command.getClass().getAnnotation(CommandInfo.class);
        if (info != null) {
            commands.put(info.name(), command);
        }
    }

    @Override
    public void interrupt() {
        try {
            terminal.close();
            reader = null;
        } catch (IOException e) {
            logger.error("Failed to close terminal", e);
        }
        super.interrupt();
        
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    @Override
    public void run() {
        setName("FK-CommandService");

        registerCommand(new HelpCommand(this));
        registerCommand(new JVMCommand());

        while (true) {
            try {
                String cmd = reader.readLine("> ");

                String[] parts = cmd.split(" ");

                if (parts.length == 0) {
                    continue;
                }

                Command command = commands.get(parts[0]);

                if (command != null) {
                    String[] args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                    command.execute(args);
                } else {
                    if(parts[0].length() == 0) continue;

                    logger.info("Unknown command: {}", parts[0]);
                }

                
            } catch (UserInterruptException | EndOfFileException e) {
                System.exit(0);
            }
            
        }
    }
}