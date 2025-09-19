package Swagger;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwaggerJsonFromInitJS {

    public static void main(String[] args) throws Exception {
        String initJsUrl = "https://fawfcakk57azargc4dxbsfhgqe0sjobd.lambda-url.eu-north-1.on.aws/api/swagger-ui-init.js";

        // Fetch JS file
        HttpURLConnection conn = (HttpURLConnection) new URL(initJsUrl).openConnection();
        conn.setRequestMethod("GET");
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("Failed to fetch init JS. HTTP code: " + status);
        }

        StringBuilder jsContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsContent.append(line).append("\n");
            }
        }

        // Find the JSON URL
        Pattern pattern = Pattern.compile("url\\s*:\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(jsContent.toString());

        if (!matcher.find()) {
            throw new RuntimeException("Could not find Swagger JSON URL in init JS file.");
        }

        String swaggerJsonUrl = matcher.group(1);
        System.out.println("✅ Found Swagger JSON URL: " + swaggerJsonUrl);

        // Make absolute if relative
        if (swaggerJsonUrl.startsWith("/")) {
            URL base = new URL(initJsUrl);
            swaggerJsonUrl = base.getProtocol() + "://" + base.getHost() + swaggerJsonUrl;
        }

        // Fetch Swagger JSON
        HttpURLConnection jsonConn = (HttpURLConnection) new URL(swaggerJsonUrl).openConnection();
        jsonConn.setRequestMethod("GET");
        if (jsonConn.getResponseCode() != 200) {
            throw new RuntimeException("Failed to fetch Swagger JSON. HTTP code: " + jsonConn.getResponseCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonConn.getInputStream());
        System.out.println("✅ Successfully fetched Swagger JSON!");
        System.out.println("Number of paths: " + root.path("paths").size());
    }
}
