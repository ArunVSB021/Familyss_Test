package UI;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.testng.annotations.*;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class loginUITest {

    private static ExtentReports extent;
    private static ExtentTest test;
    private static String screenshotDir;
    private WebDriver driver;

    @BeforeSuite
    public void setupReport() {
        String reportPath = Paths.get("reports", "loginUITestReport.html").toString();
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

        extent = new ExtentReports();
        extent.attachReporter(spark);

        // Meta info
        extent.setSystemInfo("Project", "Familyss");
        extent.setSystemInfo("Tester", "Arun V");
        extent.setSystemInfo("Environment", "QA");

        // ensure screenshots folder exists
        screenshotDir = Paths.get("reports", "screenshots").toString();
        File dir = new File(screenshotDir);
        if (!dir.exists()) dir.mkdirs();
    }

    @BeforeMethod
    public void setUpTest(Method method) {
        test = extent.createTest(method.getName());

        // Setup WebDriverManager for Chrome
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    // Utility: Capture screenshot and save
    private String captureScreenshot(String fileName) {
        try {
            File screenshotFile = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            String path = Paths.get(screenshotDir, fileName + ".png").toString();
            File dest = new File(path);
            org.openqa.selenium.io.FileHandler.copy(screenshotFile, dest);

            return "screenshots/" + fileName + ".png";
        } catch (Exception e) {
            test.warning("Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    @Test
    public void testLogin() {
        try {
            driver.get("https://www.familyss.com/login");
            test.info("Navigated to login page");

            WebElement usernameField = driver.findElement(By.id("username"));
            usernameField.clear();
            usernameField.sendKeys("arunae021@gmail.com");
            test.info("Entered username");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.clear();
            passwordField.sendKeys("Pragamonish@2430");
            test.info("Entered password");

            driver.findElement(By.xpath("//button[@type='submit']")).click();
            test.info("Clicked login button");

            // Add validation (e.g., checking redirect or element after login)
            Thread.sleep(10000); // wait for navigation
            if (driver.getCurrentUrl().contains("dashboard")) {
                test.pass("Login successful, redirected to dashboard");
            } else {
                test.fail("Login failed, not redirected to dashboard");
                String ssPath = captureScreenshot("LoginFailed");
                if (ssPath != null) test.addScreenCaptureFromPath(ssPath);
            }

        } catch (Exception e) {
            test.fail("Test execution failed: " + e.getMessage());
            String ssPath = captureScreenshot("LoginError");
            if (ssPath != null) {
                test.addScreenCaptureFromPath(ssPath);
            }
            throw new RuntimeException(e);
        }
    }

    @AfterMethod
    public void tearDownTest() {
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterSuite
    public void tearDown() {
        extent.flush();
    }
}
