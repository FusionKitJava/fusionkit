package de.marcandreher.fusionkit.core.logger;

import org.jline.reader.LineReader;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class JLineConsoleAppender extends AppenderBase<ILoggingEvent> {

    private static volatile LineReader reader;
    private PatternLayoutEncoder encoder;

    /** Called from main to give the appender access to the active LineReader. */
    public static void setReader(LineReader r) {
        reader = r;
    }

    /** Injected by Logback based on the <encoder> element in logback.xml. */
    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for JLineConsoleAppender");
            return;
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (reader.getTerminal() != null) {
            // Format the event using the configured encoder pattern
            String msg = new String(encoder.encode(event));
            // Remove only a single trailing newline (\r\n or \n) if present
            if (msg.endsWith("\r\n")) {
                msg = msg.substring(0, msg.length() - 2);
            } else if (msg.endsWith("\n")) {
                msg = msg.substring(0, msg.length() - 1);
            }
            reader.printAbove(msg);
        }else {
            // Terminal is closed, fallback to standard output
            System.err.println("Test");
            System.err.println(event.getFormattedMessage());
        }
    }
}