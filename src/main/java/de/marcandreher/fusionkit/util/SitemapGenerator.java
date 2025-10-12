package de.marcandreher.fusionkit.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redfin.sitemapgenerator.GoogleCodeSitemapUrl.Options;
import com.redfin.sitemapgenerator.WebSitemapGenerator;

public class SitemapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SitemapGenerator.class);

    private WebSitemapGenerator wsg = null;
    private String domain;
    private int urlCount = 0;
    private static final double MAX_PRIORITY = 1.0;
    private static final double MIN_PRIORITY = 0.1;

    public SitemapGenerator(String domain, String directoryPath) throws MalformedURLException {
        File sitemapDirectory = new File(directoryPath);
        
        try {
            wsg = new WebSitemapGenerator(domain, sitemapDirectory);
            this.domain = domain;
            logger.debug("WebSitemapGenerator created successfully");
        } catch (Exception e) {
            logger.error("Failed to create WebSitemapGenerator: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void addUrl(String url) {
        urlCount++;
        double priority = calculatePriority();
        addSitemapUrl(priority, url);
    }
    
    private double calculatePriority() {
        // First URL gets max priority (1.0), subsequent URLs get decreasing priority
        // Minimum priority is 0.1
        double priority = MAX_PRIORITY - ((urlCount - 1) * 0.1);
        return Math.max(priority, MIN_PRIORITY);
    }

    public void addSitemapUrl(String url) {
        urlCount++;
        double priority = calculatePriority();
        addSitemapUrlWithPriority(priority, url);
    }
    
    public void addSitemapUrl(double priority, String url) {
        addSitemapUrlWithPriority(priority, url);
    }
    
    private void addSitemapUrlWithPriority(double priority, String url) {
        try {
            String fullUrlString = domain + url;
            
            URI fullUri = new URI(fullUrlString);
            
            Options options = new Options(fullUri.toURL(), "xml");
            
            options.priority(priority);
            
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, cal.getFirstDayOfWeek());
            options.lastMod(cal.getTime());
            
            wsg.addUrl(options.build());
            logger.debug("Adding URL: {} with priority: {}", fullUrlString, priority);
        } catch (URISyntaxException e) {
            logger.error("URI syntax error for URL: {} - {}", url, e.getMessage(), e);
        } catch (MalformedURLException e) {
            logger.error("Malformed URL error for URL: {} - {}", url, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error adding sitemap URL: {} - {}", url, e.getMessage(), e);
        }
    }

    public void write() {
        try {
            wsg.write();
        } catch (Exception e) {
            logger.error("Error writing sitemap: {}", e.getMessage(), e);
            throw e;
        }
    }
}
