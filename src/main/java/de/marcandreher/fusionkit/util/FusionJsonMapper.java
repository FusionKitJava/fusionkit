package de.marcandreher.fusionkit.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.javalin.json.JsonMapper;

public class FusionJsonMapper implements JsonMapper {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String toJsonString(Object obj, Type type) {
        return gson.toJson(obj, type);
    }

    @Override
    public InputStream toJsonStream(Object obj, Type type) {
        String json = toJsonString(obj, type);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <T> T fromJsonString(String json, Type type) {
        return gson.fromJson(json, type);
    }

    @Override
    public <T> T fromJsonStream(InputStream json, Type targetType) {
        return gson.fromJson(new InputStreamReader(json, StandardCharsets.UTF_8), targetType);
    }
}
