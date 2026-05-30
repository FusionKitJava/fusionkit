package de.marcandreher.fusionkit.core.auth.store;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import de.marcandreher.fusionkit.core.auth.User;
import io.javalin.http.Context;

public class FileAuthSessionStore implements AuthSessionStore {

    private static final Logger logger = LoggerFactory.getLogger(FileAuthSessionStore.class);

    private final File sessionDirectory;
    private final TomlWriter writer = new TomlWriter();

    public FileAuthSessionStore(File sessionDirectory) {
        this.sessionDirectory = sessionDirectory;
    }

    @Override
    public User getUser(Context ctx) {
        File sessionFile = getSessionFile(ctx);
        if (!sessionFile.exists()) {
            return null;
        }

        try {
            Toml toml = new Toml().read(sessionFile);
            return toml.to(User.class);
        } catch (Exception e) {
            logger.error("Failed to read auth session file: {}", sessionFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Override
    public void setUser(Context ctx, User user) {
        if (user == null) {
            clear(ctx);
            return;
        }

        File sessionFile = getSessionFile(ctx);
        try {
            ensureDirectory();
            writer.write(user, sessionFile);
        } catch (IOException e) {
            logger.error("Failed to write auth session file: {}", sessionFile.getAbsolutePath(), e);
        }
    }

    @Override
    public void clear(Context ctx) {
        File sessionFile = getSessionFile(ctx);
        if (sessionFile.exists() && !sessionFile.delete()) {
            logger.warn("Failed to delete auth session file: {}", sessionFile.getAbsolutePath());
        }
    }

    private File getSessionFile(Context ctx) {
        ensureDirectory();
        String sessionId = ctx.req().getSession(true).getId();
        return new File(sessionDirectory, sessionId + ".toml");
    }

    private void ensureDirectory() {
        if (!sessionDirectory.exists() && !sessionDirectory.mkdirs()) {
            logger.warn("Failed to create auth session directory: {}", sessionDirectory.getAbsolutePath());
        }
    }
}
