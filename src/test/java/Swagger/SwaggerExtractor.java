package Swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class SwaggerExtractor {

    private static final String[] BASE_PATHS = {
            "/api",   // old style
            "/user",  // new style
            ""        // fallback root
    };

    private static final String[] CANDIDATE_SUFFIXES = {
            "/v3/api-docs",   // OpenAPI 3 default
            "/swagger.json",  // Swagger 2 default
            "/openapi.json"   // Alternative
    };

    public static void main(String[] args) throws Exception {
        // 🔹 Root domain only
        String rootUrl = "https://fawfcakk57azargc4dxbsfhgqe0sjobd.lambda-url.eu-north-1.on.aws";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        String workingUrl = null;

        // 🔎 Try combinations of base path + suffix
        for (String base : BASE_PATHS) {
            for (String suffix : CANDIDATE_SUFFIXES) {
                String attemptUrl = rootUrl + base + suffix;
                System.out.println("➡ Trying: " + attemptUrl);

                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(attemptUrl).openConnection();
                    conn.setRequestMethod("GET");

                    int status = conn.getResponseCode();
                    if (status != 200) {
                        System.out.println("   ❌ Skipped (HTTP " + status + ")");
                        continue;
                    }

                    String contentType = conn.getContentType();
                    String responseBody;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        responseBody = reader.lines().collect(Collectors.joining("\n"));
                    }

                    if (contentType == null || !contentType.contains("application/json")) {
                        if (responseBody.trim().startsWith("{")) {
                            // JSON without correct header
                            root = mapper.readTree(responseBody);
                            workingUrl = attemptUrl;
                            break;
                        } else {
                            System.out.println("   ❌ Skipped (not JSON, got " + contentType + ")");
                            continue;
                        }
                    }

                    root = mapper.readTree(responseBody);
                    workingUrl = attemptUrl;
                    break; // ✅ Found valid JSON
                } catch (Exception e) {
                    System.out.println("   ❌ Error: " + e.getMessage());
                }
            }
            if (root != null) break; // Stop once a valid spec is found
        }

        if (root == null) {
            throw new RuntimeException("❌ Could not find a valid Swagger/OpenAPI JSON under " + rootUrl);
        }

        System.out.println("\n✅ Using Swagger JSON from: " + workingUrl);

        JsonNode paths = root.get("paths");
        if (paths == null) {
            System.out.println("⚠️ No 'paths' section found in Swagger file");
            return;
        }

        // 🔄 Iterate over all endpoints
        Iterator<Map.Entry<String, JsonNode>> pathIter = paths.fields();
        while (pathIter.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIter.next();
            String path = pathEntry.getKey();
            JsonNode methods = pathEntry.getValue();

            Iterator<Map.Entry<String, JsonNode>> methodIter = methods.fields();
            while (methodIter.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodIter.next();
                String method = methodEntry.getKey().toUpperCase();
                JsonNode details = methodEntry.getValue();

                System.out.println("\n🔹 " + method + " " + path);

                // Request body example
                if (details.has("requestBody")) {
                    JsonNode requestExample = details.path("requestBody")
                            .path("content")
                            .path("application/json")
                            .path("example");

                    if (!requestExample.isMissingNode()) {
                        System.out.println("   📥 Request JSON:");
                        System.out.println(requestExample.toPrettyString());
                    } else {
                        System.out.println("   📥 Request JSON: (No example provided)");
                    }
                }

                // Response examples
                JsonNode responses = details.get("responses");
                if (responses != null) {
                    Iterator<Map.Entry<String, JsonNode>> respIter = responses.fields();
                    while (respIter.hasNext()) {
                        Map.Entry<String, JsonNode> respEntry = respIter.next();
                        String code = respEntry.getKey();
                        JsonNode resp = respEntry.getValue();

                        JsonNode example = resp.path("content")
                                .path("application/json")
                                .path("example");

                        if (!example.isMissingNode()) {
                            System.out.println("   📤 Response " + code + ":");
                            System.out.println(example.toPrettyString());
                        } else {
                            System.out.println("   📤 Response " + code + ": (No example provided)");
                        }
                    }
                }
            }
        }
    }
}
