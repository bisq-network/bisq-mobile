package com.browserstack;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.browserstack.utils.WaitUtils;
import com.browserstack.utils.GestureUtils;

public class BisqTest extends BrowserStackJUnitTest {

    @Test
    @DisplayName("Should bootstrap")
    public void testBootstrap() {
        WebDriverWait longWait = WaitUtils.createLongWait(driver);

        WaitUtils.waitForAppToLoad(driver);
        
        // Wait for the User Agreement screen
        WebElement agreementTitle = longWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.TextView[@text='User Agreement']")));
        String titleText = agreementTitle.getText();
        assertEquals("User Agreement", titleText);
    }

    @Test
    @DisplayName("Should create and retain profile")
    @Order(1)
    public void testProfile() {
        WebDriverWait shortWait = WaitUtils.createShortWait(driver);
        WebDriverWait mediumWait = WaitUtils.createMediumWait(driver);
        WebDriverWait longWait = WaitUtils.createLongWait(driver);
        
        WaitUtils.waitForAppToLoad(driver);

        // Wait for the User Agreement screen
        WebElement agreementTitle = longWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.TextView[@text='User Agreement']")));

        // Wait for agreement checkbox and click it
        WebElement checkbox = shortWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("agreement_accept_checkbox")
        ));
        checkbox.click();

        // Click accept user agreement button
        WebElement acceptButton = shortWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("agreement_accept_button")
        ));
        acceptButton.click();

        // Click onboarding next button twice: Next, Create profile
        WebElement onboardingNextButton = mediumWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("onboarding_next_button")
        ));
        onboardingNextButton.click();
        onboardingNextButton.click();

        WebElement textfield = mediumWait.until(ExpectedConditions.presenceOfElementLocated(By.className("android.widget.EditText")));
        textfield.click();
        textfield.clear();
        textfield.sendKeys("nakamura");

        driver.hideKeyboard();

        // Wait for avatar image to be ready
        mediumWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("create_profile_avatar")
        ));

        WebElement createProfileNextButton = shortWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("create_profile_next_button")
        ));
        createProfileNextButton.click();

        WebElement notifConfirmButton = mediumWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("dialog_confirm_yes")
        ));
        notifConfirmButton.click();
        try {
            WebElement allowButton = mediumWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//android.widget.Button[@text='Allow']")));
            allowButton.click();
        } catch (TimeoutException e) {
            // Popup did not appear; continue
        }

        WaitUtils.waitFor5seconds(driver);

        driver.runAppInBackground(Duration.ofSeconds(-1));

        // Relaunch the app
        driver.activateApp(appPackage);
        driver.rotate(ScreenOrientation.PORTRAIT);
        WaitUtils.waitForAppToLoad(driver);

        // click on profile icon at top bar
        WebElement topBarAvatar = longWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.accessibilityId("top_bar_avatar")
        ));
        topBarAvatar.click();

        // check if nickname matches
        WebElement nameField = mediumWait.until(ExpectedConditions.presenceOfElementLocated(By.className("android.widget.EditText")));
        assertEquals("nakamura", nameField.getText());

        // Go back to home screen
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
    }

    @Test
    @DisplayName("Should show notification 'Bisq Service Is Running' after exiting the app")
    @Order(3)
    public void testNotificationAfterExit() {
        WebDriverWait shortWait = WaitUtils.createShortWait(driver);
        WebDriverWait longWait = WaitUtils.createLongWait(driver);

        WaitUtils.waitForAppToLoad(driver);

        if (exitAppAndCheckNotification()) {
            return;
        }

        driver.activateApp(appPackage);
        WaitUtils.waitForAppToLoad(driver);

        if (exitAppAndCheckNotification()) {
            return;
        }
        
        throw new AssertionError("Notification did not appear after exiting the app");
    }

    private boolean checkNotificationInPanel() {
        WebDriverWait wait = WaitUtils.createMediumWait(driver);
        try {
            WebElement notification = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//android.widget.TextView[@text='Bisq Service Is Running']")));
            assertTrue(notification.isDisplayed(), "Expected Bisq notification is not displayed!");
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean checkNotificationWithScroll() {
        if (checkNotificationInPanel()) {
            return true;
        }
        
        GestureUtils.swipeUpInNotifications(driver);

        return checkNotificationInPanel();
    }

    private boolean exitAppAndCheckNotification() {
        driver.runAppInBackground(Duration.ofSeconds(-1));
        WaitUtils.waitFor5seconds(driver);
        driver.openNotifications();
        WaitUtils.waitForNotificationPanel(driver);
        
        return checkNotificationWithScroll();
    }

}
