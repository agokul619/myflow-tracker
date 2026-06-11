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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Base64;

public class AnalysisClient {
    
    private static final String REPORTS_DIR = "reports/";
    
    public AnalysisClient() {
        ensureReportsDirectoryExists();
    }

    private String callApi(String jsonPayload, String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000" + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("API call to " + endpoint + " failed: " + response.body());
        }
        return response.body();
    }
    
    /**
     * Main method to run analysis and return the path to the generated HTML report
     */
    public String runAnalysis(String jsonPayload, int days) throws IOException, InterruptedException {
        System.out.println("\n=== Starting Analysis Request ===");
        
        // 1. Call all three endpoints
        String vizResponse = callApi(jsonPayload, "/api/visualization");
        String protResponse = callApi(jsonPayload, "/api/protective-factors");
        String sleepResponse = callApi(jsonPayload, "/api/sleep-analysis");
        
        JSONObject vizJson = new JSONObject(vizResponse);
        JSONObject protJson = new JSONObject(protResponse);
        JSONObject sleepJson = new JSONObject(sleepResponse);
        
        // 2. Extract Visualization & ML Regression Data
        String base64Image = vizJson.getString("graph_image_base64");
        String recommendation = vizJson.getString("pacing_recommendation");
        String pacingState = vizJson.getString("pacing_state");
        double latestLoad = vizJson.optDouble("latest_load", 0.0);
        double loadThreshold = vizJson.optDouble("load_threshold", 0.0);
        String regressionInsight = vizJson.optString("regression_insight", "");
        
        // 🔮 ML Forecast Data
        String forecastSummary = vizJson.optString("forecast_summary", "");
        String forecastEquation = vizJson.optString("forecast_equation", "");
        String forecastEvaluation = vizJson.optString("forecast_evaluation", "");
        
        // 3. Extract Protective Factors Data
        String insightMessage = protJson.optString("insight_message", "");
        String bestFactorName = protJson.optString("best_factor_name", "");
        double bestFactorReduction = protJson.optDouble("best_factor_reduction", 0.0);
        double bestFactorAvgWith = protJson.optDouble("best_factor_avg_tics_with", 0.0);
        double bestFactorAvgWithout = protJson.optDouble("best_factor_avg_tics_without", 0.0);
        JSONArray allFactors = protJson.optJSONArray("all_factors");
        JSONArray top3Days = protJson.optJSONArray("top_3_best_days");
        
        // 4. Extract Sleep Analysis Data
        double avgSleep = sleepJson.optDouble("avg_sleep_hours", 0.0);
        double sleepCorrelation = sleepJson.optDouble("correlation_coefficient", 0.0);
        double percentDiff = sleepJson.optDouble("percent_difference", 0.0);
        double avgGoodSleepTics = sleepJson.optDouble("avg_tics_good_sleep", 0.0);
        double avgBadSleepTics = sleepJson.optDouble("avg_tics_bad_sleep", 0.0);
        String sleepInsight = sleepJson.optString("insight_message", "");
        
        // 5. Generate Report
        String reportFileName = generateReportFileName(days);
        String reportPath = REPORTS_DIR + reportFileName;
        
        generateHtmlReport(reportPath, pacingState, recommendation,
                latestLoad, loadThreshold, base64Image, regressionInsight,
                forecastSummary, forecastEquation, forecastEvaluation,
                insightMessage, bestFactorName, bestFactorReduction,
                bestFactorAvgWith, bestFactorAvgWithout, allFactors, top3Days,
                avgSleep, sleepCorrelation, percentDiff,
                avgGoodSleepTics, avgBadSleepTics, sleepInsight);
                
        System.out.println("✓ Report generated: " + reportPath);
        System.out.println("=== Analysis Complete ===\n");
        
        return reportPath;
    }
    
    private void generateHtmlReport(String outputPath, String pacingState,
            String recommendation, double latestLoad, double loadThreshold,
            String base64Image, String regressionInsight,
            String forecastSummary, String forecastEquation, String forecastEvaluation, 
            String insightMessage, String bestFactorName, double bestFactorReduction,
            double bestFactorAvgWith, double bestFactorAvgWithout, 
            JSONArray allFactors, JSONArray top3Days, 
            double avgSleep, double sleepCorrelation, double percentDiff,
            double avgGoodSleepTics, double avgBadSleepTics, String sleepInsight) throws IOException { 

        String[] colors = getColorScheme(pacingState);
        String formattedRecommendation = convertMarkdownToHtml(recommendation);
        String imageFileName = outputPath.replace(".html", "_graph.png");
        saveBase64ImageToFile(base64Image, imageFileName);
        String imageUrl = new File(imageFileName).toURI().toString();

        // Build protective factors ranking rows
        StringBuilder factorRows = new StringBuilder();
        String[] medals = {"🥇", "🥈", "🥉", "4.", "5."};
        if (allFactors != null) {
            for (int i = 0; i < allFactors.length(); i++) {
                JSONObject f = allFactors.getJSONObject(i);
                String medal = i < medals.length ? medals[i] : (i+1) + ".";
                String color = f.optDouble("tic_reduction_pct", 0) >= 20 ? "#22c55e" :
                               f.optDouble("tic_reduction_pct", 0) >= 10 ? "#f59e0b" : "#f97316";
                factorRows.append(String.format(
                    "<div style='display:flex;align-items:center;gap:12px;padding:12px;border-bottom:1px solid #f0f0f0;'>" +
                    "<span style='font-size:1.2rem;'>%s</span>" +
                    "<div style='flex:1;'>" +
                    "<span style='font-weight:600;'>%s</span>" +
                    "<span style='margin-left:8px;background:%s;color:white;padding:2px 8px;border-radius:12px;font-size:0.75rem;'>%.0f%% reduction</span>" +
                    "</div>" +
                    "<span style='color:#888;font-size:0.85rem;'>Used %dx • Avg %.1f tics with • %.1f without</span>" +
                    "</div>",
                    medal,
                    escapeHtml(f.optString("name", "")),
                    color,
                    f.optDouble("tic_reduction_pct", 0),
                    f.optInt("times_used", 0),
                    f.optDouble("avg_tics_with", 0),
                    f.optDouble("avg_tics_without", 0)
                ));
            }
        }

        // Build best days cards
        StringBuilder bestDayCards = new StringBuilder();
        if (top3Days != null) {
            for (int i = 0; i < top3Days.length(); i++) {
                JSONObject d = top3Days.getJSONObject(i);
                String border = i == 0 ? "2px solid #f59e0b" : "1px solid #e5e7eb";
                JSONArray factors = d.optJSONArray("factors");
                String factorStr = "";
                if (factors != null && factors.length() > 0) {
                    factorStr = "✨ " + factors.getString(0);
                }
                bestDayCards.append(String.format(
                    "<div style='border:%s;border-radius:12px;padding:16px;text-align:center;'>" +
                    "<div style='font-size:0.85rem;color:#888;'>%s</div>" +
                    "<div style='font-size:1.8rem;font-weight:700;color:#22c55e;'>%.1f</div>" +
                    "<div style='font-size:0.8rem;color:#888;'>TNL Score • %d tics</div>" +
                    "<div style='font-size:0.85rem;color:#6366f1;margin-top:4px;'>%s</div>" +
                    "</div>",
                    border,
                    escapeHtml(d.optString("date", "")),
                    d.optDouble("tnl", 0),
                    d.optInt("tics", 0),
                    escapeHtml(factorStr)
                ));
            }
        }

        String html = loadHtmlTemplate()
            .replace("{{STATUS_BG}}", colors[0])
            .replace("{{STATUS_BORDER}}", colors[1])
            .replace("{{STATUS_TEXT}}", colors[2])
            .replace("{{STATUS_DARK}}", colors[3])
            .replace("{{PACING_STATE}}", escapeHtml(pacingState))
            .replace("{{RECOMMENDATION_TEXT}}", formattedRecommendation)
            .replace("{{LATEST_LOAD}}", String.format("%.1f", latestLoad))
            .replace("{{LOAD_THRESHOLD}}", String.format("%.1f", loadThreshold))
            .replace("{{IMAGE_URL}}", imageUrl)
            .replace("{{REGRESSION_INSIGHT}}", escapeHtml(regressionInsight))
            .replace("{{FORECAST_SUMMARY}}", escapeHtml(forecastSummary)) 
            .replace("{{FORECAST_EQUATION}}", escapeHtml(forecastEquation))
            .replace("{{FORECAST_EVALUATION}}", escapeHtml(forecastEvaluation))
            .replace("{{BEST_FACTOR_NAME}}", escapeHtml(bestFactorName))
            .replace("{{BEST_FACTOR_REDUCTION}}", String.format("%.0f", bestFactorReduction))
            .replace("{{BEST_FACTOR_AVG_WITH}}", String.format("%.1f", bestFactorAvgWith))
            .replace("{{BEST_FACTOR_AVG_WITHOUT}}", String.format("%.1f", bestFactorAvgWithout))
            .replace("{{FACTOR_ROWS}}", factorRows.toString())
            .replace("{{BEST_DAY_CARDS}}", bestDayCards.toString())
            .replace("{{AVG_SLEEP}}", String.format("%.1f", avgSleep))
            .replace("{{SLEEP_CORRELATION}}", String.format("%.2f", sleepCorrelation))
            .replace("{{SLEEP_PERCENT_DIFF}}", String.format("%.0f", percentDiff))
            .replace("{{GOOD_SLEEP_TICS}}", String.format("%.1f", avgGoodSleepTics))
            .replace("{{BAD_SLEEP_TICS}}", String.format("%.1f", avgBadSleepTics))
            .replace("{{SLEEP_INSIGHT}}", escapeHtml(sleepInsight))
            .replace("{{GENERATION_DATE}}", LocalDate.now().toString());

        Files.writeString(Path.of(outputPath), html);
    }
    
    private void saveBase64ImageToFile(String base64Data, String outputPath) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        Files.write(Path.of(outputPath), imageBytes);
    }

    private String[] getColorScheme(String pacingState) {
        if (pacingState.contains("GREEN LIGHT")) {
            return new String[]{"bg-green-50", "border-green-500", "text-green-700", "text-green-900"};
        } else if (pacingState.contains("WARNING") || pacingState.contains("YELLOW") || pacingState.contains("SPIKE")) {
            return new String[]{"bg-yellow-50", "border-yellow-500", "text-yellow-700", "text-yellow-900"};
        } else {
            return new String[]{"bg-red-50", "border-red-500", "text-red-700", "text-red-900"};
        }
    }

    private String convertMarkdownToHtml(String text) {
        return text.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
    }

    private String generateReportFileName(int days) {
        String timestamp = LocalDate.now().toString();
        return String.format("myflow_analysis_%ddays_%s.html", days, timestamp);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

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
                </style>
            </head>
            <body class="p-4 sm:p-8">
                <div class="max-w-4xl mx-auto bg-white shadow-2xl rounded-xl p-6 sm:p-8">
                    <header class="border-b pb-4 mb-6">
                        <h1 class="text-3xl font-bold text-[#43AA8B]">📊 Your MyFlow Wellness Report</h1>
                        <p class="text-gray-500 mt-1">Generated: {{GENERATION_DATE}}</p>
                    </header>
    
                    <section class="mb-6 p-4 bg-blue-50 border-l-4 border-blue-400 rounded-lg">
                        <h3 class="font-semibold text-blue-900 mb-2">Understanding Your Results</h3>
                        <p class="text-sm text-blue-800 mb-1"><strong>Your Current Load:</strong> {{LATEST_LOAD}}</p>
                        <p class="text-sm text-blue-800"><strong>Your Threshold:</strong> {{LOAD_THRESHOLD}}</p>
                    </section>
    
                    <section class="mb-8 p-4 {{STATUS_BG}} border-l-4 {{STATUS_BORDER}} rounded-lg">
                        <h2 class="text-xl font-semibold {{STATUS_TEXT}} mb-2">{{PACING_STATE}}</h2>
                        <p class="text-lg {{STATUS_DARK}}">{{RECOMMENDATION_TEXT}}</p>
                        <div class="mt-3 text-sm {{STATUS_TEXT}}">
                            Latest Load: <strong>{{LATEST_LOAD}}</strong> | Threshold: <strong>{{LOAD_THRESHOLD}}</strong>
                        </div>
                    </section>
    
                    <section class="mb-8">
                        <h2 class="text-xl font-semibold text-gray-800 mb-4">📈 What's Affecting Your Tics?</h2>
                        <div class="bg-gray-100 p-4 rounded-lg">
                            <img src="{{IMAGE_URL}}" alt="Daily Factor Contribution Graph" class="w-full h-auto rounded-md">
                        </div>
                    </section>
    
                    <section class="mb-8 p-4 bg-indigo-50 border-l-4 border-indigo-500 rounded-lg">
                        <h2 class="text-xl font-semibold text-indigo-700 mb-2">📈 Linear Regression Finding</h2>
                        <p class="text-lg text-indigo-900">{{REGRESSION_INSIGHT}}</p>
                        <p class="text-sm text-indigo-600 mt-2">Your tracked factors have a measurable, statistically-defined relationship with your tic count.</p>
                    </section>
                    
                    <section class="mb-8 p-6 bg-emerald-50 border-l-4 border-emerald-500 rounded-lg shadow-sm">
                        <h2 class="text-xl font-bold text-emerald-800 mb-2">🔮 Tomorrow's ML Forecast</h2>
                        <p class="text-sm text-emerald-700 mb-4">Using a multiple linear regression model trained on your lagged dataset (predicting tomorrow based on today).</p>
                        
                        <div style="background-color: white; padding: 16px; border-radius: 8px; border: 1px solid #a7f3d0; margin-bottom: 12px;">
                            <h3 class="text-2xl font-bold text-emerald-600">{{FORECAST_SUMMARY}}</h3>
                        </div>
                        
                        <p class="text-xs text-emerald-800 font-mono bg-emerald-100 p-2 rounded inline-block mb-3">{{FORECAST_EQUATION}}</p>
                        <p class="text-sm text-emerald-700"><strong>Model Evaluation:</strong> {{FORECAST_EVALUATION}}</p>
                    </section>
    
                    <section class="mb-8 p-4 bg-purple-50 border-l-4 border-purple-500 rounded-lg">
                        <h2 class="text-xl font-semibold text-purple-700 mb-4">🛡️ What's Working FOR YOU</h2>
                        <p class="text-sm text-purple-600 mb-4">These are YOUR best protective factors based on YOUR data.</p>
                        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px;">
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;">🏆</div>
                                <div style="font-weight:600;font-size:0.9rem;">{{BEST_FACTOR_NAME}}</div>
                                <div style="font-size:0.75rem;color:#888;">Your #1 Factor</div>
                            </div>
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#22c55e;">{{BEST_FACTOR_REDUCTION}}%</div>
                                <div style="font-size:0.75rem;color:#888;">Tic Reduction</div>
                            </div>
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#3b82f6;">{{BEST_FACTOR_AVG_WITH}}</div>
                                <div style="font-size:0.75rem;color:#888;">Avg Tics WITH it</div>
                            </div>
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#ef4444;">{{BEST_FACTOR_AVG_WITHOUT}}</div>
                                <div style="font-size:0.75rem;color:#888;">Avg Tics WITHOUT</div>
                            </div>
                        </div>
                        <div style="background:white;border-radius:12px;overflow:hidden;">
                            {{FACTOR_ROWS}}
                        </div>
                    </section>
    
                    <section class="mb-8">
                        <h2 class="text-xl font-semibold text-gray-800 mb-2">🌟 Your Best Days (Lowest TNL)</h2>
                        <p class="text-sm text-gray-500 mb-4">These days had the lowest Total Negative Load.</p>
                        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">
                            {{BEST_DAY_CARDS}}
                        </div>
                    </section>
    
                    <section class="mb-8 p-4 bg-blue-50 border-l-4 border-blue-400 rounded-lg">
                        <h2 class="text-xl font-semibold text-blue-800 mb-2">😴 How Sleep Affects Your Tics</h2>
                        <p class="text-blue-700 mb-4">{{SLEEP_INSIGHT}}</p>
                        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:16px;">
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#3b82f6;">{{AVG_SLEEP}}</div>
                                <div style="font-size:0.75rem;color:#888;">Hours of Sleep Per Night</div>
                            </div>
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#6366f1;">{{SLEEP_CORRELATION}}</div>
                                <div style="font-size:0.75rem;color:#888;">Sleep-Tic Connection</div>
                            </div>
                            <div style="background:white;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#22c55e;">{{SLEEP_PERCENT_DIFF}}%</div>
                                <div style="font-size:0.75rem;color:#888;">Fewer Tics With Good Sleep</div>
                            </div>
                        </div>
                        <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px;">
                            <div style="background:#fee2e2;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#ef4444;">{{BAD_SLEEP_TICS}}</div>
                                <div style="font-size:0.75rem;color:#991b1b;">Average Tics (Bad Sleep)</div>
                            </div>
                            <div style="background:#dcfce7;border-radius:12px;padding:16px;text-align:center;">
                                <div style="font-size:1.5rem;font-weight:700;color:#16a34a;">{{GOOD_SLEEP_TICS}}</div>
                                <div style="font-size:0.75rem;color:#166534;">Average Tics (Good Sleep)</div>
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
}