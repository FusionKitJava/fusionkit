# 🚀 FusionKit

**A modern, lightweight Java web framework for rapid application development**

[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

FusionKit is a comprehensive Java web framework that combines the best tools and libraries into a cohesive, easy-to-use package. Built on top of battle-tested technologies like Javalin, Freemarker, and HikariCP, it provides everything you need to build modern web applications quickly and efficiently.

![FusionKit Overview](https://i.imgur.com/iicyQ9R.png)

![FusionKit Errors](https://i.imgur.com/QZoy9Xz.png)

## ✨ Features

### 🔧 **Core Framework**
- **Lightweight & Fast** - Built on Javalin for optimal performance
- **Convention over Configuration** - Sensible defaults with full customization
- **Type-Safe Configuration** - TOML-based configuration with strong typing
- **Production Ready** - Comprehensive logging and error handling

### 🗃️ **Database Integration**
- **Connection Pooling** - HikariCP for high-performance database connections
- **ORM Utilities** - Simple mapping from ResultSets to Java objects
- **MySQL Support** - Optimized for MySQL with timezone handling

### 🎨 **Templating & Views**
- **Freemarker Integration** - Powerful template engine with full feature support
- **Static File Serving** - Efficient static asset handling
- **Internationalization** - Built-in localization support
- **Error Pages** - Beautiful, customizable error handling

### 🛠️ **Developer Experience**
- **Interactive Debugger** - Built-in web-based debug console
- **Route Overview** - Visual route mapping and debugging
- **Structured Logging** - Logback integration with configurable levels

## 📁 Project Structure

FusionKit follows a clean, organized directory structure:

```
your-app/
├── .config/              # Configuration files
│   ├── database.toml     # Database configuration
│   ├── freemarker.toml   # Template engine settings
│   └── fusionkit.toml    # Framework configuration
├── src/main/java/        # Java source code
├── templates/            # Freemarker templates
├── public/               # Static assets (CSS, JS, images)
├── data/                 # Application data files
└── logs/                 # Application logs
```

| Technology | Purpose | Version |
|------------|---------|---------|
| [Javalin](https://javalin.io/) | Web framework & routing | Latest |
| [Freemarker](https://freemarker.apache.org/) | Template engine | Latest |
| [HikariCP](https://github.com/brettwooldridge/HikariCP) | Connection pooling | Latest |
| [MySQL Connector](https://dev.mysql.com/downloads/connector/j/) | Database driver | Latest |
| [Gson](https://github.com/google/gson) | JSON serialization | Latest |
| [OkHttp](https://square.github.io/okhttp/) | HTTP client | Latest |
| [Logback](https://logback.qos.ch/) | Logging framework | Latest |

### 1. Add FusionKit to your project

```xml
<dependency>
    <groupId>com.github.marcandreher</groupId>
    <artifactId>fusionkit</artifactId>
    <version>1.1.3</version>
</dependency>
```

### 2. Create your main application class

```java
public class App extends FusionKit {
    public static void main(String[] args) {
        // Initialize configuration
        FusionKit.setConfig(new AppConfig());
        AppConfig config = (AppConfig) FusionKit.getConfig().getModel();
        
        // Set up logging
        FusionKit.setLogLevel(config.getServer().getLogger());
        
        // Configure database
        Database db = new Database();
        db.connectToMySQL(
            config.getDatabase().getHost(), 
            config.getDatabase().getUser(), 
            config.getDatabase().getPassword(), 
            config.getDatabase().getDatabase(), 
            ServerTimezone.valueOf(config.getDatabase().getTimezone())
        );
    
        // Configure web application
        WebAppConfig webAppConfig = WebAppConfig.builder()
            .name("MyApp")
            .port(config.getServer().getPort())
            .domain(config.getServer().getDomain())
            .staticfiles(true)      // Enable static file serving
            .freemarker(true)       // Enable Freemarker templating
            .i18n(true)            // Enable internationalization
            .productionLevel(ProductionLevel.DEVELOPMENT)
            .build();

        // Register and start the application
        FusionKit.registerWebApplication(new MyWebApplication(webAppConfig));
    }
}
```

### 3. Create your web application

```java
public class MyWebApplication extends WebApplication {
    public MyWebApplication(WebAppConfig config) {
        super(config);
    }
    
    @Override
    public void routes() {
        // Define your routes
        get("/", ctx -> ctx.render("index.ftl"));
        get("/api/users", UserController::getAllUsers);
        post("/api/users", UserController::createUser);
    }
}
```

### Debug Console

FusionKit includes a powerful web-based debug console accessible via:
- Press **F10** to toggle the debug console
- View application state, logs, and model data
- Interactive JavaScript console for runtime debugging

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Marc Andre Herpers** - [@marcandreher](https://github.com/marcandreher)

---

⭐ **Star this repository if you find it helpful!**