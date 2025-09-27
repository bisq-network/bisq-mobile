package com.browserstack.utils;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitUtils {

    public static final int SHORT_WAIT = 10;
    public static final int MEDIUM_WAIT = 30;
    public static final int LONG_WAIT = 180;
    public static final int NOTIFICATION_WAIT = 5;

    public static WebDriverWait createWait(AndroidDriver driver, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    public static WebDriverWait createShortWait(AndroidDriver driver) {
        return createWait(driver, SHORT_WAIT);
    }

    public static WebDriverWait createMediumWait(AndroidDriver driver) {
        return createWait(driver, MEDIUM_WAIT);
    }

    public static WebDriverWait createLongWait(AndroidDriver driver) {
        return createWait(driver, LONG_WAIT);
    }

    public static void waitForAppToLoad(AndroidDriver driver) {
        WebDriverWait wait = createWait(driver, MEDIUM_WAIT);
        try {
            // Wait for any of the common app elements to appear
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.TextView[@text='User Agreement']")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("dashboard_content"))
            ));
        } catch (TimeoutException e) {
            // App might already be loaded, continue
        }
    }

    public static void waitForSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void waitFor5seconds(AndroidDriver driver) {
        waitForSeconds(5);
    }

    public static void waitFor10seconds(AndroidDriver driver) {
        waitForSeconds(10);
    }

    public static void waitForNotificationPanel(AndroidDriver driver) {
        WebDriverWait wait = createWait(driver, NOTIFICATION_WAIT);
        try {
            // Wait for notification panel elements to be present
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("com.android.systemui:id/notification_stack_scroller")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.TextView[contains(@text, 'Bisq')]"))
            ));
        } catch (TimeoutException e) {
            // Notification panel might be ready, continue
        }
    }
}
