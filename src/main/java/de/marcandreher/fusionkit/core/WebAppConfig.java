package de.marcandreher.fusionkit.core;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import de.marcandreher.fusionkit.core.auth.AuthProcessor;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.store.AuthSessionStore;
import de.marcandreher.fusionkit.core.auth.store.SessionAttributeAuthSessionStore;
import de.marcandreher.fusionkit.core.database.Database;
import de.marcandreher.fusionkit.core.javalin.JavalinConfigurator;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import io.javalin.config.RoutesConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class WebAppConfig {

    private Consumer<RoutesConfig> router = routes -> {
    };

    public final I18N i18n = new I18N();
    public final StaticFiles staticFiles = new StaticFiles();
    public final Freemarker freemarker = new Freemarker();
    public final Auth auth = new Auth();
    public final Logging logging = new Logging();
    public final Cors cors = new Cors();
    public final SSL ssl = new SSL();
    public final Sessions sessions = new Sessions();
    public final Server server = new Server();

    @Setter 
    @Getter
    private Database database;

    @Setter
    @Getter
    private String name = "UnnamedApp";

    @Setter
    @Getter
    private int port = 8080;

    @Setter
    @Getter
    private String domain = "localhost";

    @Setter
    @Getter
    private ProductionLevel productionLevel = ProductionLevel.DEVELOPMENT;

    @Setter
    @Getter
    private boolean debugger = false;

    @Setter
    @Getter
    private boolean metrics = false;

    @Setter
    @Getter
    private JavalinConfigurator javalinConfigurator = null;

    public void routes(Consumer<RoutesConfig> routes) {
        router = router.andThen(routes);
    }

    public Consumer<RoutesConfig> getRouter() {
        return router;
    }

    @Data
    public static class Auth {
        private boolean enabled = false;
        private AuthProvider authProvider = AuthProvider.NONE;
        private Set<AuthProvider> enabledProviders = new LinkedHashSet<>();
        private long authSessionInterval = 24 * 60 * 60 * 1000;
        private AuthProcessor authProcessor = null;
        private AuthSessionStore authSessionStore = new SessionAttributeAuthSessionStore();
    }

    @Data
    public static class Freemarker {
        private boolean enabled = false;
        private String templatesDirectory = "templates";
        private String templatesEncoding = "UTF-8";
        private boolean templatesAutoReload = true;
    }

    @Data
    public static class StaticFiles {
        private boolean enabled = false;
        private String directory = "public";
        private String path = "/";
        private boolean external = true;
    }

    @Data
    public static class I18N {
        private boolean enabled = false;
        private String directory = "i18n";
        private Locale defaultLanguage = Locale.ENGLISH;
    }

    @Data
    public static class Logging {
        private boolean requestLogging = true;
        private String logFormat = "[{method}] | <{host}{path}> | <{status}> | <{ms}ms> | <{agent}>";
    }

    @Data
    public static class Cors {
        private boolean enabled = false;
        private String origins = "*";
        private String methods = "GET,POST,PUT,DELETE,OPTIONS";
        private String headers = "Content-Type,Authorization";
    }

    @Data
    public static class SSL {
        private boolean enabled = false;
        private String keystorePath;
        private String keystorePassword;
    }

    @Data
    public static class Sessions {
        private boolean enabled = false;
        private int timeoutMinutes = 30;
        private String cookieName;
    }

    @Data
    public static class Server {
        private boolean showBanner = true;
        private int maxRequestSize = 1024 * 1024;
    }
}