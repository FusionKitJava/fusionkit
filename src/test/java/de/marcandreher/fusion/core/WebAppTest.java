package de.marcandreher.fusion.core;


import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.marcandreher.fusionkit.core.FusionKit;
import de.marcandreher.fusionkit.core.auth.AuthProvider;
import de.marcandreher.fusionkit.core.auth.store.SessionAttributeAuthSessionStore;
import de.marcandreher.fusionkit.core.database.Database;
import okhttp3.OkHttpClient;

public class WebAppTest extends FusionKit {
    
    @Before
    public void setUp() {
        database = new Database(config -> {
            config.setHost("local");
        });
        database.connect();

        FusionKit.setApplication(WebAppTest.class);
        FusionKit.registerWebApplication(config -> {
            config.setName("TestApp");
            config.setDomain("http://localhost");
            config.setPort(8080);
            config.setDebugger(true);

            config.setDatabase(database);
            
            config.auth.setEnabled(true);
            config.auth.setEnabledProviders(Set.of(AuthProvider.DISCORD, AuthProvider.GITHUB));
            config.auth.setAuthSessionStore(new SessionAttributeAuthSessionStore()); // Default is in memory, but files exists too and redis needs own impl but easy with interface
            config.auth.setAuthSessionInterval(60 * 60 * 24 * 3); // 3 days

            config.staticFiles.setEnabled(true);
            config.staticFiles.setExternal(true);
            config.staticFiles.setPath("/public");
            
            config.freemarker.setEnabled(true);
            config.freemarker.setTemplatesDirectory("templates/");
            config.freemarker.setTemplatesAutoReload(true); // Disable caching for development
            
            config.cors.setEnabled(true);
            config.cors.setMethods("*");
            config.cors.setOrigins("*");
            config.cors.setHeaders("*");

            config.logging.setRequestLogging(true);

            config.routes(router -> {
                router.get("/", ctx -> ctx.result("Hello World"));
            });
        });

        
    }

    @Test
    public void testClient() throws InterruptedException {
        Thread.sleep(500);
        OkHttpClient dd =  FusionKit.getHttpClient().newBuilder().build();
        // Test if works
        try (var response = dd.newCall(new okhttp3.Request.Builder().url("http://localhost:8080/").build()).execute()) {
            String body = response.body().string();
            assert body.equals("Hello World");
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Request failed";
        }
    }

}
