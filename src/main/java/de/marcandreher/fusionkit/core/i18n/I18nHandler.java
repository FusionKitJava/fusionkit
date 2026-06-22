package de.marcandreher.fusionkit.core.i18n;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import de.marcandreher.fusionkit.core.WebAppConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class I18nHandler implements Handler {

    private final ClassLoader classLoader;
    private final WebAppConfig config;

    public I18nHandler(ClassLoader classLoader, WebAppConfig config) {
        this.classLoader = classLoader;
        this.config = config;
    }

    @Override
    public void handle(Context ctx) {
        try {
            // 1. Check for preferred language cookie first
            String preferredLanguage = ctx.cookie("preferred-language");
            String acceptLanguage = ctx.header("Accept-Language");

            // 2. Determine locale - support all available locales
            Locale locale;
            
            if (preferredLanguage != null) {
                // Use cookie preference - try to parse any language code
                locale = parseLocale(preferredLanguage);
            } else {
                // Fall back to Accept-Language header
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(
                    acceptLanguage != null ? acceptLanguage : "en"
                );
                // Try to find the best match from available locales
                locale = findBestMatch(ranges);
            }

            // 3. Get ResourceBundle with fallback
            ResourceBundle messages;
            try {
                messages = ResourceBundle.getBundle(config.i18n.getDirectory() + "/messages", locale, classLoader);
            } catch (MissingResourceException e) {
                // Fallback to default locale if bundle not found
                messages = ResourceBundle.getBundle(config.i18n.getDirectory() + "/messages", config.i18n.getDefaultLanguage(), classLoader);
                locale = config.i18n.getDefaultLanguage();
            }

            // 4. Store in attributes
            ctx.attribute("locale", locale);
            ctx.attribute("acceptLanguage", acceptLanguage != null ? acceptLanguage : "Not provided");
            ctx.attribute("msg", messages);

        } catch (Exception e) {
            // Fallback in case of any error
            ctx.attribute("locale", config.i18n.getDefaultLanguage());
            ctx.attribute("acceptLanguage", "Error occurred");
            ctx.attribute("msg", ResourceBundle.getBundle(config.i18n.getDirectory() + "/messages", config.i18n.getDefaultLanguage(), classLoader));
        }
    }
    
    /**
     * Parses a language string into a Locale object.
     * Supports formats like "en", "de", "en-US", "de-DE", etc.
     */
    private Locale parseLocale(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return config.i18n.getDefaultLanguage();
        }
        
        // Normalize the language code to use hyphens (standard format)
        String normalizedCode = languageCode.toLowerCase().replace("_", "-");
        
        try {
            // Use forLanguageTag which is the modern way to create locales
            return Locale.forLanguageTag(normalizedCode);
        } catch (Exception e) {
            // Fallback to default language if parsing fails
            return config.i18n.getDefaultLanguage();
        }
    }
    
    /**
     * Finds the best matching locale from Accept-Language header ranges.
     * First tries to find an exact match with available resource bundles,
     * then falls back to language-only matches, and finally to English.
     */
    private Locale findBestMatch(List<Locale.LanguageRange> ranges) {
        // Try each language range in order of preference
        for (Locale.LanguageRange range : ranges) {
            String langTag = range.getRange();
            
            // Skip wildcard ranges
            if ("*".equals(langTag)) {
                continue;
            }
            
            // Parse the language tag
            Locale candidate = Locale.forLanguageTag(langTag);
            
            // Check if we have a resource bundle for this locale
            if (hasResourceBundle(candidate, this.classLoader)) {
                return candidate;
            }
            
            // Try language-only match if we have country/variant
            if (!candidate.getCountry().isEmpty() || !candidate.getVariant().isEmpty()) {
                Locale languageOnly = Locale.forLanguageTag(candidate.getLanguage());
                if (hasResourceBundle(languageOnly, this.classLoader)) {
                    return languageOnly;
                }
            }
        }

        // Fall back to default language if no match found
        return config.i18n.getDefaultLanguage();
    }
    
    /**
     * Checks if a resource bundle exists for the given locale using the specified class loader.
     * This allows dynamic discovery of supported languages.
     */
    private boolean hasResourceBundle(Locale locale, ClassLoader classLoader) {
        try {
            ResourceBundle.getBundle(config.i18n.getDirectory() + "/messages", locale, classLoader);
            return true;
        } catch (MissingResourceException e) {
            return false;
        }
    }
}
