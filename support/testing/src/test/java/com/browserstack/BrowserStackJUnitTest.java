package com.browserstack;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.ScreenOrientation;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

public class BrowserStackJUnitTest {

    String appPackage = "network.bisq.mobile.node.debug";

    public static AndroidDriver driver;
    public static String userName;
    public static String accessKey;
    public static UiAutomator2Options options;
    public static Map<String, Object> browserStackYamlMap;
    public static final String USER_DIR = "user.dir";

    // Static block to initialize YAML configuration
    static {
        try {
            File file = new File(System.getProperty(USER_DIR) + "/browserstack.yml");
            browserStackYamlMap = convertYamlFileToMap(file, new HashMap<>());
        } catch (Exception e) {
            browserStackYamlMap = new HashMap<>();
            System.err.println("Warning: Failed to load browserstack.yml - " + e.getMessage());
        }
    }

    public BrowserStackJUnitTest() {
        // Constructor no longer needs to initialize browserStackYamlMap
    }

    @BeforeAll
    static void setupOnce() throws Exception {
        options = new UiAutomator2Options();
        options.setNoReset(true);
        options.setFullReset(false);
        // options.setApp("bs://b345188348e0d7cf61bc7cd3d58a28c93cf625c5");
        options.setPlatformName("Android");

        // Get credentials from environment variables first, then fallback to YAML
        userName = System.getenv("BROWSERSTACK_USERNAME");
        if (userName == null) {
            userName = (String) browserStackYamlMap.get("userName");
        }
        
        accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (accessKey == null) {
            accessKey = (String) browserStackYamlMap.get("accessKey");
        }
        
        // Validate that we have credentials from either source
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalStateException("BrowserStack username not found. Please set BROWSERSTACK_USERNAME environment variable or add userName to browserstack.yml");
        }
        
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalStateException("BrowserStack access key not found. Please set BROWSERSTACK_ACCESS_KEY environment variable or add accessKey to browserstack.yml");
        }
        
        driver = new AndroidDriver(new URL(
                String.format("https://%s:%s@hub.browserstack.com/wd/hub", userName, accessKey)
        ), options);
    }

    @AfterAll
    static void tearDownOnce() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    public void setup() throws Exception {
        driver.activateApp(appPackage);
        driver.rotate(ScreenOrientation.PORTRAIT);
    }

    @AfterEach
    public void tearDown() throws Exception {
        driver.runAppInBackground(Duration.ofSeconds(-1));
        // Should ideally kill the app from memory, after each test and re-launch it for next test.
        // But some BrowserStack policy kills the session, when app gets killed from memory.
        // This results in fresh app install for each test. Thus storage getting cleared.
        // driver.terminateApp(appPackage); 
    }

    private String getUserDir() {
        return System.getProperty(USER_DIR);
    }

    private static Map<String, Object> convertYamlFileToMap(File yamlFile, Map<String, Object> map) {
        if (!yamlFile.exists()) {
            return map;
        }
        try (InputStream inputStream = Files.newInputStream(yamlFile.toPath())) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            if (config != null) {
                map.putAll(config);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(String.format("Failed to read browserstack.yml - %s.", e.getMessage()), e);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Malformed browserstack.yml file - %s.", e.getMessage()), e);
        }
        return map;
    }

}
