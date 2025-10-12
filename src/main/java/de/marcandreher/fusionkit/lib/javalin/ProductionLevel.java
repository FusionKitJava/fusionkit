package de.marcandreher.fusionkit.lib.javalin;

public enum ProductionLevel {
    DEVELOPMENT,
    STAGING,
    PRODUCTION;

    public static boolean isInDevelopment(ProductionLevel level) {
        return level == DEVELOPMENT || level == STAGING;
    }
}
