# Rediff Alert Handling — Selenium Mini Project

A Selenium + TestNG automation project that validates JavaScript alerts and window handling on the Rediffmail login page. Built as a single-file mini project demonstrating cross-browser support, screenshot capture, and TestNG assertions.

---

## What this project does

The Rediffmail login page (`https://mail.rediff.com/cgi-bin/login.cgi`) shows JS alerts when the user submits the form with missing fields, and opens linked pages in new tabs. This project automates four scenarios end-to-end:

1. **Empty username** → click Sign In → verify alert text *"Please enter a valid user name"*
2. **Empty password** (with username filled) → click Sign In → verify alert text *"Please enter your password"*
3. **Forgot Password** → click Next without details → verify the resulting alert *"Please enter your email ID"*
4. **Privacy Policy** link → verify it opens in a new tab on `policy.html`

Each test takes a screenshot afterwards (pass or fail) and the browser closes cleanly at the end.

---

## Tech stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 25 | Language |
| Selenium WebDriver | 4.43.0 | Browser automation |
| TestNG | 7.10.2 | Test framework, assertions, reports |
| Maven | — | Dependency management |
| IntelliJ IDEA | 2025.3 | IDE |

Selenium 4.6+ ships with **Selenium Manager**, so no `WebDriverManager` or manual driver downloads are needed — drivers are auto-resolved at runtime.

---

## Project structure

```
SeleniumTraining/
├── pom.xml
├── testng.xml
├── src/test/java/com/selenium/miniproject/
│   └── RediffAlertMiniProject.java   ← the entire project, single file
├── screenshots/                       ← auto-created, holds PNGs after each run
└── README.md
```

Everything is in **one file**: the test class, the `BrowserFactory` (nested), and the `ScreenshotUtil` (nested).

---

## Maven dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.43.0</version>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>7.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## How to run

The browser is selected through `testng.xml` using a parameter named `browserType`. To switch browsers, just edit the XML — no code changes needed.

### Run the suite
Right-click `testng.xml` in IntelliJ → **Run 'testng.xml'**.

### Run with a different browser

In `testng.xml`, comment out the `<test>` blocks you don't want and uncomment the one you do:

```xml
<suite name="RediffAlertSuite" parallel="tests" thread-count="2">

    <test name="Edge Test">
        <parameter name="browserType" value="edge" />
        <classes>
            <class name="com.selenium.miniproject.RediffAlertMiniProject"/>
        </classes>
    </test>

    <!-- Uncomment to run on Chrome too -->
    <!-- <test name="Chrome Test">
        <parameter name="browserType" value="chrome" />
        <classes>
            <class name="com.selenium.miniproject.RediffAlertMiniProject"/>
        </classes>
    </test> -->

</suite>
```

`parallel="tests" thread-count="2"` means if multiple `<test>` blocks are active they will run in parallel (up to 2 at a time), each in its own browser instance.

### Run directly from the IDE without testng.xml

If you right-click the test class and run it directly (no XML suite), the `@Optional("chrome")` fallback in `setUp()` kicks in and Chrome is used by default.

---

## Implementation walkthrough

### Step 1 — Browser factory (multi-browser support)

A nested `BrowserFactory` class returns a `WebDriver` based on a string. Each branch builds its own options object so you can customize per-browser flags without affecting the others.

```java
case "firefox": {
    FirefoxOptions opts = new FirefoxOptions();
    opts.addArguments("--start-maximized");
    return new FirefoxDriver(opts);
}
```

The factory throws `IllegalArgumentException` for unknown browser names, so a typo in `testng.xml` (e.g. `value="chorme"`) fails fast with a clear message instead of silently falling back to a default.

The browser name is injected by TestNG into `setUp()` via `@Parameters("browserType")`. The `@Optional("chrome")` annotation on the parameter means *"if no parameter is provided (running without testng.xml), default to chrome"*:

```java
@BeforeClass
@Parameters("browserType")
public void setUp(@Optional("chrome") String browserType) {
    driver = BrowserFactory.createDriver(browserType);
    // ...
}
```

### Step 2 — Element strategy: locally declared WebElements

Elements are declared inside each test using plain `driver.findElement()`. No class-level fields, no annotations, no PageFactory.

```java
@Test(priority = 1, ...)
public void testAlertOnEmptyUsername() {
    WebElement usernameField = driver.findElement(By.id("login1"));
    WebElement passwordField = driver.findElement(By.id("password"));
    WebElement loginButton   = driver.findElement(By.cssSelector("button.signin-btn"));
    // ... use them ...
}
```

This keeps each test self-contained — selectors live next to the actions that use them. There's no shared state between tests, and no risk of `StaleElementReferenceException` when Test 3 navigates away and back, because each test fetches fresh references when it runs.

### Step 3 — TestNG lifecycle

| Annotation | When | Purpose |
|---|---|---|
| `@BeforeClass` | Once, before any test | Read browser parameter, launch browser, load login page |
| `@Test(priority=N)` | In priority order | Run each scenario |
| `@AfterMethod` | After every test | Take screenshot |
| `@AfterClass` | Once, after all tests | Quit browser |

`priority` matters because the tests share page state — Test 2 expects the page from Test 1, etc. Without priorities, TestNG runs methods in alphabetical order, which would break the flow.

### Step 4 — Alert handling

A reusable helper waits for the alert, reads the text, and accepts it:

```java
private String handleAlert() {
    wait.until(ExpectedConditions.alertIsPresent());
    Alert alert = driver.switchTo().alert();
    String text = alert.getText();
    alert.accept();
    return text;
}
```

The returned text is then verified with `Assert.assertEquals(...)`.

### Step 5 — Window/tab handling

For the Privacy Policy test, after clicking we wait for two windows, then iterate through handles to switch to the non-original one:

```java
wait.until(ExpectedConditions.numberOfWindowsToBe(2));
for (String h : driver.getWindowHandles()) {
    if (!h.equals(parent)) {
        driver.switchTo().window(h);
        return;
    }
}
```

After verification, we close the new tab and switch back to the main window.

### Step 6 — Robust click (overlay fallback)

The Privacy Policy link sits at the bottom of the page, often blocked by a sticky ad. Two techniques solve this:

```java
js.executeScript("arguments[0].scrollIntoView({block:'center'});", privacyPolicyLink);
js.executeScript("arguments[0].click();", privacyPolicyLink);
```

Scroll the element into view, then JS-click to bypass any overlapping ad layer. The reusable `clickElement()` helper also has an `ElementClickInterceptedException` fallback to JS click.

### Step 7 — Screenshots

A nested `ScreenshotUtil` saves a PNG into `./screenshots/` with a timestamped filename. Three capture points exist:

- **Inside Test 3** — captured *while still on the forgot password page* before clicking Next. Otherwise the page is dismissed (popup closed or navigated back) before `@AfterMethod` runs.
- **Inside Test 4** — captured *while still in the new tab* before closing it. Otherwise the `@AfterMethod` capture would only see the login page after the new tab had closed.
- **After every test (`@AfterMethod`)** — a "final state" screenshot named `PASS_<testName>_<timestamp>.png` or `FAIL_...`.

> **Note on alert screenshots:** Selenium's `getScreenshotAs()` only captures DOM content, not native OS dialogs. JavaScript alerts on Chrome/Edge desktop are native popups — they don't appear in these PNGs. The screenshots show the page *behind* the alert (which is verified to be the expected page in code). To capture the actual dialog, you'd need an OS-level tool like `java.awt.Robot`.

### Step 8 — Assertions

Two TestNG assertion styles are demonstrated:

**Hard assertions** (`Assert.*`) — first failure stops the test:
```java
Assert.assertEquals(alertText, EXPECTED_ALERT_EMPTY_USERNAME, "...");
Assert.assertNotNull(driver.getTitle(), "...");
Assert.assertTrue(forgotPwdLink.isDisplayed(), "...");
```

**Soft assertions** (`SoftAssert`) — collect all failures, report at end:
```java
SoftAssert soft = new SoftAssert();
soft.assertEquals(...);
soft.assertTrue(...);
soft.assertAll();   // must call this or failures are silently ignored
```

Used in Test 4 to verify multiple aspects of the new tab in one go.

---

## Sample console output

```
=== Launching CHROME ===
Login page loaded: Rediffmail - Free Email for Login with Secure Access

--- Test 1: Empty Username ---
Alert text: "Please enter a valid user name"
Screenshot saved: ...\screenshots\PASS_testAlertOnEmptyUsername_20260428_143902.png

--- Test 2: Empty Password ---
Alert text: "Please enter your password"
Screenshot saved: ...\screenshots\PASS_testAlertOnEmptyPassword_20260428_143903.png

--- Test 3: Forgot Password ---
Screenshot saved: ...\screenshots\Test3_forgotPassword_page_20260428_143905.png
Alert text: "Please enter your email ID"
Forgot Password alert: Please enter your email ID
Screenshot saved: ...\screenshots\PASS_testForgotPasswordAlert_20260428_143905.png

--- Test 4: Privacy Policy New Tab ---
New tab title: Rediff: Welcome to rediff.com
New tab URL  : https://www.rediff.com/w3c/policy.html
Screenshot saved: ...\screenshots\Test4_privacyPolicy_newTab_20260428_143909.png
Screenshot saved: ...\screenshots\PASS_testPrivacyPolicyNewTab_20260428_143909.png

=== Browser closed ===

===============================================
Default Suite
Total tests run: 4, Passes: 4, Failures: 0, Skips: 0
===============================================
```

---

## Output screenshots

Captured automatically by `@AfterMethod` after each test, plus extra inline captures for Test 3 (forgot password page) and Test 4 (privacy policy page) — taken before the page is dismissed.

### Test 1 — Empty Username

The alert *"Please enter a valid user name"* fires and is verified in code. The PNG shows the login page underneath the native alert dialog.

![Empty Username](screenshots/PASS_testAlertOnEmptyUsername_20260428_143902.png)

### Test 2 — Empty Password

The alert *"Please enter your password"* fires and is verified.

![Empty Password](screenshots/PASS_testAlertOnEmptyPassword_20260428_143903.png)

### Test 3 — Forgot Password Page

Captured **inline** while still on the forgot password page, before clicking Next. This shows the actual page that triggers the alert.

![Forgot Password Page](screenshots/Test3_forgotPassword_page_20260428_143905.png)

### Test 4 — Privacy Policy in New Tab

Captured **inside** the new tab while still focused on `policy.html`. This is the meaningful evidence for this test.

![Privacy Policy Page](screenshots/Test4_privacyPolicy_newTab_20260428_143909.png)

> **Note:** filenames include a timestamp, so update the references above when viewing screenshots from a fresh run, or rename them to fixed names if you want this README to stay in sync.

---

## Key concepts demonstrated

- **Cross-browser execution** via a factory pattern + `testng.xml` parameter (`browserType`) + `@Optional` fallback
- **TestNG annotations**: `@BeforeClass`, `@Test`, `@AfterMethod`, `@AfterClass`, `@Parameters`, `@Optional`
- **Test ordering** via `priority` attribute
- **Plain WebElement usage** with `driver.findElement(By...)` declared locally per test
- **Explicit waits**: `WebDriverWait` + `ExpectedConditions` (no hardcoded sleeps for sync)
- **JS alert handling**: `driver.switchTo().alert()` + `getText()` + `accept()`
- **Window handling**: `getWindowHandles()` + `switchTo().window()`
- **Hard vs soft assertions**: `Assert` vs `SoftAssert`
- **Screenshot capture**: `TakesScreenshot` interface + `OutputType.FILE`, with inline captures for transient pages
- **Robust clicks**: `JavascriptExecutor` fallback for overlay-blocked elements
- **Parallel suite execution**: `parallel="tests" thread-count="2"` in `testng.xml`

---

## Troubleshooting

**SLF4J warning at startup:**
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
```
Harmless. Selenium's logger has no binding. Add `slf4j-simple` if you want it gone:
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.16</version>
    <scope>test</scope>
</dependency>
```

**Test 3 fails with "no alert found":**
Rediff sometimes shows inline HTML5 validation instead of a JS alert. The test handles both cases — check the console output to see which path was taken.

**Privacy Policy click intercepted:**
Already handled by the `scrollIntoView` + JS click combo. If you remove that, an ad overlay at the bottom of the page will block the native click.

**`IllegalArgumentException: Provide a valid browser name...`:**
Either the `value` in `testng.xml` is misspelled, or no parameter is being passed and somehow `@Optional` isn't matching. Confirm the value is exactly `chrome`, `firefox`, or `edge` (case-insensitive is fine).

**Why are there two screenshots per test for Tests 3 and 4?**
The `@AfterMethod` hook always runs at the end and saves a `PASS_<testName>_*.png`. But by then, transient pages (forgot password, privacy policy new tab) are gone — the browser is back on the login page. So those tests also save an inline screenshot **before** dismissal:

- `Test3_forgotPassword_page_*.png` — actual forgot password page
- `Test4_privacyPolicy_newTab_*.png` — actual policy page
- `PASS_test*_*.png` — post-cleanup state of the main window (login page)

The inline captures are the meaningful evidence; the `PASS_*` files are kept as a uniform "final state" record for every test.