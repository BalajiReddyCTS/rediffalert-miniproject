package com.selenium.miniproject;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Case Study: Alert Handling - Rediff Login Page
 * Site: https://mail.rediff.com/cgi-bin/login.cgi
 */
public class RediffAlertMiniProject {

    private WebDriver driver;
    private WebDriverWait wait;
    private SoftAssert softAssert;

    private static final String EXPECTED_ALERT_EMPTY_USERNAME = "Please enter a valid user name";
    private static final String EXPECTED_ALERT_EMPTY_PASSWORD = "Please enter your password";

    // ════════════════════════════════════════════════════════════════════════
    //  1. Browser Factory
    // ════════════════════════════════════════════════════════════════════════

    static class BrowserFactory {
        static WebDriver createDriver(String browser) {
            switch (browser.toLowerCase()) {
                case "firefox": {
                    FirefoxOptions opts = new FirefoxOptions();
                    opts.addArguments("--start-maximized");
                    opts.addArguments("--disable-notifications");
                    return new FirefoxDriver(opts);
                }
                case "edge": {
                    EdgeOptions opts = new EdgeOptions();
                    opts.addArguments("--start-maximized");
                    opts.addArguments("--disable-notifications");
                    return new EdgeDriver(opts);
                }
                case "chrome": {
                    ChromeOptions opts = new ChromeOptions();
                    opts.addArguments("--start-maximized");
                    opts.addArguments("--disable-notifications");
                    return new ChromeDriver(opts);
                }
                default:
                    throw new IllegalArgumentException("Provide a valid browser (chrome, firefox, edge). Got: " + browser);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  2. Tests
    // ════════════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Sign In with empty username triggers alert")
    public void testAlertOnEmptyUsername() {
        System.out.println("\n--- Test 1: Empty Username ---");

        WebElement usernameField = driver.findElement(By.id("login1"));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton   = driver.findElement(By.cssSelector("button.signin-btn"));

        usernameField.clear();
        passwordField.clear();
        clickElement(loginButton);

        String alertText = handleAlert();
        softAssert.assertEquals(alertText, EXPECTED_ALERT_EMPTY_USERNAME, "Alert text mismatch");
        softAssert.assertAll();
    }

    @Test(priority = 2, description = "Sign In with empty password triggers alert")
    public void testAlertOnEmptyPassword() {
        System.out.println("\n--- Test 2: Empty Password ---");

        WebElement usernameField = driver.findElement(By.id("login1"));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton   = driver.findElement(By.cssSelector("button.signin-btn"));

        usernameField.clear();
        usernameField.sendKeys("testuser");
        passwordField.clear();
        clickElement(loginButton);

        String alertText = handleAlert();
        softAssert.assertEquals(alertText, EXPECTED_ALERT_EMPTY_PASSWORD, "Alert text mismatch");
        softAssert.assertAll();
    }

    @Test(priority = 3, description = "Forgot Password page validation")
    public void testForgotPasswordAlert() {
        System.out.println("\n--- Test 3: Forgot Password ---");

        WebElement forgotPwdLink = driver.findElement(By.cssSelector("a[href*='newforgot']"));
        String mainWindow = driver.getWindowHandle();

        clickElement(forgotPwdLink);
        sleep(2000);

        handleForgotPasswordPage();
        driver.navigate().back();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login1"))); //wait till login page is loaded

        softAssert.assertEquals(driver.getWindowHandles().size(), 1, "Should be back to 1 window after Forgot Password test");
        softAssert.assertAll();
    }

    @Test(priority = 4, description = "Privacy Policy link opens in a new tab")
    public void testPrivacyPolicyNewTab() {
        System.out.println("\n--- Test 4: Privacy Policy New Tab ---");

        WebElement privacyLink = driver.findElement(By.cssSelector("a[href*='policy.html']"));
        String mainWindow = driver.getWindowHandle();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Scroll into view + JS click (avoids ad-overlay interception)
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", privacyLink);
        sleep(1000);
        js.executeScript("arguments[0].click();", privacyLink);

        wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        switchToNewWindow(mainWindow);
        wait.until(ExpectedConditions.urlContains("policy")); //wait.until(d -> d.getCurrentUrl().contains("policy"));
        sleep(1500);

        System.out.println("New tab URL: " + driver.getCurrentUrl());
        ScreenshotUtil.capture(driver, "Test4_privacyPolicy_newTab");

        softAssert.assertEquals(driver.getWindowHandles().size(), 2, "Should have 2 windows");
        softAssert.assertTrue(driver.getCurrentUrl().contains("policy"), "URL should contain 'policy' but was: " + driver.getCurrentUrl());

        driver.close();
        driver.switchTo().window(mainWindow);

        softAssert.assertAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  3. Reusable Functions
    // ════════════════════════════════════════════════════════════════════════

    private String handleAlert() {
        wait.until(ExpectedConditions.alertIsPresent());
        Alert alert = driver.switchTo().alert();
        String text = alert.getText();
        System.out.println("Alert text: \"" + text + "\"");
        alert.accept();
        return text;
    }

    private void handleForgotPasswordPage() {
        try {
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='submit'], button[type='submit']")));

            ScreenshotUtil.capture(driver, "Test3_forgotPassword_page");
            clickElement(nextBtn);

            try {
                wait.until(ExpectedConditions.alertIsPresent());
                String alertText = handleAlert();
                softAssert.assertNotNull(alertText, "Forgot Password alert text should not be null");
                softAssert.assertFalse(alertText.isEmpty(), "Forgot Password alert text should not be empty");
            } catch (Exception noAlert) {
                System.out.println("No JS alert (likely inline HTML5 validation).");
            }
        } catch (Exception e) {
            System.out.println("Next button not found: " + e.getMessage());
        }
    }

    private void switchToNewWindow(String parent) {
        for (String h : driver.getWindowHandles()) {
            if (!h.equals(parent)) {
                driver.switchTo().window(h);
                return;
            }
        }
    }

    /** Waits for clickability, clicks; falls back to JS click if intercepted. */
    private void clickElement(WebElement el) {
        wait.until(ExpectedConditions.elementToBeClickable(el));
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** Saves Selenium screenshots into ./screenshots/. */
    static class ScreenshotUtil {
        private static final String SCREENSHOT_DIR = "screenshots";
        private static final DateTimeFormatter TS =
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        static String capture(WebDriver driver, String name) {
            try {
                Path dir = Paths.get(SCREENSHOT_DIR);
                if (!Files.exists(dir)) Files.createDirectories(dir);

                File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path target = dir.resolve(name + "_" + LocalDateTime.now().format(TS) + ".png");

                Files.copy(src.toPath(), target);
                System.out.println("Screenshot saved: " + target.toAbsolutePath());
                return target.toString();
            } catch (IOException e) {
                System.out.println("Screenshot failed: " + e.getMessage());
                return null;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  4. Setup / Teardown
    // ════════════════════════════════════════════════════════════════════════

    @BeforeClass
    @Parameters("browserType")
    public void setUp(@Optional("chrome") String browserType) {
        System.out.println("=== Launching " + browserType.toUpperCase() + " ===");

        driver = BrowserFactory.createDriver(browserType);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(15));

        driver.get("https://mail.rediff.com/cgi-bin/login.cgi");
        driver.manage().deleteAllCookies();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login1")));

        System.out.println("Login page loaded: " + driver.getTitle());
    }

    @BeforeMethod
    public void resetSoftAssert() {
        softAssert = new SoftAssert();
    }

    @AfterMethod
    public void afterEachTest(ITestResult result) {
        String status = result.isSuccess() ? "PASS" : "FAIL";
        ScreenshotUtil.capture(driver, status + "_" + result.getName());
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("=== Browser closed ===");
        }
    }
}