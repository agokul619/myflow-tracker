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
        setTitle("MyFlow - Track How You're Feeling");
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
        tabbedPane.addTab("📝 Today's Check-In", logPanel);
        
        // Tab 2: Diagnostics
        diagnosticsPanel = new DiagnosticsPanel(this::onRunDiagnostics);
        tabbedPane.addTab("📊 My Patterns", diagnosticsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
     // Status bar at bottom
        statusLabel = new JLabel(dataManager.getDataStats(), SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)), // Light top border
            BorderFactory.createEmptyBorder(8, 15, 8, 15) // Internal padding
        ));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Slightly larger font
        statusLabel.setForeground(new Color(100, 100, 100)); // Darker gray text
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
        // Use a slightly darker teal/green for contrast against the light content area
        panel.setBackground(new Color(55, 150, 110)); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25)); // Increased padding
        
        // Left side: Logo + App Name + Tagline
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0)); // Increased gap
        leftPanel.setOpaque(false);
        
        // --- 1. Streamlined Logo Loading ---
        
        JLabel logoLabel = new JLabel("💗"); // Default emoji fallback
        final int LOGO_SIZE = 55; // Smaller logo for the header
        
        try {
            // Simplified logic: try relative path first, then src path
            String[] logoPaths = {"logo.png", "src/logo.png"};
            ImageIcon logoIcon = null;
            
            for (String path : logoPaths) {
                java.io.File logoFile = new java.io.File(path);
                if (logoFile.exists() && logoFile.canRead()) {
                    logoIcon = new ImageIcon(logoFile.getAbsolutePath());
                    if (logoIcon.getIconWidth() > 0) {
                        Image img = logoIcon.getImage().getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH);
                        logoLabel = new JLabel(new ImageIcon(img));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // If loading fails, keep the emoji fallback
        }
        
        // If logo is still the emoji, use a larger font
     // If the logo was not successfully loaded (i.e., it still has the emoji text)
        // or if it doesn't have an icon, apply the larger font to the emoji.
        if (logoLabel.getIcon() == null) {
             logoLabel.setFont(new Font("Arial", Font.PLAIN, 40));
        }

        leftPanel.add(logoLabel);
        
        // --- 2. Text Panel Refinement ---
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel appNameLabel = new JLabel("MyFlow");
        appNameLabel.setFont(new Font("Arial", Font.BOLD, 36)); // Slightly bigger app name
        appNameLabel.setForeground(Color.WHITE);
        
        // Combined Title and Subtitle for a cleaner look
        JLabel mainTitleLabel = new JLabel("Track your feelings and what's happening in your life");
        mainTitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        mainTitleLabel.setForeground(new Color(200, 230, 215)); // Light gray-green
        
        textPanel.add(appNameLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(mainTitleLabel);
        
        leftPanel.add(textPanel);
        panel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: Placeholder for user profile/settings button
        JButton settingsButton = new JButton("👤");
        settingsButton.setFont(new Font("Arial", Font.PLAIN, 20));
        settingsButton.setToolTipText("Settings and Profile");
        settingsButton.setBackground(new Color(80, 180, 140));
        settingsButton.setForeground(Color.WHITE);
        settingsButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        settingsButton.setFocusPainted(false);
        settingsButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(settingsButton);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    /**
     * Called when user clicks "Save Data" button
     */
    private void onSaveData(DailyLog log) {
        boolean success = dataManager.saveDailyLog(log);
        
        if (success) {
            JOptionPane.showMessageDialog(this,
                "✓ Your check-in was saved for " + log.getDate(),
                "Saved!",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Update status bar
            statusLabel.setText(dataManager.getDataStats());
            
        } else {
            JOptionPane.showMessageDialog(this,
                "Oops! Something went wrong. Please try saving again.",
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
                "You need more check-ins to see your patterns.\n" +
                "Keep tracking for at least 7 days to get helpful insights.\n" +
                "You have " + logs.size() + " days so far - keep it up!",
                "Not Quite Ready",
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
                        "Something went wrong while looking at your patterns:\n" + errorMessage +
                        "\n\nMake sure the helper program is running:\n" +
                        "python server.py",
                        "Oops!",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    private JDialog createLoadingDialog() {
        JDialog dialog = new JDialog(this, "Looking at Your Patterns", true);
        dialog.setLayout(new BorderLayout(20, 20));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("Finding patterns in your data...", SwingConstants.CENTER);
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