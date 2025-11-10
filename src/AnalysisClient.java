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
     * Generate the HTML report file
     */
    private void generateHtmlReport(String outputPath, String pacingState,
                                    String recommendation, double latestLoad,
                                    double loadThreshold, String base64Image) 
                                    throws IOException {
        
        String html = loadHtmlTemplate()
            .replace("{{PACING_STATE}}", escapeHtml(pacingState))
            .replace("{{RECOMMENDATION_TEXT}}", escapeHtml(recommendation))
            .replace("{{LATEST_LOAD}}", String.format("%.1f", latestLoad))
            .replace("{{LOAD_THRESHOLD}}", String.format("%.1f", loadThreshold))
            .replace("{{BASE64_IMAGE_DATA}}", base64Image)
            .replace("{{GENERATION_DATE}}", LocalDate.now().toString());
        
        Files.writeString(Path.of(outputPath), html);
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
                    
                    <section class="mb-8 p-4 bg-red-50 border-l-4 border-red-500 rounded-lg">
                        <h2 class="text-xl font-semibold text-red-700 mb-2">{{PACING_STATE}}</h2>
                        <p class="text-lg text-red-900">{{RECOMMENDATION_TEXT}}</p>
                        <div class="mt-3 text-sm text-red-600">
                            Latest Load: <strong>{{LATEST_LOAD}}</strong> | 
                            Threshold: <strong>{{LOAD_THRESHOLD}}</strong>
                        </div>
                    </section>
                    
                    <section>
                        <h2 class="text-xl font-semibold text-gray-800 mb-4">Factor Contribution Visualization</h2>
                        <div class="bg-gray-100 p-4 rounded-lg">
                            <img src="data:image/png;base64,{{BASE64_IMAGE_DATA}}" 
                                 alt="Daily Factor Contribution Graph" 
                                 class="w-full h-auto rounded-md shadow-inner">
                        </div>
                        <p class="text-sm text-gray-500 mt-4">
                            The stacked bars represent the Total Negative Load (TNL). 
                            Protective factors are shown below the zero line, actively reducing daily load.
                        </p>
                    </section>
                    
                    <section class="mt-8">
                        <h3 class="text-lg font-semibold text-gray-800 mb-3">Visualization Key</h3>
                        <div class="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #FF8C42;"></span>
                                <span class="text-gray-600 font-medium">Stress (0-10)</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #43AA8B;"></span>
                                <span class="text-gray-600 font-medium">Cognitive Load</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #B22222;"></span>
                                <span class="text-gray-600 font-medium">Sleep Deficit Penalty</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #2D7DD2;"></span>
                                <span class="text-gray-600 font-medium">Positive Custom Factor</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: #F5E663; border: 1px solid #d4c05a;"></span>
                                <span class="text-gray-600 font-medium">Protective Factor</span>
                            </div>
                            <div class="flex items-center space-x-2">
                                <span class="color-box" style="background-color: transparent; border: 2px solid red;"></span>
                                <span class="text-gray-600 font-medium">Tic Level (Red Line)</span>
                            </div>
                        </div>
                    </section>
                    
                    <footer class="mt-8 pt-4 border-t text-center text-gray-500 text-sm">
                        <p>MyFlow Mental Health Fitness Tracker | Powered by Python ML Microservice</p>
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
