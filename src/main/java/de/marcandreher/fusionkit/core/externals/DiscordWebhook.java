package de.marcandreher.fusionkit.core.externals;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.marcandreher.fusionkit.core.FusionKit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A comprehensive Discord webhook wrapper for sending messages and embeds to Discord channels.
 * Supports content, embeds, username overrides, avatar overrides, and various embed features.
 */
public class DiscordWebhook {
    private static final Logger logger = FusionKit.getLogger(DiscordWebhook.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_EMBED_DESCRIPTION_LENGTH = 4096;
    private static final int MAX_EMBED_TITLE_LENGTH = 256;
    private static final int MAX_EMBEDS_PER_MESSAGE = 10;
    
    private final OkHttpClient client;
    private final String webhookUrl;
    private final String content;
    private final String username;
    private final String avatarUrl;
    private final List<Embed> embeds;
    private final boolean tts;

    private DiscordWebhook(Builder builder) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.webhookUrl = builder.webhookUrl;
        this.content = builder.content;
        this.username = builder.username;
        this.avatarUrl = builder.avatarUrl;
        this.embeds = new ArrayList<>(builder.embeds);
        this.tts = builder.tts;
    }

    /**
     * Sends the webhook message to Discord.
     * @throws IOException if the request fails
     * @throws IllegalStateException if the webhook has no content or embeds
     */
    public void send() throws IOException {
        validateMessage();
        
        JsonObject payload = createPayload();
        
        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .addHeader("User-Agent", "FusionKit-DiscordWebhook/1.0")
                .build();
        
        logger.debug("Sending Discord webhook to: {}", webhookUrl);
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                logger.error("Discord webhook failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new IOException("Discord webhook request failed: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            logger.debug("Discord webhook sent successfully");
        }
    }
    
    /**
     * Validates the message content and structure.
     */
    private void validateMessage() {
        if ((content == null || content.trim().isEmpty()) && embeds.isEmpty()) {
            throw new IllegalStateException("Webhook must have either content or embeds");
        }
        
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }
        
        if (embeds.size() > MAX_EMBEDS_PER_MESSAGE) {
            throw new IllegalArgumentException("Cannot have more than " + MAX_EMBEDS_PER_MESSAGE + " embeds per message");
        }
    }
    
    /**
     * Creates the JSON payload for the webhook request.
     */
    private JsonObject createPayload() {
        JsonObject payload = new JsonObject();

        if (content != null && !content.trim().isEmpty()) {
            payload.addProperty("content", content.trim());
        }
        
        if (username != null && !username.trim().isEmpty()) {
            payload.addProperty("username", username.trim());
        }
        
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            payload.addProperty("avatar_url", avatarUrl.trim());
        }
        
        if (tts) {
            payload.addProperty("tts", true);
        }

        if (!embeds.isEmpty()) {
            JsonArray embedsArray = new JsonArray();
            for (Embed embed : embeds) {
                embedsArray.add(embed.toJson());
            }
            payload.add("embeds", embedsArray);
        }
        
        return payload;
    }

    /**
     * Builder class for creating Discord webhook instances.
     */
    public static class Builder {
        private final String webhookUrl;
        private String content;
        private String username;
        private String avatarUrl;
        private boolean tts = false;
        private final List<Embed> embeds = new ArrayList<>();

        /**
         * Creates a new builder with the specified webhook URL.
         * @param webhookUrl The Discord webhook URL
         * @throws IllegalArgumentException if webhookUrl is null or empty
         */
        public Builder(String webhookUrl) {
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Webhook URL cannot be null or empty");
            }
            if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") && 
                !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
                throw new IllegalArgumentException("Invalid Discord webhook URL format");
            }
            this.webhookUrl = webhookUrl;
        }

        /**
         * Sets the text content of the message.
         * @param content The message content (max 2000 characters)
         * @return This builder instance
         */
        public Builder setContent(String content) {
            this.content = content;
            return this;
        }
        
        /**
         * Sets a custom username for the webhook message.
         * @param username The username to display
         * @return This builder instance
         */
        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }
        
        /**
         * Sets a custom avatar URL for the webhook message.
         * @param avatarUrl The avatar image URL
         * @return This builder instance
         */
        public Builder setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }
        
        /**
         * Enables or disables text-to-speech for the message.
         * @param tts Whether to enable TTS
         * @return This builder instance
         */
        public Builder setTts(boolean tts) {
            this.tts = tts;
            return this;
        }

        /**
         * Adds an embed to the message.
         * @param embed The embed to add
         * @return This builder instance
         * @throws IllegalArgumentException if embed is null
         */
        public Builder addEmbed(Embed embed) {
            if (embed == null) {
                throw new IllegalArgumentException("Embed cannot be null");
            }
            this.embeds.add(embed);
            return this;
        }
        
        /**
         * Adds multiple embeds to the message.
         * @param embeds The embeds to add
         * @return This builder instance
         */
        public Builder addEmbeds(Embed... embeds) {
            for (Embed embed : embeds) {
                addEmbed(embed);
            }
            return this;
        }
        
        /**
         * Clears all embeds from the message.
         * @return This builder instance
         */
        public Builder clearEmbeds() {
            this.embeds.clear();
            return this;
        }

        /**
         * Builds and returns a new DiscordWebhook instance.
         * @return The configured DiscordWebhook
         */
        public DiscordWebhook build() {
            return new DiscordWebhook(this);
        }
    }

    /**
     * Represents a Discord embed with rich content.
     */
    public static class Embed {
        private String title;
        private String description;
        private String url;
        private Integer color;
        private String timestamp;
        private Footer footer;
        private Image image;
        private Thumbnail thumbnail;
        private Author author;
        private final List<Field> fields = new ArrayList<>();

        /**
         * Sets the title of the embed.
         * @param title The title (max 256 characters)
         * @return This embed instance
         */
        public Embed setTitle(String title) {
            if (title != null && title.length() > MAX_EMBED_TITLE_LENGTH) {
                throw new IllegalArgumentException("Title exceeds maximum length of " + MAX_EMBED_TITLE_LENGTH + " characters");
            }
            this.title = title;
            return this;
        }

        /**
         * Sets the description of the embed.
         * @param description The description (max 4096 characters)
         * @return This embed instance
         */
        public Embed setDescription(String description) {
            if (description != null && description.length() > MAX_EMBED_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("Description exceeds maximum length of " + MAX_EMBED_DESCRIPTION_LENGTH + " characters");
            }
            this.description = description;
            return this;
        }
        
        /**
         * Sets the URL that the embed title links to.
         * @param url The URL
         * @return This embed instance
         */
        public Embed setUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the color of the embed.
         * @param color The color as an integer (0x000000 to 0xFFFFFF)
         * @return This embed instance
         */
        public Embed setColor(int color) {
            this.color = color;
            return this;
        }
        
        /**
         * Sets the color of the embed using RGB values.
         * @param r Red component (0-255)
         * @param g Green component (0-255)
         * @param b Blue component (0-255)
         * @return This embed instance
         */
        public Embed setColor(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }
        
        /**
         * Sets the timestamp of the embed to the current time.
         * @return This embed instance
         */
        public Embed setTimestampNow() {
            this.timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return this;
        }
        
        /**
         * Sets a custom timestamp for the embed.
         * @param timestamp The timestamp in ISO 8601 format
         * @return This embed instance
         */
        public Embed setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        /**
         * Sets the footer of the embed.
         * @param text The footer text
         * @return This embed instance
         */
        public Embed setFooter(String text) {
            this.footer = new Footer(text, null);
            return this;
        }
        
        /**
         * Sets the footer of the embed with an icon.
         * @param text The footer text
         * @param iconUrl The footer icon URL
         * @return This embed instance
         */
        public Embed setFooter(String text, String iconUrl) {
            this.footer = new Footer(text, iconUrl);
            return this;
        }
        
        /**
         * Sets the main image of the embed.
         * @param imageUrl The image URL
         * @return This embed instance
         */
        public Embed setImage(String imageUrl) {
            this.image = new Image(imageUrl);
            return this;
        }
        
        /**
         * Sets the thumbnail of the embed.
         * @param thumbnailUrl The thumbnail URL
         * @return This embed instance
         */
        public Embed setThumbnail(String thumbnailUrl) {
            this.thumbnail = new Thumbnail(thumbnailUrl);
            return this;
        }
        
        /**
         * Sets the author of the embed.
         * @param name The author name
         * @return This embed instance
         */
        public Embed setAuthor(String name) {
            this.author = new Author(name, null, null);
            return this;
        }
        
        /**
         * Sets the author of the embed with URL and icon.
         * @param name The author name
         * @param url The author URL
         * @param iconUrl The author icon URL
         * @return This embed instance
         */
        public Embed setAuthor(String name, String url, String iconUrl) {
            this.author = new Author(name, url, iconUrl);
            return this;
        }
        
        /**
         * Adds a field to the embed.
         * @param name The field name
         * @param value The field value
         * @param inline Whether the field should be inline
         * @return This embed instance
         */
        public Embed addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }
        
        /**
         * Adds a non-inline field to the embed.
         * @param name The field name
         * @param value The field value
         * @return This embed instance
         */
        public Embed addField(String name, String value) {
            return addField(name, value, false);
        }
        
        /**
         * Adds a blank field (useful for spacing).
         * @param inline Whether the field should be inline
         * @return This embed instance
         */
        public Embed addBlankField(boolean inline) {
            return addField("\u200B", "\u200B", inline);
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            
            if (title != null && !title.trim().isEmpty()) {
                json.addProperty("title", title.trim());
            }
            if (description != null && !description.trim().isEmpty()) {
                json.addProperty("description", description.trim());
            }
            if (url != null && !url.trim().isEmpty()) {
                json.addProperty("url", url.trim());
            }
            if (color != null) {
                json.addProperty("color", color);
            }
            if (timestamp != null && !timestamp.trim().isEmpty()) {
                json.addProperty("timestamp", timestamp.trim());
            }
            if (footer != null) {
                json.add("footer", footer.toJson());
            }
            if (image != null) {
                json.add("image", image.toJson());
            }
            if (thumbnail != null) {
                json.add("thumbnail", thumbnail.toJson());
            }
            if (author != null) {
                json.add("author", author.toJson());
            }
            if (!fields.isEmpty()) {
                JsonArray fieldsArray = new JsonArray();
                for (Field field : fields) {
                    fieldsArray.add(field.toJson());
                }
                json.add("fields", fieldsArray);
            }
            
            return json;
        }
        
        // Helper classes for embed components
        private static class Footer {
            private final String text;
            private final String iconUrl;
            
            Footer(String text, String iconUrl) {
                this.text = text;
                this.iconUrl = iconUrl;
            }
            
            JsonObject toJson() {
                JsonObject json = new JsonObject();
                if (text != null && !text.trim().isEmpty()) {
                    json.addProperty("text", text.trim());
                }
                if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                    json.addProperty("icon_url", iconUrl.trim());
                }
                return json;
            }
        }
        
        private static class Image {
            private final String url;
            
            Image(String url) {
                this.url = url;
            }
            
            JsonObject toJson() {
                JsonObject json = new JsonObject();
                if (url != null && !url.trim().isEmpty()) {
                    json.addProperty("url", url.trim());
                }
                return json;
            }
        }
        
        private static class Thumbnail {
            private final String url;
            
            Thumbnail(String url) {
                this.url = url;
            }
            
            JsonObject toJson() {
                JsonObject json = new JsonObject();
                if (url != null && !url.trim().isEmpty()) {
                    json.addProperty("url", url.trim());
                }
                return json;
            }
        }
        
        private static class Author {
            private final String name;
            private final String url;
            private final String iconUrl;
            
            Author(String name, String url, String iconUrl) {
                this.name = name;
                this.url = url;
                this.iconUrl = iconUrl;
            }
            
            JsonObject toJson() {
                JsonObject json = new JsonObject();
                if (name != null && !name.trim().isEmpty()) {
                    json.addProperty("name", name.trim());
                }
                if (url != null && !url.trim().isEmpty()) {
                    json.addProperty("url", url.trim());
                }
                if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                    json.addProperty("icon_url", iconUrl.trim());
                }
                return json;
            }
        }
        
        private static class Field {
            private final String name;
            private final String value;
            private final boolean inline;
            
            Field(String name, String value, boolean inline) {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }
            
            JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.addProperty("name", name != null ? name.trim() : "");
                json.addProperty("value", value != null ? value.trim() : "");
                json.addProperty("inline", inline);
                return json;
            }
        }
    }
    
    /**
     * Utility class for common Discord webhook operations.
     */
    public static class Utils {
        
        /**
         * Common Discord embed colors.
         */
        public static class Colors {
            public static final int SUCCESS = 0x00FF00;
            public static final int WARNING = 0xFFFF00;
            public static final int ERROR = 0xFF0000;
            public static final int INFO = 0x0099FF;
            public static final int PURPLE = 0x9932CC;
            public static final int ORANGE = 0xFFA500;
            public static final int BLURPLE = 0x5865F2; // Discord's brand color
        }
        
        /**
         * Creates a simple success embed.
         * @param title The embed title
         * @param description The embed description
         * @return A configured Embed instance
         */
        public static Embed createSuccessEmbed(String title, String description) {
            return new Embed()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Colors.SUCCESS)
                    .setTimestampNow();
        }
        
        /**
         * Creates a simple error embed.
         * @param title The embed title
         * @param description The embed description
         * @return A configured Embed instance
         */
        public static Embed createErrorEmbed(String title, String description) {
            return new Embed()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Colors.ERROR)
                    .setTimestampNow();
        }
        
        /**
         * Creates a simple warning embed.
         * @param title The embed title
         * @param description The embed description
         * @return A configured Embed instance
         */
        public static Embed createWarningEmbed(String title, String description) {
            return new Embed()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Colors.WARNING)
                    .setTimestampNow();
        }
        
        /**
         * Creates a simple info embed.
         * @param title The embed title
         * @param description The embed description
         * @return A configured Embed instance
         */
        public static Embed createInfoEmbed(String title, String description) {
            return new Embed()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Colors.INFO)
                    .setTimestampNow();
        }
        
        /**
         * Truncates text to fit Discord's limits.
         * @param text The text to truncate
         * @param maxLength The maximum allowed length
         * @return The truncated text
         */
        public static String truncateText(String text, int maxLength) {
            if (text == null || text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 3) + "...";
        }
        
        /**
         * Escapes Discord markdown characters in text.
         * @param text The text to escape
         * @return The escaped text
         */
        public static String escapeMarkdown(String text) {
            if (text == null) {
                return null;
            }
            return text.replace("*", "\\*")
                      .replace("_", "\\_")
                      .replace("`", "\\`")
                      .replace("~", "\\~")
                      .replace("|", "\\|");
        }
    }
}