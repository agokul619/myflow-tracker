// RegistrationScreen.java
// New user registration with profile information

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class RegistrationScreen extends JFrame {
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField nameField;
    private JTextField profileNameField;
    private JSpinner ageSpinner;
    private JButton registerButton;
    private JButton backButton;
    private static final String USERS_FILE = "users.properties";
    private static final String PROFILES_FILE = "profiles.properties";
    
    public RegistrationScreen() {
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("MyFlow - Create Your Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Set a larger size to accommodate all fields comfortably
        setSize(700, 850); 
        setLocationRelativeTo(null);
        // Allow resizing in case user needs more space, but set a good minimum size
        setMinimumSize(new Dimension(650, 750)); 
        
        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                Color color1 = new Color(67, 170, 139);
                Color color2 = new Color(80, 200, 160);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        // Use BoxLayout for vertical centering
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Add vertical glue to center the content from the top
        mainPanel.add(Box.createVerticalGlue());
        
        // --- 1. Header ---
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JLabel titleLabel = new JLabel("Create Your Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);
        
        headerPanel.add(Box.createVerticalStrut(10));
        
        JLabel subtitleLabel = new JLabel("Start tracking your wellness journey");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subtitleLabel.setForeground(new Color(230, 255, 245));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitleLabel);
        
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(30)); 
        
        // --- 2. Registration Form Panel ---
        
        // Form panel will be centered horizontally
        JPanel formCenterPanel = new JPanel();
        formCenterPanel.setOpaque(false);
        formCenterPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        formCenterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel formPanel = new JPanel();
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(480, 600)); // Increased size
        formPanel.setMaximumSize(new Dimension(480, 600)); 
        // Using BoxLayout Y_AXIS for vertical stacking of fields
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Internal variables for field sizing
        int fieldHeight = 50; // Increased height for bigger fields
        int strutHeight = 20; 

        // Helper function for adding field groups
        autoAddTextField(formPanel, "Full Name:", nameField = new JTextField(), fieldHeight);
        formPanel.add(Box.createVerticalStrut(strutHeight));
        
        autoAddTextField(formPanel, "Profile Name (what should we call you?):", profileNameField = new JTextField(), fieldHeight);
        formPanel.add(Box.createVerticalStrut(strutHeight));

        // Age Spinner (needs special handling)
        JLabel ageLabel = new JLabel("Age:");
        ageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        ageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(ageLabel);
        
        ageSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 120, 1));
        ageSpinner.setFont(new Font("Arial", Font.PLAIN, 18));
        ageSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        ageSpinner.setPreferredSize(new Dimension(100, fieldHeight));
        ageSpinner.setMaximumSize(new Dimension(100, fieldHeight));
        formPanel.add(ageSpinner);
        formPanel.add(Box.createVerticalStrut(strutHeight));

        autoAddTextField(formPanel, "Username:", usernameField = new JTextField(), fieldHeight);
        formPanel.add(Box.createVerticalStrut(strutHeight));

        autoAddPasswordField(formPanel, "Password:", passwordField = new JPasswordField(), fieldHeight);
        formPanel.add(Box.createVerticalStrut(strutHeight));

        autoAddPasswordField(formPanel, "Confirm Password:", confirmPasswordField = new JPasswordField(), fieldHeight);
        
        formCenterPanel.add(formPanel);
        mainPanel.add(formCenterPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // --- 3. Buttons ---
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Back button (Styled to match LoginScreen's primary button)
        backButton = new JButton("← Back to Login");
        backButton.setFont(new Font("Arial", Font.BOLD, 15));
        backButton.setPreferredSize(new Dimension(200, 45));
        backButton.setBackground(Color.WHITE);
        backButton.setForeground(new Color(67, 170, 139));
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createLineBorder(new Color(67, 170, 139), 2));
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> goBackToLogin());
        buttonPanel.add(backButton);

        // Register button (Styled to match LoginScreen's secondary button)
        registerButton = new JButton("Create Account");
        registerButton.setFont(new Font("Arial", Font.BOLD, 15));
        registerButton.setPreferredSize(new Dimension(200, 45));
        registerButton.setBackground(new Color(67, 170, 139));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> handleRegistration());
        buttonPanel.add(registerButton);
        
        mainPanel.add(buttonPanel);
        
        // Add vertical glue to center the content from the bottom
        mainPanel.add(Box.createVerticalGlue());
        
        add(mainPanel);
    }
    
    // --- New Helper Methods (Must be added to RegistrationScreen class) ---

    /** Helper for adding standard JTextFields */
    private void autoAddTextField(JPanel container, String labelText, JTextField field, int height) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
        
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(420, height));
        field.setMaximumSize(new Dimension(420, height));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(field);
    }

    /** Helper for adding standard JPasswordFields */
    private void autoAddPasswordField(JPanel container, String labelText, JPasswordField field, int height) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
        
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(420, height));
        field.setMaximumSize(new Dimension(420, height));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(field);
    }

    // ... (rest of the class code: handleRegistration, goBackToLogin) ...
    private void handleRegistration() {
        String name = nameField.getText().trim();
        String profileName = profileNameField.getText().trim();
        int age = (Integer) ageSpinner.getValue();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());
        
        // Validation
        if (name.isEmpty() || profileName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all fields!",
                "Missing Information",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this,
                "Passwords don't match!\nPlease try again.",
                "Password Mismatch",
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            return;
        }
        
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this,
                "Password must be at least 4 characters long!",
                "Weak Password",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if username already exists
        Properties users = new Properties();
        try {
            File file = new File(USERS_FILE);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                users.load(fis);
                fis.close();
                
                if (users.containsKey(username)) {
                    JOptionPane.showMessageDialog(this,
                        "Username already taken!\nPlease choose another one.",
                        "Username Taken",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Save credentials
        users.setProperty(username, password);
        try {
            FileOutputStream fos = new FileOutputStream(USERS_FILE);
            users.store(fos, "MyFlow User Credentials");
            fos.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving account: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Save profile info
        Properties profiles = new Properties();
        try {
            File file = new File(PROFILES_FILE);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                profiles.load(fis);
                fis.close();
            }
            
            profiles.setProperty(username + ".name", name);
            profiles.setProperty(username + ".profile_name", profileName);
            profiles.setProperty(username + ".age", String.valueOf(age));
            
            FileOutputStream fos = new FileOutputStream(PROFILES_FILE);
            profiles.store(fos, "MyFlow User Profiles");
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Success!
        JOptionPane.showMessageDialog(this,
            "Welcome to MyFlow, " + profileName + "! 🎉\n\n" +
            "Your account has been created successfully.\n" +
            "You can now log in!",
            "Account Created",
            JOptionPane.INFORMATION_MESSAGE);
        
        goBackToLogin();
    }
    
    private void goBackToLogin() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen();
            loginScreen.setVisible(true);
        });
    }
}