// CustomFactorRow.java
// A single row for custom factor input with improved UI

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class CustomFactorRow extends JPanel {
    
    private JTextField nameField;
    private JSpinner levelSpinner;
    private JComboBox<String> effectCombo;
    private JButton removeButton;
    private Consumer<CustomFactorRow> removeCallback;
    
    public CustomFactorRow(Consumer<CustomFactorRow> removeCallback) {
        this.removeCallback = removeCallback;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new GridBagLayout());
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridy = 0;
        
        // Name field - bigger and cuter
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        nameField = new JTextField();
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        nameField.setPreferredSize(new Dimension(200, 35));
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        add(nameField, gbc);
        
        // Level spinner - bigger
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        levelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        levelSpinner.setFont(new Font("Arial", Font.BOLD, 14));
        levelSpinner.setPreferredSize(new Dimension(80, 35));
        JComponent editor = levelSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setHorizontalAlignment(JTextField.CENTER);
        }
        add(levelSpinner, gbc);
        
        // Effect dropdown - bigger
        gbc.gridx = 2;
        gbc.weightx = 0.25;
        effectCombo = new JComboBox<>(new String[]{"+1 (Harder)", "-1 (Easier)"});
        effectCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        effectCombo.setPreferredSize(new Dimension(120, 35));
        add(effectCombo, gbc);
        
        // Remove button - smaller and cuter
        gbc.gridx = 3;
        gbc.weightx = 0;
        removeButton = new JButton("✕");
        removeButton.setFont(new Font("Arial", Font.BOLD, 14));
        removeButton.setPreferredSize(new Dimension(35, 35));
        removeButton.setBackground(new Color(255, 100, 100));
        removeButton.setForeground(Color.WHITE);
        removeButton.setFocusPainted(false);
        removeButton.setBorderPainted(false);
        removeButton.setToolTipText("Remove this item");
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.addActionListener(e -> removeCallback.accept(this));
        add(removeButton, gbc);
    }
    
    public CustomFactor getCustomFactor() {
        String name = nameField.getText().trim();
        int level = (Integer) levelSpinner.getValue();
        int effect = effectCombo.getSelectedIndex() == 0 ? 1 : -1;
        
        return new CustomFactor(name, level, effect);
    }
    
    public void setCustomFactor(CustomFactor factor) {
        nameField.setText(factor.getName());
        levelSpinner.setValue(factor.getLevel());
        effectCombo.setSelectedIndex(factor.getEffect() == 1 ? 0 : 1);
    }
}