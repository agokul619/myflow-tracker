// DiagnosticsPanel.java
// Panel for running diagnostics and displaying analysis results

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

public class DiagnosticsPanel extends JPanel {
    
    private Consumer<Integer> runDiagnosticsCallback;
    private JComboBox<String> rangeCombo;
    private JButton runButton;
    private JEditorPane resultsPane;
    private JLabel statusLabel;
    
    public DiagnosticsPanel(Consumer<Integer> runDiagnosticsCallback) {
        this.runDiagnosticsCallback = runDiagnosticsCallback;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Top control panel
        add(createControlPanel(), BorderLayout.NORTH);
        
        // Center results display
        add(createResultsPanel(), BorderLayout.CENTER);
        
        // Bottom status
        statusLabel = new JLabel("Ready to analyze. Select a range and click 'Run Diagnostics'.");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createSectionBorder("Analysis Settings"));
        
        // Instructions
        JLabel instructions = new JLabel(
            "<html><b>Run statistical analysis</b> on your data to receive adaptive pacing recommendations. " +
            "Requires at least 7 days of data for baseline calculation.</html>"
        );
        instructions.setFont(new Font("Arial", Font.PLAIN, 13));
        panel.add(instructions);
        panel.add(Box.createVerticalStrut(15));
        
        // Range selector and button
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        JLabel rangeLabel = new JLabel("Analysis Range:");
        rangeLabel.setFont(new Font("Arial", Font.BOLD, 13));
        controlRow.add(rangeLabel);
        
        String[] ranges = {
            "Last 7 Days",
            "Last 14 Days",
            "Last 30 Days",
            "Last 60 Days",
            "Last 90 Days"
        };
        rangeCombo = new JComboBox<>(ranges);
        rangeCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        rangeCombo.setPreferredSize(new Dimension(150, 30));
        controlRow.add(rangeCombo);
        
        controlRow.add(Box.createHorizontalStrut(20));
        
        runButton = new JButton("📊 Run Diagnostics");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.setBackground(new Color(238, 66, 102)); // Red/pink from design
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.setPreferredSize(new Dimension(180, 35));
        runButton.addActionListener(e -> handleRunDiagnostics());
        controlRow.add(runButton);
        
        panel.add(controlRow);
        
        return panel;
    }
    
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createSectionBorder("Analysis Results"));
        
        // HTML viewer for results
        resultsPane = new JEditorPane();
        resultsPane.setContentType("text/html");
        resultsPane.setEditable(false);
        resultsPane.setFont(new Font("Arial", Font.PLAIN, 13));
        
        // Enable hyperlink clicking
        resultsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openInBrowser(e.getURL().toString());
            }
        });
        
        // Initial placeholder content
        displayPlaceholder();
        
        JScrollPane scrollPane = new JScrollPane(resultsPane);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void displayPlaceholder() {
        String placeholder = 
            "<html><body style='font-family: Arial; padding: 20px; color: #666;'>" +
            "<h2 style='color: #43AA8B;'>No Analysis Results Yet</h2>" +
            "<p>Click <b>Run Diagnostics</b> to analyze your data and receive personalized recommendations.</p>" +
            "<hr>" +
            "<h3>What You'll Get:</h3>" +
            "<ul>" +
            "<li><b>Adaptive Pacing Alert:</b> Real-time spike detection</li>" +
            "<li><b>Factor Contribution Chart:</b> Visual breakdown of load sources</li>" +
            "<li><b>Personalized Recommendations:</b> Micro-goals, rest suggestions, or momentum maintenance</li>" +
            "</ul>" +
            "</body></html>";
        
        resultsPane.setText(placeholder);
    }
    
    private Border createSectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(67, 170, 139)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }
    
    private void handleRunDiagnostics() {
        // Get selected range
        String selected = (String) rangeCombo.getSelectedItem();
        int days = extractDaysFromRange(selected);
        
        statusLabel.setText("Running analysis for " + selected + "...");
        runButton.setEnabled(false);
        
        // Trigger callback
        runDiagnosticsCallback.accept(days);
    }
    
    private int extractDaysFromRange(String rangeText) {
        // Extract number from "Last X Days"
        String[] parts = rangeText.split(" ");
        return Integer.parseInt(parts[1]);
    }
    
    /**
     * Display analysis results (called from main app after Python service responds)
     */
    public void displayResults(String htmlFilePath) {
        try {
            // Load the HTML report file
            File reportFile = new File(htmlFilePath);
            if (reportFile.exists()) {
                resultsPane.setPage(reportFile.toURI().toURL());
                statusLabel.setText("✓ Analysis complete. Viewing results from: " + htmlFilePath);
            } else {
                displayError("Report file not found: " + htmlFilePath);
            }
        } catch (Exception e) {
            displayError("Error loading results: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runButton.setEnabled(true);
        }
    }
    
    /**
     * Display inline results from Python API response
     */
    public void displayInlineResults(String pacingState, String recommendation,
                                     double latestLoad, double threshold,
                                     String base64Image) {
        String html = String.format(
            "<html><body style='font-family: Arial; padding: 20px;'>" +
            "<div style='background-color: #fee; border-left: 4px solid #e00; padding: 15px; margin-bottom: 20px;'>" +
            "<h2 style='color: #c00; margin-top: 0;'>%s</h2>" +
            "<p style='font-size: 14px;'>%s</p>" +
            "<p style='font-size: 12px; color: #666;'>" +
            "Latest Load: <b>%.1f</b> | Threshold: <b>%.1f</b>" +
            "</p>" +
            "</div>" +
            "<h3>Factor Contribution Visualization</h3>" +
            "<img src='data:image/png;base64,%s' width='100%%' />" +
            "</body></html>",
            pacingState, recommendation, latestLoad, threshold, base64Image
        );
        
        resultsPane.setText(html);
        statusLabel.setText("✓ Analysis complete.");
        runButton.setEnabled(true);
    }
    
    private void displayError(String errorMessage) {
        String html = 
            "<html><body style='font-family: Arial; padding: 20px;'>" +
            "<h2 style='color: #d00;'>❌ Error</h2>" +
            "<p>" + errorMessage + "</p>" +
            "<hr>" +
            "<h3>Troubleshooting:</h3>" +
            "<ul>" +
            "<li>Ensure the Python service is running: <code>python server.py</code></li>" +
            "<li>Check that you have at least 7 days of data</li>" +
            "<li>Verify all required Python packages are installed</li>" +
            "</ul>" +
            "</body></html>";
        
        resultsPane.setText(html);
        statusLabel.setText("Analysis failed.");
        runButton.setEnabled(true);
    }
    
    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new File(url).toURI());
            }
        } catch (Exception e) {
            System.err.println("Could not open URL: " + e.getMessage());
        }
    }
}