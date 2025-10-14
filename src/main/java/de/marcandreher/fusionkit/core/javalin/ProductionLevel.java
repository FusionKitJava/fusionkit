package de.marcandreher.fusionkit.core.javalin;

public enum ProductionLevel {
    DEVELOPMENT,
    STAGING,
    PRODUCTION;

    public static boolean isInDevelopment(ProductionLevel level) {
        return level == DEVELOPMENT || level == STAGING;
    }
}
