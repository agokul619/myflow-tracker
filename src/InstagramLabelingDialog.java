 import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstagramLabelingDialog extends JDialog {
    
    private final Map<String, JComboBox<String>> accountDropdowns;
    private boolean saved = false;
    private Map<String, Integer> translatedWeights;

    /**
     * Creates the Vibe Check popup to categorize unknown Instagram accounts.
     * @param parent          The main application window (so the popup centers over it)
     * @param unknownAccounts A list of Instagram usernames found in the JSON file
     */
    public InstagramLabelingDialog(Frame parent, List<String> unknownAccounts) {
        super(parent, "Instagram Vibe Check", true); // 'true' makes it modal (blocks other clicks)
        
        // Window settings
        setSize(450, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        accountDropdowns = new HashMap<>();
        translatedWeights = new HashMap<>();

        // 1. Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        headerPanel.setBackground(new Color(248, 250, 252)); // Slate 50
        
        JLabel titleLabel = new JLabel("<html><b>Unknown Accounts Found</b><br><i style='font-size:10px; color:#64748b;'>How do these affect your stress levels?</i></html>");
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Scrollable List of Accounts
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        listPanel.setBackground(Color.WHITE);

        // The 3 Mathematical Weights
        String[] options = {
            "Stressful / FOMO (+1)", 
            "Neutral / Memes (0)", 
            "Supportive / Friend (-1)"
        };

        for (String account : unknownAccounts) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(8, 0, 8, 0));
            row.setMaximumSize(new Dimension(500, 40)); // Keep rows neatly sized

            JLabel accountLabel = new JLabel("@" + account);
            accountLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            accountLabel.setForeground(new Color(15, 23, 42)); // Slate 900
            
            JComboBox<String> categoryBox = new JComboBox<>(options);
            categoryBox.setSelectedIndex(1); // Default to Neutral
            
            // Save the dropdown reference so we can check it later
            accountDropdowns.put(account, categoryBox);

            row.add(accountLabel, BorderLayout.WEST);
            row.add(categoryBox, BorderLayout.EAST);
            
            // Add a subtle line between rows
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240))); 
            
            listPanel.add(row);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null); // Clean look
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        add(scrollPane, BorderLayout.CENTER);

        // 3. Footer / Save Button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        footerPanel.setBackground(new Color(248, 250, 252));

        JButton saveBtn = new JButton("Save & Apply to Load");
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.setBackground(new Color(37, 99, 235)); // Blue 600
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        
        saveBtn.addActionListener(e -> {
            // Convert human selections into mathematical weights
            for (Map.Entry<String, JComboBox<String>> entry : accountDropdowns.entrySet()) {
                String accountName = entry.getKey();
                String selection = (String) entry.getValue().getSelectedItem();
                
                int mathWeight = 0;
                if (selection.contains("+1")) {
                    mathWeight = 1;
                } else if (selection.contains("-1")) {
                    mathWeight = -1;
                }
                
                translatedWeights.put(accountName, mathWeight);
            }
            
            saved = true; // Mark as successfully saved!
            dispose();    // Close the popup, which resumes DailyLogPanel code
        });

        footerPanel.add(saveBtn);
        add(footerPanel, BorderLayout.SOUTH);
    }

    // DailyLogPanel needs this to check if the user clicked "Save" instead of the "X" button
    public boolean isSaved() {
        return saved;
    }

    // Added this so DailyLogPanel can retrieve the math weights once it resumes
    public Map<String, Integer> getTranslatedWeights() {
        return translatedWeights;
    }
}