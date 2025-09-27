package com.browserstack;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
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
    public UiAutomator2Options options;
    public static Map<String, Object> browserStackYamlMap;
    public static final String USER_DIR = "user.dir";

    public BrowserStackJUnitTest() {
        File file = new File(getUserDir() + "/browserstack.yml");
        this.browserStackYamlMap = convertYamlFileToMap(file, new HashMap<>());
    }

    @BeforeAll
    static void setupOnce() throws Exception {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setNoReset(true);
        options.setFullReset(false);
        options.setApp("bs://b345188348e0d7cf61bc7cd3d58a28c93cf625c5");
        options.setPlatformName("Android");

        userName = System.getenv("BROWSERSTACK_USERNAME") != null ? System.getenv("BROWSERSTACK_USERNAME") : (String) browserStackYamlMap.get("userName");
        accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY") != null ? System.getenv("BROWSERSTACK_ACCESS_KEY") : (String) browserStackYamlMap.get("accessKey");
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

    private Map<String, Object> convertYamlFileToMap(File yamlFile, Map<String, Object> map) {
        try {
            InputStream inputStream = Files.newInputStream(yamlFile.toPath());
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            map.putAll(config);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Malformed browserstack.yml file - %s.", e));
        }
        return map;
    }

}
