import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstagramLabelingDialog extends JDialog {
    
    private Map<String, Integer> categorizedAccounts;
    private boolean isSaved = false;

    public InstagramLabelingDialog(Frame parent, List<String> unknownAccounts) {
        super(parent, "📱 Instagram Data Labeling", true); // true makes it a modal (blocks background)
        categorizedAccounts = new HashMap<>();
        
        // Default everything to 0 (Neutral) just in case
        for (String acc : unknownAccounts) {
            categorizedAccounts.put(acc, 0);
        }
        
        initializeUI(unknownAccounts);
    }

    private void initializeUI(List<String> unknownAccounts) {
        setSize(600, 500);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Tag Your Unknown Accounts");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel subLabel = new JLabel("What's the usual vibe of these accounts? This helps the ML model learn.");
        subLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subLabel.setForeground(Color.GRAY);
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(subLabel);
        add(headerPanel, BorderLayout.NORTH);

        // The Scrolling List of Accounts
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        for (String account : unknownAccounts) {
            listPanel.add(createAccountRow(account));
            listPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Save Button at the bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("💾 Save Tags & Import");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setBackground(new Color(67, 170, 139));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> {
            isSaved = true;
            dispose(); // Close the window
        });
        
        bottomPanel.add(saveButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createAccountRow(String accountName) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // Account Name
        JLabel nameLabel = new JLabel("@" + accountName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        row.add(nameLabel, BorderLayout.WEST);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        // +1 (Negative Load)
        JButton btnNegative = new JButton("FOMO / Drama");
        btnNegative.setBackground(new Color(255, 230, 230)); // Light Red
        
        // 0 (Neutral Load)
        JButton btnNeutral = new JButton("Memes / Updates");
        btnNeutral.setBackground(new Color(230, 230, 230)); // Gray
        
        // -1 (Protective Load)
        JButton btnPositive = new JButton("Hype / Support");
        btnPositive.setBackground(new Color(230, 255, 230)); // Light Green

        // Button Group so only one can be visually "active" at a time
        JButton[] buttons = {btnNegative, btnNeutral, btnPositive};
        
        btnNegative.addActionListener(e -> selectButton(buttons, btnNegative, accountName, 1));
        btnNeutral.addActionListener(e -> selectButton(buttons, btnNeutral, accountName, 0));
        btnPositive.addActionListener(e -> selectButton(buttons, btnPositive, accountName, -1));

        // Default selection visually is Neutral
        btnNeutral.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        buttonsPanel.add(btnNegative);
        buttonsPanel.add(btnNeutral);
        buttonsPanel.add(btnPositive);
        
        row.add(buttonsPanel, BorderLayout.EAST);
        return row;
    }

    private void selectButton(JButton[] allButtons, JButton clicked, String account, int score) {
        // Reset borders for all buttons
        for (JButton btn : allButtons) {
            btn.setBorder(UIManager.getBorder("Button.border"));
        }
        // Highlight clicked button
        clicked.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        
        // Save the math value in the background
        categorizedAccounts.put(account, score);
    }

    public Map<String, Integer> getCategorizedAccounts() {
        return categorizedAccounts;
    }

    public boolean isSaved() {
        return isSaved;
    }
}