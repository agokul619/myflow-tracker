// analysis/AnalysisClient.java
// Handles communication with the Python microservice and report generation

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.json.JSONObject;
import java.util.Base64;

public class AnalysisClient {
    
    private static final String PYTHON_API_URL = "http://127.0.0.1:5000/api/visualization";
    private static final String REPORTS_DIR = "reports/";
    
    public AnalysisClient() {
        ensureReportsDirectoryExists();
    }
    
    /**
     * Main method to run analysis and return the path to the generated HTML report
     * @param jsonPayload The JSON string containing daily log data
     * @param days Number of days being analyzed (for report naming)
     * @return Path to the generated HTML report file
     */
    public String runAnalysis(String jsonPayload, int days) throws IOException, InterruptedException {
        System.out.println("\n=== Starting Analysis Request ===");
        System.out.println("Sending " + jsonPayload.length() + " bytes to Python service...");
        
        // 1. Call Python API
        String responseBody = callPythonApi(jsonPayload);
        System.out.println("✓ Response received from Python service");
        
        // 2. Parse response
        JSONObject json = new JSONObject(responseBody);
        
        if (!json.optBoolean("success", false)) {
            throw new IOException("Python service returned error: " + 
                                json.optString("error", "Unknown error"));
        }
        
        // 3. Extract data
        String base64Image = json.getString("graph_image_base64");
        String recommendation = json.getString("pacing_recommendation");
        String pacingState = json.getString("pacing_state");
        double latestLoad = json.optDouble("latest_load", 0.0);
        double loadThreshold = json.optDouble("load_threshold", 0.0);
        
        // 4. Generate HTML report
        String reportFileName = generateReportFileName(days);
        String reportPath = REPORTS_DIR + reportFileName;
        
        generateHtmlReport(reportPath, pacingState, recommendation, 
                          latestLoad, loadThreshold, base64Image);
        
        System.out.println("✓ Report generated: " + reportPath);
        System.out.println("=== Analysis Complete ===\n");
        
        return reportPath;
    }
    
    /**
     * Call the Python microservice API
     */
    private String callPythonApi(String jsonPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        
        HttpResponse<String> response = client.send(request, 
                                                    HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException(
                "API call failed with status " + response.statusCode() + 
                ": " + response.body()
            );
        }
        
        return response.body();
    }
    
    /**
     * Determine color scheme based on pacing state
     */
    private String[] getColorScheme(String pacingState) {
        // Returns: [bg-color, border-color, text-color, text-dark-color]
        if (pacingState.contains("GREEN LIGHT")) {
            return new String[]{"bg-green-50", "border-green-500", "text-green-700", "text-green-900"};
        } else if (pacingState.contains("WARNING") || pacingState.contains("YELLOW")) {
            return new String[]{"bg-yellow-50", "border-yellow-500", "text-yellow-700", "text-yellow-900"};
        } else {
            return new String[]{"bg-red-50", "border-red-500", "text-red-700", "text-red-900"};
        }
    }
    
    /**
     * Convert markdown bold to HTML bold
     */
    private String convertMarkdownToHtml(String text) {
        // Replace **text** with <strong>text</strong>
        return text.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
    }
    
    /**
     * Generate the HTML report file
     */
    private void generateHtmlReport(String outputPath, String pacingState,
            String recommendation, double latestLoad,
            double loadThreshold, String base64Image) 
            throws IOException {

// Get dynamic colors based on status
String[] colors = getColorScheme(pacingState);

// Convert markdown formatting to HTML
String formattedRecommendation = convertMarkdownToHtml(recommendation);

// ✅ SAVE IMAGE AS SEPARATE FILE
String imageFileName = outputPath.replace(".html", "_graph.png");
saveBase64ImageToFile(base64Image, imageFileName);

// ✅ USE file:// URL instead of base64
String imageUrl = new File(imageFileName).toURI().toString();

String html = loadHtmlTemplate()
.replace("{{STATUS_BG}}", colors[0])
.replace("{{STATUS_BORDER}}", colors[1])
.replace("{{STATUS_TEXT}}", colors[2])
.replace("{{STATUS_DARK}}", colors[3])
.replace("{{PACING_STATE}}", escapeHtml(pacingState))
.replace("{{RECOMMENDATION_TEXT}}", formattedRecommendation)
.replace("{{LATEST_LOAD}}", String.format("%.1f", latestLoad))
.replace("{{LOAD_THRESHOLD}}", String.format("%.1f", loadThreshold))
.replace("{{IMAGE_URL}}", imageUrl)  // ✅ Changed from base64
.replace("{{GENERATION_DATE}}", LocalDate.now().toString());

Files.writeString(Path.of(outputPath), html);
}

//✅ ADD THIS NEW METHOD
private void saveBase64ImageToFile(String base64Data, String outputPath) throws IOException {
byte[] imageBytes = Base64.getDecoder().decode(base64Data);
Files.write(Path.of(outputPath), imageBytes);
}
    
    /**
     * HTML template for the report
     */
    private String loadHtmlTemplate() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MyFlow Analysis Report</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
                    body { font-family: 'Inter', sans-serif; background-color: #f8fafc; }
                    .color-box { width: 1rem; height: 1rem; border-radius: 9999px; }
                </style>
            </head>
            <body class="p-4 sm:p-8">
                <div class="max-w-4xl mx-auto bg-white shadow-2xl rounded-xl p-6 sm:p-8">
                    <header class="border-b pb-4 mb-6">
                        <h1 class="text-3xl font-bold text-[#43AA8B]">MyFlow Analysis Report</h1>
                        <p class="text-gray-500 mt-1">Generated: {{GENERATION_DATE}}</p>
                    </header>
                    
                    <!-- What This Means Section -->
                    <section class="mb-6 p-4 bg-blue-50 border-l-4 border-blue-400 rounded-lg">
                        <h3 class="font-semibold text-blue-900 mb-2">📊 Understanding Your Results</h3>
                        <p class="text-sm text-blue-800 mb-1">
                            <strong>Your Current Load:</strong> {{LATEST_LOAD}} - This is your total stress level based on all tracked factors
                        </p>
                        <p class="text-sm text-blue-800">
                            <strong>Your Threshold:</strong> {{LOAD_THRESHOLD}} - When you go above this, it's time to take action
                        </p>
                    </section>
                    
                    <!-- Status Section with Dynamic Colors -->
                    <section class="mb-8 p-4 {{STATUS_BG}} border-l-4 {{STATUS_BORDER}} rounded-lg">
                        <h2 class="text-xl font-semibold {{STATUS_TEXT}} mb-2">{{PACING_STATE}}</h2>
                        <p class="text-lg {{STATUS_DARK}}">{{RECOMMENDATION_TEXT}}</p>
                        <div class="mt-3 text-sm {{STATUS_TEXT}}">
                            Latest Load: <strong>{{LATEST_LOAD}}</strong> | 
                            Threshold: <strong>{{LOAD_THRESHOLD}}</strong>
                        </div>
                    </section>
                    
                    <section>
                        <h2 class="text-xl font-semibold text-gray-800 mb-4">What's Affecting You</h2>
                        <div class="bg-gray-100 p-4 rounded-lg">
                            <img src="{{IMAGE_URL}}"
                                 alt="Daily Factor Contribution Graph" 
                                 class="w-full h-auto rounded-md shadow-inner">
                        </div>
                        <p class="text-sm text-gray-600 mt-4">
                            <strong>How to read this chart:</strong> Bars going up show things that increase your stress. 
                            Bars going down (yellow) show helpful activities that reduce your load.
                        </p>
                    </section>
                    
                    <section class="mt-8">
                        <h3 class="text-lg font-semibold text-gray-800 mb-3">Chart Color Guide</h3>
                        <div class="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #FF8C42;"></span>
                                <span class="text-gray-600 font-medium">Stress Level (0-10)</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #43AA8B;"></span>
                                <span class="text-gray-600 font-medium">Study & Work Time</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #2D7DD2;"></span>
                                <span class="text-gray-600 font-medium">Stressful Events</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #F5E663; border: 1px solid #d4c05a;"></span>
                                <span class="text-gray-600 font-medium">Helpful Activities</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: transparent; border: 2px solid #EE4266;"></span>
                                <span class="text-gray-600 font-medium">Your Tic Count (Red Line)</span>
                            </div>
                        </div>
                    </section>
                    
                    <footer class="mt-8 pt-4 border-t text-center text-gray-500 text-sm">
                        <p>MyFlow Mental Health Tracker | Powered by Python AI</p>
                    </footer>
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * Generate report filename based on analysis range
     */
    private String generateReportFileName(int days) {
        String timestamp = LocalDate.now().toString();
        return String.format("myflow_analysis_%ddays_%s.html", days, timestamp);
    }
    
    /**
     * Simple HTML escaping
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Ensure reports directory exists
     */
    private void ensureReportsDirectoryExists() {
        Path reportsPath = Path.of(REPORTS_DIR);
        if (!Files.exists(reportsPath)) {
            try {
                Files.createDirectories(reportsPath);
                System.out.println("Created reports directory: " + REPORTS_DIR);
            } catch (IOException e) {
                System.err.println("Error creating reports directory: " + e.getMessage());
            }
        }
    }
}