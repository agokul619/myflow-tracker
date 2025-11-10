// LoginScreen.java
// Responsive login screen with logo background that adapts to window size

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;
import java.io.IOException; // Add this if not present
import javax.imageio.ImageIO; 

public class LoginScreen extends JFrame {
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton newUserButton;
    private Properties userCredentials;
    private static final String USERS_FILE = "users.properties";
    
    public LoginScreen() {
        loadUserCredentials();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("MyFlow - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750); // Increased size to fit larger components better
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 650));  

        // Main panel with solid color/gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // FIXED TYPO: Must be Graphics2D
                Graphics2D g2d = (Graphics2D) g; 
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Gradient background
                Color color1 = new Color(67, 170, 139);
                Color color2 = new Color(80, 200, 160);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // Use BoxLayout (Y_AXIS) for main panel to center ALL content vertically
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Add vertical glue to push content to the center from the top
        mainPanel.add(Box.createVerticalGlue());
        
        // --- 1. Title/Tagline Panel (Header) ---
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        JLabel titleLabel = new JLabel("MyFlow");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 64));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(15));

        JLabel taglineLabel = new JLabel("Your patterns. Your triggers. Your solutions.");
        taglineLabel.setFont(new Font("Arial", Font.BOLD, 16));
        taglineLabel.setForeground(Color.WHITE);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(taglineLabel);
        
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(40)); // Space below the header

        // --- 2. Login Form and Logo Panel (Central Content - Side-by-Side) ---
        JPanel centralFormsPanel = new JPanel();
        centralFormsPanel.setOpaque(false);
        centralFormsPanel.setLayout(new BoxLayout(centralFormsPanel, BoxLayout.X_AXIS)); 
        centralFormsPanel.setAlignmentX(Component.CENTER_ALIGNMENT); 
        centralFormsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Create the Logo panel
        JPanel logoPanel = createLogoPanel(); 
        centralFormsPanel.add(logoPanel);
        
        // Spacer between logo and form
        centralFormsPanel.add(Box.createHorizontalStrut(50));
        
        // All Login components (Form + Buttons)
        JPanel loginContainer = new JPanel();
        loginContainer.setOpaque(false);
        loginContainer.setLayout(new BoxLayout(loginContainer, BoxLayout.Y_AXIS));
        loginContainer.setAlignmentY(Component.CENTER_ALIGNMENT); 
        
        // Login form panel
        JPanel loginPanel = createLoginPanel();
        loginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginContainer.add(loginPanel);
        
        loginContainer.add(Box.createVerticalStrut(30));
        
        // Login button
        loginButton = new JButton("Log In");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(350, 45));
        loginButton.setMaximumSize(new Dimension(350, 45));
        loginButton.setBackground(Color.WHITE); 
        loginButton.setForeground(new Color(67, 170, 139)); 
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(true); 
        loginButton.setBorder(BorderFactory.createLineBorder(new Color(67, 170, 139), 2));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());
        loginContainer.add(loginButton);
        
        loginContainer.add(Box.createVerticalStrut(15));
        
        // New user button
        newUserButton = new JButton("I'm New Here - Create Account");
        newUserButton.setFont(new Font("Arial", Font.BOLD, 15));
        newUserButton.setPreferredSize(new Dimension(350, 45));
        newUserButton.setMaximumSize(new Dimension(350, 45));
        newUserButton.setBackground(new Color(67, 170, 139)); 
        newUserButton.setForeground(Color.WHITE);
        newUserButton.setFocusPainted(false);
        newUserButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        newUserButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newUserButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        newUserButton.setOpaque(true);
        newUserButton.addActionListener(e -> showRegistrationScreen());
        loginContainer.add(newUserButton);
        
        centralFormsPanel.add(loginContainer);
        
        mainPanel.add(centralFormsPanel);

        // Add vertical glue to push content to the center from the bottom
        mainPanel.add(Box.createVerticalGlue());
        
        // Press Enter to login
        passwordField.addActionListener(e -> handleLogin());
        
        add(mainPanel);
    }
    
    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false); 
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        final int LOGO_SIZE = 450; 
        
        try {
            Image img = null;
            String[] paths = {"logo.png", "src/logo.png", "./logo.png"};
            
            for (String path : paths) {
                java.io.File logoFile = new java.io.File(path);
                if (logoFile.exists() && logoFile.length() > 0) { 
                    try {
                        img = javax.imageio.ImageIO.read(logoFile);
                        if (img != null && img.getWidth(null) > 0) {
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("ImageIO read failed for: " + path + " - " + e.getMessage());
                    }
                }
            }
            
            if (img != null) {
                Image scaledImg = img.getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
                
                logoLabel.setOpaque(false); 
                logoPanel.add(logoLabel);
                
                Dimension panelDim = new Dimension(LOGO_SIZE, LOGO_SIZE);
                logoPanel.setPreferredSize(panelDim);
                logoPanel.setMaximumSize(panelDim);

            }
        } catch (Exception e) {
            System.err.println("Error creating logo panel: " + e.getMessage());
            e.printStackTrace(); 
        }
        
        return logoPanel;
    }
    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(Color.WHITE);
        // Increased height to fit larger fields
        loginPanel.setPreferredSize(new Dimension(450, 300)); 
        loginPanel.setMaximumSize(new Dimension(450, 300));
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 2),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        
        // Username label
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(userLabel);
        
        loginPanel.add(Box.createVerticalStrut(15)); // Increased space
        
        // Username field (BIGGER)
        usernameField = new JTextField();
        usernameField.setFont(new Font("Arial", Font.PLAIN, 18));
        usernameField.setPreferredSize(new Dimension(400, 60)); // <-- BIGGER
        usernameField.setMaximumSize(new Dimension(400, 60));   // <-- BIGGER
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(usernameField);
        
        loginPanel.add(Box.createVerticalStrut(25)); // Increased separation
        
        // Password label
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(passLabel);
        
        loginPanel.add(Box.createVerticalStrut(15)); // Increased space
        
        // Password field (BIGGER)
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 18));
        passwordField.setPreferredSize(new Dimension(400, 60)); // <-- BIGGER
        passwordField.setMaximumSize(new Dimension(400, 60));   // <-- BIGGER
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(passwordField);
        
        return loginPanel;
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password!",
                "Missing Info",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String storedPassword = userCredentials.getProperty(username);
        
        if (storedPassword != null && storedPassword.equals(password)) {
            // Successful login
            dispose();
            SwingUtilities.invokeLater(() -> {
                MyFlowApp app = new MyFlowApp();
                app.setVisible(true);
            });
        } else {
            JOptionPane.showMessageDialog(this,
                "Wrong username or password!\nTry again or click 'I'm New Here' to register.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }
    
    private void showRegistrationScreen() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            RegistrationScreen regScreen = new RegistrationScreen();
            regScreen.setVisible(true);
        });
    }
    
    private void loadUserCredentials() {
        userCredentials = new Properties();
        try {
            File file = new File(USERS_FILE);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                userCredentials.load(fis);
                fis.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading user credentials: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen();
            loginScreen.setVisible(true);
        });
    }
}