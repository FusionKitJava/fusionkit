package de.marcandreher.fusionkit.core.config;

import java.io.File;
import java.io.IOException;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import de.marcandreher.fusionkit.core.error.FreemarkerExceptionHandler;
import de.marcandreher.fusionkit.core.javalin.ProductionLevel;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Version;
import lombok.Data;

public class FreemarkerConfiguration {

    private static final String FREEMARKER_VERSION = "2.3.34";

    private FreemarkerConfigModel model;

    public FreemarkerConfiguration() {
        File configFile = new File(".config/freemarker.toml");
        if (!configFile.exists()) {
            FreemarkerConfigModel defaultConfig = new FreemarkerConfigModel();
            TomlWriter writer = new TomlWriter();
            try {
                writer.write(defaultConfig, configFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.model = defaultConfig;
        } else {
            this.model = new Toml().read(configFile).to(FreemarkerConfigModel.class);
        }
    }

    public Configuration applyConfig(WebAppConfig webAppConfig, File templateDir) throws IOException {
        FileTemplateLoader templateLoader = new FileTemplateLoader(templateDir);
        Configuration fmConfig = new Configuration(new Version(FREEMARKER_VERSION));
        fmConfig.setTemplateLoader(templateLoader);
        fmConfig.setDirectoryForTemplateLoading(templateDir);
        fmConfig.setDefaultEncoding(webAppConfig.getTemplatesEncoding());
        fmConfig.setTemplateUpdateDelayMilliseconds(webAppConfig.isTemplatesAutoReload() ? 0 : Integer.MAX_VALUE);
        fmConfig.setDefaultEncoding(model.getDefaultEncoding());
        fmConfig.setNumberFormat(model.getNumberFormat());
        fmConfig.setWhitespaceStripping(model.isWhitespaceStripping());
        fmConfig.setLogTemplateExceptions(model.isLogTemplateExceptions());
        fmConfig.setWrapUncheckedExceptions(model.isWrapUncheckedExceptions());
        fmConfig.setTemplateUpdateDelayMilliseconds(model.getTemplateUpdateDelayMilliseconds());

        fmConfig.setDateFormat(model.getDateFormat());
        fmConfig.setDateTimeFormat(model.getDateTimeFormat());
        fmConfig.setBooleanFormat(model.getBooleanFormat());

        if (ProductionLevel.isInDevelopment(webAppConfig.getProductionLevel())) {
            fmConfig.setTemplateExceptionHandler(FreemarkerExceptionHandler.create(webAppConfig));
        }

        return fmConfig;
    }

    @Data
    public static class FreemarkerConfigModel {
        private String defaultEncoding = "UTF-8";
        private String numberFormat = "computer";
        private boolean whitespaceStripping = true;
        private boolean logTemplateExceptions = true;
        private boolean wrapUncheckedExceptions = true;
        private int templateUpdateDelayMilliseconds = 3000;

        private String dateFormat = "yyyy-MM-dd";
        private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
        private String booleanFormat = "true,false";
    }
}
