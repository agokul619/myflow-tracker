// LoginScreen.java
// Clean login screen with no logo image, fully visible buttons

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;

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
        setSize(600, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(500, 550));

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

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(Box.createVerticalGlue());

        // --- Header ---
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("MyFlow");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 64));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(10));

        JLabel taglineLabel = new JLabel("Your patterns. Your triggers. Your solutions.");
        taglineLabel.setFont(new Font("Arial", Font.BOLD, 16));
        taglineLabel.setForeground(Color.WHITE);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(taglineLabel);

        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(35));

        // --- Login Form ---
        JPanel loginPanel = createLoginPanel();
        loginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(loginPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        // --- Login Button (dark teal so it stands out) ---
        loginButton = new JButton("Log In");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(350, 48));
        loginButton.setMaximumSize(new Dimension(350, 48));
        loginButton.setBackground(new Color(30, 100, 80));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());
        mainPanel.add(loginButton);

        mainPanel.add(Box.createVerticalStrut(12));

        // --- New User Button ---
        newUserButton = new JButton("I'm New Here - Create Account");
        newUserButton.setFont(new Font("Arial", Font.BOLD, 15));
        newUserButton.setPreferredSize(new Dimension(350, 48));
        newUserButton.setMaximumSize(new Dimension(350, 48));
        newUserButton.setBackground(Color.WHITE);
        newUserButton.setForeground(new Color(67, 170, 139));
        newUserButton.setFocusPainted(false);
        newUserButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        newUserButton.setOpaque(true);
        newUserButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newUserButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        newUserButton.addActionListener(e -> showRegistrationScreen());
        mainPanel.add(newUserButton);

        mainPanel.add(Box.createVerticalGlue());

        // Press Enter to login
        passwordField.addActionListener(e -> handleLogin());

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(Color.WHITE);
        loginPanel.setPreferredSize(new Dimension(420, 280));
        loginPanel.setMaximumSize(new Dimension(420, 280));
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
        loginPanel.add(Box.createVerticalStrut(10));

        // Username field
        usernameField = new JTextField();
        usernameField.setFont(new Font("Arial", Font.PLAIN, 18));
        usernameField.setPreferredSize(new Dimension(360, 55));
        usernameField.setMaximumSize(new Dimension(360, 55));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(usernameField);
        loginPanel.add(Box.createVerticalStrut(20));

        // Password label
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(passLabel);
        loginPanel.add(Box.createVerticalStrut(10));

        // Password field
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 18));
        passwordField.setPreferredSize(new Dimension(360, 55));
        passwordField.setMaximumSize(new Dimension(360, 55));
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