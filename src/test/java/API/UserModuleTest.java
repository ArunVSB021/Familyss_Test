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

public class UserModuleTest {

    private static ExtentReports extent;
    private static ExtentTest test;
    private static String screenshotDir;
    private static String jsonDir;

    @BeforeSuite
    public void setupReport() {
        String reportPath = Paths.get("reports", "UserModuleReport.html").toString();
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

        extent = new ExtentReports();
        extent.attachReporter(spark);

        extent.setSystemInfo("Project", "Familyss");
        extent.setSystemInfo("Tester", "Arun V");
        extent.setSystemInfo("Environment", "QA");

        screenshotDir = Paths.get("reports", "screenshots").toString();
        new File(screenshotDir).mkdirs();

        jsonDir = Paths.get("testdata").toString();
        new File(jsonDir).mkdirs();
    }

    @BeforeMethod
    public void setUpTest(Method method) {
        test = extent.createTest(method.getName());
        RestAssured.baseURI = "https://fawfcakk57azargc4dxbsfhgqe0sjobd.lambda-url.eu-north-1.on.aws";
    }

    // ---------------- Utilities ----------------
    private String saveTextAsImage(String text, String fileName) {
        try {
            int width = 1000, lineHeight = 20;
            String[] lines = text.split("\n");
            int height = Math.max(200, lines.length * lineHeight + 40);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK); g.setFont(new Font("Consolas", Font.PLAIN, 14));

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

    private String readJsonFromFile(String fileName) {
        try {
            String basePath = System.getProperty("user.dir");
            String filePath = Paths.get(basePath, "testdata", fileName).toString();
            return Files.lines(Paths.get(filePath)).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            test.fail("Failed to read JSON file: " + fileName + " -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ------------------- Tests -------------------

    @Test
    public void testLoginSuccess() {
        String requestBody = readJsonFromFile("login_success.json");
        String reqImagePath = saveTextAsImage(requestBody, "LoginSuccess_Request");
        if (reqImagePath != null) test.info("Request JSON").addScreenCaptureFromPath(reqImagePath);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/user/login")
                .then()
                .statusCode(200)
                .body("$", anyOf(hasKey("token"), hasKey("jwt"), hasKey("accessToken")))
                .extract().response();

        String resImagePath = saveTextAsImage(response.asPrettyString(), "LoginSuccess_Response");
        if (resImagePath != null) test.info("Response JSON").addScreenCaptureFromPath(resImagePath);

        test.pass("Login Success API passed");
    }

    @Test
    public void testRegisterUser() {
        String requestBody = readJsonFromFile("register_user.json");
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/user/register")
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "RegisterUser_Response");
    }

    @Test
    public void testForgotPassword() {
        String requestBody = readJsonFromFile("forgot_password.json");
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/user/forgot-password")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "ForgotPassword_Response");
    }

    @Test
    public void testResetPassword() {
        String requestBody = readJsonFromFile("reset_password.json");
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/user/reset-password")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "ResetPassword_Response");
    }

    @Test
    public void testVerifyOtp() {
        String requestBody = readJsonFromFile("verify_otp.json");
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/user/verify-otp")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "VerifyOtp_Response");
    }

    @Test
    public void testLookupUser() {
        Response response = RestAssured.given()
                .queryParam("phone", "9876543210")
                .when()
                .get("/user/lookup")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "LookupUser_Response");
    }

    @Test
    public void testDeleteUser() {
        Response response = RestAssured.given()
                .when()
                .delete("/user/123") // replace with dynamic userId
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(403)))
                .extract().response();

        saveTextAsImage(response.asPrettyString(), "DeleteUser_Response");
    }

    @AfterSuite
    public void tearDown() {
        extent.flush();
    }
}
