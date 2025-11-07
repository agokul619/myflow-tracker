// MyFlowApp.java
// Main application window with two tabs: Daily Log Entry and Diagnostics

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class MyFlowApp extends JFrame {
    
    private DataManager dataManager;
    private DailyLogPanel logPanel;
    private DiagnosticsPanel diagnosticsPanel;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    
    public MyFlowApp() {
        dataManager = DataManager.getInstance();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("MyFlow - Mental Health Fitness Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 800);
        setLocationRelativeTo(null);
        
        // Main layout
        setLayout(new BorderLayout(10, 10));
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Tabbed pane for main content
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Tab 1: Daily Log Entry
        logPanel = new DailyLogPanel(this::onSaveData);
        tabbedPane.addTab("📝 Daily Log", logPanel);
        
        // Tab 2: Diagnostics
        diagnosticsPanel = new DiagnosticsPanel(this::onRunDiagnostics);
        tabbedPane.addTab("📊 Diagnostics", diagnosticsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar at bottom
        statusLabel = new JLabel(dataManager.getDataStats());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(Color.GRAY);
        add(statusLabel, BorderLayout.SOUTH);
        
        // Apply modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(67, 170, 139)); // Teal from your design
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("MyFlow Daily Log");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Capture your fixed metrics and personalized factors");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(230, 255, 245));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        
        panel.add(textPanel, BorderLayout.WEST);
        
        return panel;
    }
    
    // === CALLBACK METHODS ===
    
    /**
     * Called when user clicks "Save Data" button
     */
    private void onSaveData(DailyLog log) {
        boolean success = dataManager.saveDailyLog(log);
        
        if (success) {
            JOptionPane.showMessageDialog(this,
                "✓ Data saved successfully for " + log.getDate(),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Update status bar
            statusLabel.setText(dataManager.getDataStats());
            
            // Optionally clear the form or load next date
            logPanel.loadNextDay();
            
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to save data. Please try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Called when user clicks "Run Diagnostics" button
     */
    private void onRunDiagnostics(int days) {
        // Check if we have enough data
        List<DailyLog> logs = dataManager.getRecentLogs(days);
        
        if (logs.size() < 7) {
            JOptionPane.showMessageDialog(this,
                "Insufficient data for analysis.\n" +
                "You need at least 7 days of data for baseline calculation.\n" +
                "Current data points: " + logs.size(),
                "Insufficient Data",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show loading dialog
        JDialog loadingDialog = createLoadingDialog();
        loadingDialog.setVisible(true);
        
        // Run analysis in background thread to keep UI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;
            
            @Override
            protected Void doInBackground() {
                try {
                    // Get JSON payload for Python API
                    String jsonPayload = dataManager.getJsonForApi(logs);
                    
                    // Call Python microservice
                    AnalysisClient client = new AnalysisClient();
                    String result = client.runAnalysis(jsonPayload, days);
                    
                    // Update diagnostics panel with results
                    SwingUtilities.invokeLater(() -> {
                        diagnosticsPanel.displayResults(result);
                    });
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                loadingDialog.dispose();
                
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(MyFlowApp.this,
                        "Error running diagnostics:\n" + errorMessage +
                        "\n\nMake sure the Python service is running:\n" +
                        "python server.py",
                        "Analysis Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    private JDialog createLoadingDialog() {
        JDialog dialog = new JDialog(this, "Running Analysis", true);
        dialog.setLayout(new BorderLayout(20, 20));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("Analyzing your data...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        panel.add(label);
        panel.add(progressBar);
        
        dialog.add(panel);
        return dialog;
    }
    
    // === MAIN METHOD ===
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MyFlowApp app = new MyFlowApp();
            app.setVisible(true);
        });
    }
}