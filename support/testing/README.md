# Bisq Mobile BrowserStack Testing

This project contains automated UI tests for the Bisq mobile application using BrowserStack's cloud testing platform. The tests are written in Java using JUnit 5 and Appium.

## 1. Test Coverage

The test suite includes **3 main test cases**:

### 1. Bootstrap Test (`testBootstrap`)
Verifies the app launches successfully and displays the User Agreement screen.

### 2. Profile Creation Test (`testProfile`)
Tests user onboarding flow and profile persistence after app restart.

### 3. Notification Test (`testNotificationAfterExit`)
Validates that the "Bisq Service Is Running" notification appears when the app is backgrounded.

More can be added later, as required.

## 2. How to Run the Project

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Valid BrowserStack account with username and access key

### Running Tests

#### Single Device Testing
```bash
# From support/testing/
mvn test -P bisq-test
# From repo root
+mvn -f support/testing/pom.xml -B -q test -P bisq-test
```
Note: BrowserStack limits concurrent tests to 5 devices. For testing more devices, use batch testing below.

#### Batch Device Testing
```bash
./run_device_batches.sh
```

The batch testing script will:
- 5 YAML configurations are provided in /device_batches (update them manually as needed)
- Run tests sequentially for each batch

List of supported devices:
<https://www.browserstack.com/list-of-browsers-and-platforms/app_automate?fw-lang=java>

## 3. How to Upload APK to BrowserStack

### Using BrowserStack REST API
```bash
curl -u "${BROWSERSTACK_USERNAME}:${BROWSERSTACK_ACCESS_KEY}" \
    -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
    -F "file=@${APK_PATH:-BisqAndroid-0.0.56.apk}" \
    -F "bypass_secure_screen_restriction=false"
```

## 4. Configuration Settings

### Update App Configuration
After uploading, update the `app` field in `browserstack.yml` and in /device_batches:
```yaml
app: bs://your_app_id_here
```

### Settings APP_PACKAGE
The app package is configured in the `BrowserStackJUnitTest.java` file:
```java
String appPackage = "network.bisq.mobile.node.debug";
```

## 5. How to Set Username/Access Key

### Environment Variables (Recommended)
Get Username and Access key from <https://www.browserstack.com/accounts/profile/details>
```bash
export BROWSERSTACK_USERNAME="your_username"
export BROWSERSTACK_ACCESS_KEY="your_access_key"
```