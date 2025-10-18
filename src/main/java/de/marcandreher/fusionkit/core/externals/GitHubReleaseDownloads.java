package de.marcandreher.fusionkit.core.externals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.marcandreher.fusionkit.core.FusionKit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class GitHubReleaseDownloads {
    private static final OkHttpClient client = new OkHttpClient();

    // Simple in-memory cache keyed by "owner/repo" storing total downloads for latest release.
    private static final Cache<String, Long> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    private static Logger logger = FusionKit.getLogger(GitHubReleaseDownloads.class);

    private final String OWNER;
    private final String REPO;
    private final String API_URL;

    public GitHubReleaseDownloads(String owner, String repo) {
        this.OWNER = owner;
        this.REPO = repo;
        this.API_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
    }

    public long getTotalDownloadsForRelease() {
        final String key = OWNER + "/" + REPO;
        // Use cache: compute if absent
        try {
            return cache.get(key, k -> {
                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("Accept", "application/vnd.github+json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        return extractDownloadCount(json);
                    } else {
                        logger.error("Failed to fetch release data for {}: {}", key, response.message());
                        return 0L;
                    }
                } catch (IOException e) {
                    logger.error("IOException occurred while fetching release data for {}: {}", key, e.getMessage());
                    return 0L;
                }
            });
        } catch (RuntimeException e) {
            // Caffeine may wrap exceptions in RuntimeException; fallback to direct fetch
            e.printStackTrace();
            // fallback: direct fetch
            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Accept", "application/vnd.github+json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    return extractDownloadCount(json);
                } else {
                    logger.error("Failed to fetch release data for {}: {}", key, response.message());
                    return 0;
                }
            } catch (IOException ex) {
                logger.error("IOException occurred while fetching release data for {}: {}", key, ex.getMessage());
                return 0;
            }
        }
    }

    private long extractDownloadCount(String jsonInput) {
        long totalDownloads = 0;
        JsonObject json = JsonParser.parseString(jsonInput).getAsJsonObject();
        JsonArray assets = json.getAsJsonArray("assets");

        for (JsonElement element : assets) {
                JsonObject asset = element.getAsJsonObject();
                int count = asset.get("download_count").getAsInt();
                totalDownloads += count;
            }
        return totalDownloads;
    }
}
