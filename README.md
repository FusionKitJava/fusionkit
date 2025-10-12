# FusionKit - A java webframework containing tools

A set of java libaries put into a framework for building fast and modern java web apps

### Features:
- Error Handling (Freemarker/Javalin)
- Gson formatting
- Database connecting
- Templating
- Extensive configuration for production
- Mapping classes to ResultSets
- Predefined file structure
- App configuration
- Logging

### File structure:
```bash
.config/ # Config directory
   - database.toml
   - freemarker.toml
   - fusionkit.toml # If set
logs/ # Logback log dir
public/ # Static files
templates/ # Freemarker template dir
```


### Main Dependencies:
- Hikari/MySQL-Connector-J
- Javalin
- Freemarker
- Gson
- OkHttp
- logback

### Usage
```java
public class App extends FusionKit
{
    public static void main( String[] args )
    {
        FusionKit.setConfig(new AppConfig());

        AppConfig config = (AppConfig) FusionKit.getConfig().getModel();

        FusionKit.setLogLevel(config.getServer().getLogger());

        Database db = new Database();
        db.connectToMySQL( config.getDatabase().getHost(), 
                           config.getDatabase().getUser(), 
                           config.getDatabase().getPassword(), 
                           config.getDatabase().getDatabase(), 
                           ServerTimezone.valueOf(config.getDatabase().getTimezone()));

    
        WebAppConfig webAppConfig = WebAppConfig.builder()
            .name("TestApp")
            .port(config.getServer().getPort())
            .domain(config.getServer().getDomain())
            .staticfiles(true)
            .freemarker(true)
            .routeOverview(true)
            .build();

        FusionKit.registerWebApplication(new TestApplication(webAppConfig));
    }
}
```