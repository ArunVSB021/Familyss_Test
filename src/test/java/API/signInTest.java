package API;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;

public class signInTest {

    private static ExtentReports extent;
    private static ExtentTest test;
    private static String screenshotDir;
    private static String jsonDir;

    @BeforeSuite
    public void setupReport() {
        String reportPath = Paths.get("reports", "signinTestReport.html").toString();
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

        // JSON test data folder
        jsonDir = Paths.get("testdata").toString();
        File jsonFolder = new File(jsonDir);
        if (!jsonFolder.exists()) jsonFolder.mkdirs();
    }

    @BeforeMethod
    public void setUpTest(Method method) {
        test = extent.createTest(method.getName());
        RestAssured.baseURI = "https://fawfcakk57azargc4dxbsfhgqe0sjobd.lambda-url.eu-north-1.on.aws";
    }

    // --- Utility: Save JSON text as an image for attaching to Extent report ---
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

    // --- Utility: Read JSON file as String ---
 // --- Utility: Read JSON file as String ---
    private String readJsonFromFile(String fileName) {
        try {
            // Ensure consistent base path (project root + testdata folder)
            String basePath = System.getProperty("user.dir"); // project root at runtime
            String filePath = Paths.get(basePath, "testdata", fileName).toString();

            return Files.lines(Paths.get(filePath))
                        .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            test.fail("Failed to read JSON file: " + fileName + " -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


   
    @Test
    public void testLoginSuccess() {
        String requestBody = readJsonFromFile("login_success.json");

        String reqImagePath = saveTextAsImage(requestBody, "LoginSuccess_Request");
        if (reqImagePath != null) test.info("Request JSON").addScreenCaptureFromPath(reqImagePath);

        try {
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/user/login")
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("$", anyOf(
                            hasKey("token"),
                            hasKey("accessToken"),
                            hasKey("jwt")
                    ))
                    .extract().response();

            String resImagePath = saveTextAsImage(response.asPrettyString(), "LoginSuccess_Response");
            if (resImagePath != null) test.info("Response JSON").addScreenCaptureFromPath(resImagePath);

            test.pass("Login API passed with status code: " + response.statusCode());

        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Login API execution failed: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void testLoginFailure() {
        String requestBody = readJsonFromFile("login_failure.json");

        String reqImagePath = saveTextAsImage(requestBody, "LoginFailure_Request");
        if (reqImagePath != null) test.info("Request JSON").addScreenCaptureFromPath(reqImagePath);

        try {
            Response response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/user/login")
                    .then()
                    .statusCode(anyOf(equalTo(400), equalTo(401)))
                    .body("$", anyOf(
                            hasKey("error"),
                            hasKey("message"),
                            hasKey("detail")
                    ))
                    .extract().response();

            String resImagePath = saveTextAsImage(response.asPrettyString(), "LoginFailure_Response");
            if (resImagePath != null) test.info("Response JSON").addScreenCaptureFromPath(resImagePath);

            test.pass("Invalid login handled correctly with status: " + response.statusCode());

        } catch (AssertionError ae) {
            test.fail("Assertion failed: " + ae.getMessage());
            throw ae;
        } catch (Exception e) {
            test.fail("Login failure test execution failed: " + e.getMessage());
            throw e;
        }
    }

    @AfterSuite
    public void tearDown() {
        extent.flush();
    }
}
