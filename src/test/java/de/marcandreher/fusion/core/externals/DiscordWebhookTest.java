package de.marcandreher.fusion.core.externals;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

import de.marcandreher.fusionkit.core.externals.DiscordWebhook;
import de.marcandreher.fusionkit.core.externals.DiscordWebhook.Embed;

public class DiscordWebhookTest {
    
    private static final String VALID_WEBHOOK_URL = "https://discord.com/api/webhooks/123456789/test-webhook-token";
    private static final String INVALID_WEBHOOK_URL = "https://example.com/invalid";
    
    @Test
    public void testBuilderValidation() {
        // Test valid webhook URL
        assertNotNull("Should create builder with valid URL", new DiscordWebhook.Builder(VALID_WEBHOOK_URL));
        
        // Test invalid webhook URL
        try {
            new DiscordWebhook.Builder(INVALID_WEBHOOK_URL);
            fail("Should throw exception for invalid webhook URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error message about invalid URL", 
                e.getMessage().contains("Invalid Discord webhook URL format"));
        }
        
        // Test null webhook URL
        try {
            new DiscordWebhook.Builder(null);
            fail("Should throw exception for null webhook URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error message about null URL", 
                e.getMessage().contains("Webhook URL cannot be null or empty"));
        }
        
        // Test empty webhook URL
        try {
            new DiscordWebhook.Builder("");
            fail("Should throw exception for empty webhook URL");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error message about empty URL", 
                e.getMessage().contains("Webhook URL cannot be null or empty"));
        }
    }
    
    @Test
    public void testBuilderMethods() {
        DiscordWebhook.Builder builder = new DiscordWebhook.Builder(VALID_WEBHOOK_URL);
        
        // Test method chaining
        DiscordWebhook.Builder result = builder
                .setContent("Test message")
                .setUsername("Test Bot")
                .setAvatarUrl("https://example.com/avatar.png")
                .setTts(true);
        
        assertSame("Builder methods should return same instance", builder, result);
        
        // Test building webhook
        DiscordWebhook webhook = builder.build();
        assertNotNull("Should create webhook instance", webhook);
    }
    
    @Test
    public void testEmbedCreation() {
        Embed embed = new Embed()
                .setTitle("Test Title")
                .setDescription("Test Description")
                .setColor(0xFF0000)
                .setUrl("https://example.com")
                .setTimestampNow()
                .setFooter("Footer text", "https://example.com/icon.png")
                .setImage("https://example.com/image.png")
                .setThumbnail("https://example.com/thumb.png")
                .setAuthor("Author Name", "https://example.com", "https://example.com/author.png")
                .addField("Field 1", "Value 1", true)
                .addField("Field 2", "Value 2", false)
                .addBlankField(true);
        
        assertNotNull("Embed should be created", embed);
    }
    
    @Test
    public void testEmbedValidation() {
        Embed embed = new Embed();
        
        // Test title length validation
        String longTitle = "a".repeat(257); // Exceeds max length
        try {
            embed.setTitle(longTitle);
            fail("Should throw exception for title too long");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error about title length", 
                e.getMessage().contains("Title exceeds maximum length"));
        }
        
        // Test description length validation
        String longDescription = "a".repeat(4097); // Exceeds max length
        try {
            embed.setDescription(longDescription);
            fail("Should throw exception for description too long");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error about description length", 
                e.getMessage().contains("Description exceeds maximum length"));
        }
    }
    
    @Test
    public void testWebhookValidation() {
        // Test webhook with no content or embeds
        try {
            DiscordWebhook webhook = new DiscordWebhook.Builder(VALID_WEBHOOK_URL).build();
            webhook.send(); // This should be mocked in a real test
            fail("Should throw exception for webhook with no content");
        } catch (IllegalStateException e) {
            assertTrue("Should contain error about missing content", 
                e.getMessage().contains("Webhook must have either content or embeds"));
        } catch (IOException e) {
            // Expected in this test since we're using a fake URL
        }
    }
    
    @Test
    public void testContentValidation() {
        String longContent = "a".repeat(2001); // Exceeds max length
        
        try {
            DiscordWebhook webhook = new DiscordWebhook.Builder(VALID_WEBHOOK_URL)
                    .setContent(longContent)
                    .build();
            webhook.send(); // This should be mocked in a real test
            fail("Should throw exception for content too long");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error about content length", 
                e.getMessage().contains("Content exceeds maximum length"));
        } catch (IOException e) {
            // Expected in this test since we're using a fake URL
        }
    }
    
    @Test
    public void testEmbedLimitValidation() {
        DiscordWebhook.Builder builder = new DiscordWebhook.Builder(VALID_WEBHOOK_URL);
        
        // Add 11 embeds (exceeds limit of 10)
        for (int i = 0; i < 11; i++) {
            builder.addEmbed(new Embed().setTitle("Embed " + i));
        }
        
        try {
            DiscordWebhook webhook = builder.build();
            webhook.send(); // This should be mocked in a real test
            fail("Should throw exception for too many embeds");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error about embed limit", 
                e.getMessage().contains("Cannot have more than"));
        } catch (IOException e) {
            // Expected in this test since we're using a fake URL
        }
    }
    
    @Test
    public void testNullEmbedValidation() {
        try {
            new DiscordWebhook.Builder(VALID_WEBHOOK_URL).addEmbed(null);
            fail("Should throw exception for null embed");
        } catch (IllegalArgumentException e) {
            assertTrue("Should contain error about null embed", 
                e.getMessage().contains("Embed cannot be null"));
        }
    }
    
    @Test
    public void testBuilderEmbedMethods() {
        DiscordWebhook.Builder builder = new DiscordWebhook.Builder(VALID_WEBHOOK_URL);
        
        Embed embed1 = new Embed().setTitle("Embed 1");
        Embed embed2 = new Embed().setTitle("Embed 2");
        Embed embed3 = new Embed().setTitle("Embed 3");
        
        // Test adding multiple embeds
        builder.addEmbeds(embed1, embed2, embed3);
        
        // Test clearing embeds
        builder.clearEmbeds();
        
        // Should be able to build after clearing
        DiscordWebhook webhook = builder.setContent("Test").build();
        assertNotNull("Should create webhook after clearing embeds", webhook);
    }
    
    @Test
    public void testEmbedColorMethods() {
        Embed embed = new Embed();
        
        // Test RGB color method
        embed.setColor(255, 128, 64);
        
        // Test hex color method
        embed.setColor(0xFF8040);
        
        assertNotNull("Embed should be created with colors", embed);
    }
    
    @Test
    public void testUtilityMethods() {
        // Test utility embed creation methods
        assertNotNull("Should create success embed", 
            DiscordWebhook.Utils.createSuccessEmbed("Success", "It worked!"));
        assertNotNull("Should create error embed", 
            DiscordWebhook.Utils.createErrorEmbed("Error", "Something went wrong"));
        assertNotNull("Should create warning embed", 
            DiscordWebhook.Utils.createWarningEmbed("Warning", "Be careful"));
        assertNotNull("Should create info embed", 
            DiscordWebhook.Utils.createInfoEmbed("Info", "Here's some info"));
        
        // Test text truncation
        String longText = "This is a very long text that should be truncated";
        String truncated = DiscordWebhook.Utils.truncateText(longText, 20);
        assertTrue("Should truncate text", truncated.length() <= 20);
        assertTrue("Should end with ellipsis", truncated.endsWith("..."));
        
        // Test markdown escaping
        String markdown = "*bold* _italic_ `code` ~strikethrough~ |spoiler|";
        String escaped = DiscordWebhook.Utils.escapeMarkdown(markdown);
        assertFalse("Should escape markdown", escaped.contains("*bold*"));
        assertTrue("Should contain escaped characters", escaped.contains("\\*bold\\*"));
    }
    
}