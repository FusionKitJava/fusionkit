package de.marcandreher.fusion.core.externals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.marcandreher.fusionkit.core.externals.SitemapGenerator;

public class SitemapGeneratorTest {
    
    private static final String TEST_DOMAIN = "http://localhost:7000";
    private static final String TEST_DATA_DIR = "test-data";
    private Path testDataPath;
    
    @Before
    public void setUp() throws IOException {
        testDataPath = Paths.get(TEST_DATA_DIR);
        // Create test directory if it doesn't exist
        if (!Files.exists(testDataPath)) {
            Files.createDirectories(testDataPath);
        }
    }
    
    @After
    public void tearDown() throws IOException {
        // Clean up test files
        if (Files.exists(testDataPath)) {
            Files.walk(testDataPath)
                .map(Path::toFile)
                .forEach(File::delete);
            Files.deleteIfExists(testDataPath);
        }
    }
    
    @Test
    public void testSitemapGeneratedWithSingleUrl() throws MalformedURLException {
        SitemapGenerator generator = new SitemapGenerator(TEST_DOMAIN, TEST_DATA_DIR);
        generator.addUrl("/test1");
        
        long writeTime = generator.write();
        
        File sitemap = new File(TEST_DATA_DIR + "/sitemap.xml");
        Assert.assertTrue("Sitemap file should exist", sitemap.exists());
        Assert.assertTrue("Sitemap file should not be empty", sitemap.length() > 0);
        Assert.assertTrue("Write time should be positive", writeTime >= 0);
    }
    
    @Test
    public void testSitemapGeneratedWithMultipleUrls() throws MalformedURLException {
        SitemapGenerator generator = new SitemapGenerator(TEST_DOMAIN, TEST_DATA_DIR);
        generator.addUrl("/page1");
        generator.addUrl("/page2");
        generator.addUrl("/page3");
        generator.addSitemapUrl(0.8, "/special-page");
        
        long writeTime = generator.write();
        
        File sitemap = new File(TEST_DATA_DIR + "/sitemap.xml");
        Assert.assertTrue("Sitemap file should exist", sitemap.exists());
        Assert.assertTrue("Sitemap file should not be empty", sitemap.length() > 0);
        Assert.assertTrue("Write time should be positive", writeTime >= 0);
    }
    
    @Test
    public void testSitemapContentContainsUrls() throws MalformedURLException, IOException {
        SitemapGenerator generator = new SitemapGenerator(TEST_DOMAIN, TEST_DATA_DIR);
        generator.addUrl("/test-page");
        generator.addUrl("/another-page");
        
        generator.write();
        
        File sitemap = new File(TEST_DATA_DIR + "/sitemap.xml");
        String content = new String(Files.readAllBytes(sitemap.toPath()));
        
        Assert.assertTrue("Sitemap should contain XML declaration", 
            content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        Assert.assertTrue("Sitemap should contain test-page URL", 
            content.contains(TEST_DOMAIN + "/test-page"));
        Assert.assertTrue("Sitemap should contain another-page URL", 
            content.contains(TEST_DOMAIN + "/another-page"));
        Assert.assertTrue("Sitemap should contain urlset element", 
            content.contains("<urlset"));
        Assert.assertTrue("Sitemap should contain url elements", 
            content.contains("<url>"));
    }
    
    
    @Test
    public void testSitemapWithSpecialCharacters() throws MalformedURLException {
        SitemapGenerator generator = new SitemapGenerator(TEST_DOMAIN, TEST_DATA_DIR);
        generator.addUrl("/page-with-dashes");
        generator.addUrl("/page_with_underscores");
        generator.addUrl("/page/with/slashes");
        
        long writeTime = generator.write();
        
        File sitemap = new File(TEST_DATA_DIR + "/sitemap.xml");
        Assert.assertTrue("Sitemap file should exist", sitemap.exists());
        Assert.assertTrue("Sitemap file should not be empty", sitemap.length() > 0);
        Assert.assertTrue("Write time should be positive", writeTime >= 0);
    }

}
