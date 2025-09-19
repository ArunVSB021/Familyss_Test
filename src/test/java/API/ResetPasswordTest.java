package API;


import org.testng.Assert;
import org.testng.annotations.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.http.ContentType;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.time.Duration;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

import static org.hamcrest.Matchers.*;

public class ResetPasswordTest {

    private static ExtentReports extent;
    private static ExtentTest test;
    private static String screenshotDir;
    private static String otp; // to store OTP for reset password
    private WebDriver driver;
    private WebDriverWait wait;
    @BeforeSuite
    public void setupReport() {
        String reportPath = Paths.get("reports", "ResetPasswordReport.html").toString();
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

        extent = new ExtentReports();
        extent.attachReporter(spark);

        // Meta info
        extent.setSystemInfo("Project", "Familyss");
        extent.setSystemInfo("Tester", "Arun V");
        extent.setSystemInfo("Environment", "QA");

        // Ensure screenshots folder exists
        screenshotDir = Paths.get("reports", "screenshots").toString();
        File dir = new File(screenshotDir);
        if (!dir.exists()) dir.mkdirs();
    }

    @BeforeMethod
    public void setUpTest(Method method) {
        test = extent.createTest(method.getName());
        RestAssured.baseURI = "https://fawfcakk57azargc4dxbsfhgqe0sjobd.lambda-url.eu-north-1.on.aws";
    }

    // --- Utility: Save JSON or text as image ---
    private String saveTextAsImage(String text, String fileName) {
        try {
            int width = 1000;
            int lineHeight = 20;
            String[] lines = text.split("\n");
            int height = Math.max(200, lines.length * lineHeight + 40);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Consolas", Font.PLAIN, 14));

            int y = 20;
            for (String line : lines) {
                g.drawString(line, 10, y);
                y += lineHeight;
            }
            g.dispose();

            String path = Paths.get(screenshotDir, fileName + ".png").toString();
            ImageIO.write(image, "png", new File(path));

            return "screenshots/" + fileName + ".png";

        } catch (Exception e) {
            test.warning("Failed to save JSON as image: " + e.getMessage());
            return null;
        }
    }

    // --- Utility: Read JSON from file ---
    private String readJsonFromFile(String fileName) {
        try {
            String basePath = System.getProperty("user.dir");
            String filePath = Paths.get(basePath, "testdata", fileName).toString();

            return Files.lines(Paths.get(filePath))
                        .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            test.fail("Failed to read JSON file: " + fileName + " -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // --- Test 1: Request OTP ---
    @Test(priority = 1)
    public void testRequestOTP() {
        String requestBody = readJsonFromFile("requestOtp.json");
        String reqImagePath = saveTextAsImage(requestBody, "RequestOTP_Request");
        if (reqImagePath != null) test.info("Request JSON").addScreenCaptureFromPath(reqImagePath);

        try {
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/user/resend-otp")
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("$", hasKey("message"))
                    .extract().response();

            String resImagePath = saveTextAsImage(response.asPrettyString(), "RequestOTP_Response");
            if (resImagePath != null) test.info("Response JSON").addScreenCaptureFromPath(resImagePath);

            test.pass("OTP request API passed with status: " + response.statusCode());

        } catch (Exception e) {
            test.fail("OTP request API failed: " + e.getMessage());
            throw e;
        }
    }

    // --- Test 2: Fetch OTP from Mailinator ---
    @Test(priority = 2)
    public void testFetchOTPFromMailinator() throws InterruptedException {
        try {
        	 WebDriverManager.chromedriver().setup();

             driver = new ChromeDriver();
             driver.manage().window().maximize();

            // Example: Mailinator public inbox
             driver.get("https://www.mailinator.com/v4/public/inboxes.jsp");

             // 2. Enter inbox name (use your public inbox name, e.g. test021)
             WebElement inboxField = wait.until(ExpectedConditions.elementToBeClickable(By.id("inbox_field")));
             inboxField.sendKeys("test021"); // <-- change this to your email prefix
             driver.findElement(By.xpath("//button[contains(text(),'GO')]")).click();

             // 3. Wait for inbox table and click the latest mail row
             WebElement mailRow = wait.until(ExpectedConditions.elementToBeClickable(
                     By.xpath("//table[@id='inboxpane']//tr[1]")));
             mailRow.click();

             // 4. Switch into the email iframe
             wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("msg_body")));

             // 5. Extract OTP (example: from an <h1> tag or text containing digits)
             WebElement otpElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                     By.xpath("//h1[contains(text(),'')]")));
             String otpText = otpElement.getText();
             System.out.println("Extracted OTP: " + otpText);
             Thread.sleep(5000);
             Assert.assertTrue(otpText.matches("\\d+"), "OTP should be numeric!");

//            driver.quit();

        } catch (Exception e) {
            test.fail("Failed to fetch OTP from Mailinator: " + e.getMessage());
            if (driver != null) driver.quit();
            throw new RuntimeException(e);
        }
    }

    // --- Test 3: Reset Password ---
    @Test(priority = 3, dependsOnMethods = {"testFetchOTPFromMailinator"})
    public void testResetPassword() {
        String jsonTemplate = readJsonFromFile("resetPassword.json");
        String requestBody = jsonTemplate.replace("{{OTP}}", otp); // inject fetched OTP

        String reqImagePath = saveTextAsImage(requestBody, "ResetPassword_Request");
        if (reqImagePath != null) test.info("Request JSON").addScreenCaptureFromPath(reqImagePath);

        try {
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/user/reset-password")
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("$", hasKey("message"))
                    .extract().response();

            String resImagePath = saveTextAsImage(response.asPrettyString(), "ResetPassword_Response");
            if (resImagePath != null) test.info("Response JSON").addScreenCaptureFromPath(resImagePath);

            test.pass("Reset password API passed with status: " + response.statusCode());

        } catch (Exception e) {
            test.fail("Reset password API failed: " + e.getMessage());
            throw e;
        }
    }

    @AfterSuite
    public void tearDown() {
        extent.flush();
    }
}
