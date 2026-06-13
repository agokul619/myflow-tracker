// DailyLogPanel.java
// The main data entry form matching the UI screenshot design

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DailyLogPanel extends JPanel {
    
    private Consumer<DailyLog> saveCallback;
    private DataManager dataManager;
    
    // Date selector
    private JTextField dateField;
    
    // Fixed metrics - Sliders
    private JSlider stressSlider;
    private JLabel stressValueLabel;
    private JSlider ticSlider;
    private JLabel ticValueLabel;
    private JSlider studySlider;
    private JLabel studyValueLabel;
    
    // Fixed metrics - Text fields
    private JTextField screenTimeField;
    private JTextField sleepHoursField;
    
    // Custom factors panel
    private JPanel customFactorsContainer;
    private List<CustomFactorRow> customFactorRows;
    
    // Journal entry
    private JTextArea journalArea;
    
    // Buttons
    private JButton saveButton;
    private JButton addFactorButton;
    
    public DailyLogPanel(Consumer<DailyLog> saveCallback) {
        this.saveCallback = saveCallback;
        this.dataManager = DataManager.getInstance();
        this.customFactorRows = new ArrayList<>();
        
        initializeUI();
        loadToday();
        
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Main content in scroll pane
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        
        // Date selector
        mainContent.add(createDatePanel());
        mainContent.add(Box.createVerticalStrut(15));
        
        // Section 1: Fixed Metrics
        mainContent.add(createFixedMetricsPanel());
        mainContent.add(Box.createVerticalStrut(15));
        
        // Section 2: Personalized Factors
        mainContent.add(createCustomFactorsPanel());
        mainContent.add(Box.createVerticalStrut(15));
        
        
        
        mainContent.add(createInstagramPanel());
        mainContent.add(Box.createVerticalStrut(15));
        
        
        
        // Section 3: Journal Entry
        mainContent.add(createJournalPanel());
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom button panel
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createDatePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel.setBackground(new Color(245, 250, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 240), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel label = new JLabel("📅");
        label.setFont(new Font("Arial", Font.PLAIN, 24));
        
        // Previous Day button - darker blue so text is visible
        JButton prevButton = new JButton("◀ Previous Day");
        prevButton.setFont(new Font("Arial", Font.BOLD, 13));
        prevButton.setPreferredSize(new Dimension(150, 40));
        prevButton.setBackground(new Color(70, 130, 220));  // Darker blue
        prevButton.setForeground(new Color(166,7,55));
        prevButton.setFocusPainted(false);
        prevButton.setBorderPainted(false);
        prevButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        prevButton.addActionListener(e -> loadPreviousDay());
        
        // Date display - calendar style
        dateField = new JTextField(LocalDate.now().toString(), 12);
        dateField.setFont(new Font("Arial", Font.BOLD, 16));
        dateField.setHorizontalAlignment(JTextField.CENTER);
        dateField.setPreferredSize(new Dimension(150, 40));
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(67, 170, 139), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Today button - darker teal so text is visible
        JButton todayButton = new JButton("📍 Today");
        todayButton.setFont(new Font("Arial", Font.BOLD, 13));
        todayButton.setPreferredSize(new Dimension(120, 40));
        todayButton.setBackground(new Color(50, 140, 115));  // Darker teal/green
        todayButton.setForeground(new Color(166,7,55));
        todayButton.setFocusPainted(false);
        todayButton.setBorderPainted(false);
        todayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        todayButton.addActionListener(e -> loadToday());
        
        panel.add(label);
        panel.add(prevButton);
        panel.add(dateField);
        panel.add(todayButton);
        
        return panel;
    }
    	
    private Color Color(int i, int j, int k) {
		// TODO Auto-generated method stub
		return null;
	}

	public void loadPreviousDay() {
    	    try {
    	        LocalDate currentDate = LocalDate.parse(dateField.getText());
    	        LocalDate prevDate = currentDate.minusDays(1);
    	        dateField.setText(prevDate.toString());
    	        
    	        DailyLog existingLog = dataManager.getLogByDate(prevDate);
    	        if (existingLog != null) {
    	            loadLogIntoUI(existingLog);
    	        } else {
    	            clearForm();
    	        }
    	    } catch (Exception e) {
    	        loadToday();
    	    }
    	}


	private JPanel createFixedMetricsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createSectionBorder("1. How You're Feeling"));
        
        // Emotional Stress (0-10)
        panel.add(createSliderPanel(
            "How Stressed Are You? (0 = Not Stressed, 10 = Very Stressed)",
            0, 10, 0,
            slider -> stressSlider = slider,
            label -> stressValueLabel = label
        ));
        panel.add(Box.createVerticalStrut(10));
        
        // Tic Level (0-10)
        panel.add(createSliderPanel(
            "Tic Level (0 = None, 10 = A Lot)",
            0, 10, 0,
            slider -> ticSlider = slider,
            label -> ticValueLabel = label
        ));
        panel.add(Box.createVerticalStrut(10));
        
        // Study/Cognitive Hours (0-15)
        panel.add(createSliderPanel(
            "How Many Hours Did You Study or Focus Hard? (0 - 15 Hours)",
            0, 15, 0,
            slider -> studySlider = slider,
            label -> studyValueLabel = label
        ));
        panel.add(Box.createVerticalStrut(15));
        
        // Text input fields
        JPanel textFieldsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        
        textFieldsPanel.add(new JLabel("Screen Time (Hours):"));
        screenTimeField = new JTextField("1.0", 8);
        textFieldsPanel.add(screenTimeField);
        
        textFieldsPanel.add(new JLabel("Sleep Hours:"));
        sleepHoursField = new JTextField("7.5", 8);
        textFieldsPanel.add(sleepHoursField);
        
        panel.add(textFieldsPanel);
        
        return panel;
    }
    
    private JPanel createSliderPanel(String label, int min, int max, int initial,
                                     Consumer<JSlider> sliderConsumer,
                                     Consumer<JLabel> labelConsumer) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        
        JSlider slider = new JSlider(min, max, initial);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        
        JLabel valueLabel = new JLabel(String.valueOf(initial));
        valueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setForeground(new Color(67, 170, 139));
        
        slider.addChangeListener(e -> {
            valueLabel.setText(String.valueOf(slider.getValue()));
        });
        
        sliderConsumer.accept(slider);
        labelConsumer.accept(valueLabel);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(valueLabel, BorderLayout.EAST);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCustomFactorsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createSectionBorder("2. What Else Happened Today?"));
        
        // Instructions
        JLabel instructions = new JLabel(
            "<html>Add things that were special about your day. <b>Level</b> = how strong (1-5), " +
            "<b>Effect</b> = Did it make things harder (+1) or easier (-1)?</html>"
        );
        instructions.setFont(new Font("Arial", Font.PLAIN, 11));
        instructions.setForeground(Color.GRAY);
        panel.add(instructions);
        panel.add(Box.createVerticalStrut(10));
        
        // Header row
        JPanel headerRow = new JPanel(new GridLayout(1, 3, 10, 0));
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        headerRow.add(createHeaderLabel("What Happened?"));
        headerRow.add(createHeaderLabel("How Much? (1-5)"));
        headerRow.add(createHeaderLabel("Harder (+1) or Easier (-1)?"));
        panel.add(headerRow);
        panel.add(Box.createVerticalStrut(5));
        
        // Container for dynamic custom factor rows
        customFactorsContainer = new JPanel();
        customFactorsContainer.setLayout(new BoxLayout(customFactorsContainer, BoxLayout.Y_AXIS));
        panel.add(customFactorsContainer);
        
        // Add Factor button
        addFactorButton = new JButton("+ Add Something");
        addFactorButton.setFont(new Font("Arial", Font.PLAIN, 12));
        addFactorButton.setBackground(new Color(245, 245, 245));
        addFactorButton.setFocusPainted(false);
        addFactorButton.addActionListener(e -> addCustomFactorRow());
        
        
    
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        buttonPanel.add(addFactorButton);
        panel.add(buttonPanel);
        
        
        
        return panel;
    }
    
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }
    
    private void addCustomFactorRow() {
        CustomFactorRow row = new CustomFactorRow(this::removeCustomFactorRow);
        customFactorRows.add(row);
        customFactorsContainer.add(row);
        customFactorsContainer.revalidate();
        customFactorsContainer.repaint();
    }
    
    private void removeCustomFactorRow(CustomFactorRow row) {
        customFactorRows.remove(row);
        customFactorsContainer.remove(row);
        customFactorsContainer.revalidate();
        customFactorsContainer.repaint();
    }
    
    private JPanel createJournalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createSectionBorder("Write About Your Day (Optional)"));
        
        journalArea = new JTextArea(4, 40);
        journalArea.setLineWrap(true);
        journalArea.setWrapStyleWord(true);
        journalArea.setFont(new Font("Arial", Font.PLAIN, 13));
        
        JScrollPane scrollPane = new JScrollPane(journalArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        saveButton = new JButton("💾 Save My Check-In");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setBackground(new Color(67, 170, 139));
        saveButton.setForeground(new Color (20, 131, 120) );
        saveButton.setFocusPainted(false);
        saveButton.setPreferredSize(new Dimension(180, 40));
        saveButton.addActionListener(e -> handleSave());
        
        panel.add(saveButton);
        
        return panel;
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
    
    private void handleSave() {
        try {
            DailyLog log = collectDataFromUI();
            saveCallback.accept(log);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error collecting data: " + e.getMessage(),
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private DailyLog collectDataFromUI() {
        DailyLog log = new DailyLog();
        
        // Date
        log.setDate(LocalDate.parse(dateField.getText()));
        
        // Fixed metrics
        log.getEmotional().setStress(stressSlider.getValue());
        log.getSymptoms().setTicCount(ticSlider.getValue());
        
        // Convert study slider hours to minutes
        int studyHours = studySlider.getValue();
        log.getCognitiveLoad().setStudyMinutes(studyHours * 60);
        
        // Text fields
        log.getScreen().setScreenTimeHours(
            Double.parseDouble(screenTimeField.getText()));
        log.getPhysiological().setSleepHours(
            Double.parseDouble(sleepHoursField.getText()));
        
        // Custom factors
        for (CustomFactorRow row : customFactorRows) {
            CustomFactor factor = row.getCustomFactor();
            if (!factor.getName().isEmpty()) {
                log.addCustomFactor(factor);
            }
        }
        
        // Journal
        log.setJournal(journalArea.getText());
        
        return log;
    }
    
    public void loadToday() {
        LocalDate today = LocalDate.now();
        dateField.setText(today.toString());
        
        // Try to load existing data for today
        DailyLog existingLog = dataManager.getLogByDate(today);
        
        if (existingLog != null) {
            loadLogIntoUI(existingLog);
        } else {
            clearForm();
        }
    }
    
    private void loadLogIntoUI(DailyLog log) {
        stressSlider.setValue(log.getEmotional().getStress());
        ticSlider.setValue(log.getSymptoms().getTicCount());
        
        // Convert study minutes back to hours for slider
        int studyHours = log.getCognitiveLoad().getStudyMinutes() / 60;
        studySlider.setValue(studyHours);
        
        screenTimeField.setText(String.valueOf(log.getScreen().getScreenTimeHours()));
        sleepHoursField.setText(String.valueOf(log.getPhysiological().getSleepHours()));
        
        // Clear and reload custom factors
        customFactorRows.clear();
        customFactorsContainer.removeAll();
        
        for (CustomFactor factor : log.getCustomFactors()) {
            CustomFactorRow row = new CustomFactorRow(this::removeCustomFactorRow);
            row.setCustomFactor(factor);
            customFactorRows.add(row);
            customFactorsContainer.add(row);
        }
        
        customFactorsContainer.revalidate();
        customFactorsContainer.repaint();
        
        journalArea.setText(log.getJournal());
    }
    
    private void clearForm() {
        stressSlider.setValue(0);
        ticSlider.setValue(0);
        studySlider.setValue(0);
        screenTimeField.setText("1.0");
        sleepHoursField.setText("7.5");
        
        customFactorRows.clear();
        customFactorsContainer.removeAll();
        customFactorsContainer.revalidate();
        customFactorsContainer.repaint();
        
        journalArea.setText("");
    }
    
  
    
    private void triggerInstagramImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Instagram JSON Data");
        
        // Only allow .json files to be selected
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToProcess = fileChooser.getSelectedFile();
            
            // TODO: Later, we will send fileToProcess to Python here!
            // For now, let's pop up the labeling dialog to test the UI flow.
            java.util.List<String> mockUnknowns = java.util.Arrays.asList(
                "sarah_smith_123", "cupertino_fbla", "josh_b_ball"
            );
            
            InstagramLabelingDialog dialog = new InstagramLabelingDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), 
                mockUnknowns
            );
            dialog.setVisible(true); // Pauses here until the user clicks Save
            
            if (dialog.isSaved()) {
                JOptionPane.showMessageDialog(this, 
                    "Successfully imported " + fileToProcess.getName() + " and saved tags!", 
                    "Import Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }  
    
    
    
    private JPanel createInstagramPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createSectionBorder("3. Passive Data (Instagram)"));

        JLabel description = new JLabel("<html>Import your Instagram JSON data to calculate your algorithmic stress load and tag peer content.</html>");
        description.setFont(new Font("Arial", Font.PLAIN, 12));
        description.setForeground(Color.GRAY);
        panel.add(description, BorderLayout.NORTH);

        JButton importIgButton = new JButton(" Import Instagram JSON");
        importIgButton.setFont(new Font("Arial", Font.BOLD, 13));
        importIgButton.setBackground(new Color(225, 48, 108)); // Instagram Pink
        importIgButton.setForeground(Color.DARK_GRAY);
        importIgButton.setFocusPainted(false);
        importIgButton.setPreferredSize(new Dimension(220, 40));
        importIgButton.addActionListener(e -> triggerInstagramImport());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(importIgButton);
        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }
    
    
    
}