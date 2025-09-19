package API;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.time.Duration;

public class MailinatorOTPTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeClass
    public void setUp() {
        // Auto-detect and download the correct ChromeDriver for your Chrome (140.x)
        WebDriverManager.chromedriver().setup();

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().window().maximize();
    }




    @Test
    public void testFetchOtpFromMailinator() throws Exception {
        // 1. Navigate to Mailinator public inbox
        driver.get("https://www.mailinator.com/v4/public/inboxes.jsp");

        // 2. Enter inbox name (use your public inbox name, e.g. test021)
        WebElement inboxField = wait.until(ExpectedConditions.elementToBeClickable(By.id("inbox_field")));
        inboxField.clear();
        inboxField.sendKeys("test021"); 
        Thread.sleep(1000);
        Robot R = new Robot();
        R.keyPress(KeyEvent.VK_ENTER);
        R.keyRelease(KeyEvent.VK_ENTER);// <-- change this to your email prefix
        

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement emailRow = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//td[contains(text(),'Email Verification OTP')]")
        ));
        emailRow.click();
        Thread.sleep(1000);

     // Wait for the email row and click it
       

        // ✅ Click on the "Raw" tab (adjust locator if needed)
        WebElement rawTab = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[@id='pills-raw-tab']")
        ));
        rawTab.click();

        // ✅ Locate the raw email content area
        WebElement rawContent = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[@id='pills-raw-content']/pre")   // check Mailinator, sometimes it's <pre id="rawmsg_body">
        ));

        // ✅ Get all the text
        String fullRawText = rawContent.getText();
        System.out.println("Full RAW Email Content:\n" + fullRawText);

        // (No need to switchTo().frame since RAW is plain text)

        // Store OTP for later use (reset password API)
        // In practice, you can save it to a static variable, file, or Extent report
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
//            driver.quit();
        }
    }
}
