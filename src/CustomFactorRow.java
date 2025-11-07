// CustomFactorRow.java
// A single row in the custom factors section with name, level, effect, and delete button

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class CustomFactorRow extends JPanel {
    
    private JTextField nameField;
    private JSpinner levelSpinner;
    private JComboBox<String> effectCombo;
    private JButton deleteButton;
    private Consumer<CustomFactorRow> deleteCallback;
    
    public CustomFactorRow(Consumer<CustomFactorRow> deleteCallback) {
        this.deleteCallback = deleteCallback;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new GridLayout(1, 4, 10, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Factor name field
        nameField = new JTextField();
        nameField.setFont(new Font("Arial", Font.PLAIN, 12));
        add(nameField);
        
        // Level spinner (1-5)
        SpinnerNumberModel levelModel = new SpinnerNumberModel(5, 1, 5, 1);
        levelSpinner = new JSpinner(levelModel);
        levelSpinner.setFont(new Font("Arial", Font.PLAIN, 12));
        add(levelSpinner);
        
        // Effect combo box
        String[] effects = {"+1 (Adds Stress)", "-1 (Protective)"};
        effectCombo = new JComboBox<>(effects);
        effectCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        add(effectCombo);
        
        // Delete button
        deleteButton = new JButton("×");
        deleteButton.setFont(new Font("Arial", Font.BOLD, 18));
        deleteButton.setForeground(Color.RED);
        deleteButton.setFocusPainted(false);
        deleteButton.setPreferredSize(new Dimension(40, 30));
        deleteButton.addActionListener(e -> deleteCallback.accept(this));
        add(deleteButton);
    }
    
    /**
     * Get the CustomFactor object from this row's current values
     */
    public CustomFactor getCustomFactor() {
        String name = nameField.getText().trim();
        int level = (Integer) levelSpinner.getValue();
        int effect = effectCombo.getSelectedIndex() == 0 ? 1 : -1;
        
        return new CustomFactor(name, level, effect);
    }
    
    /**
     * Set this row's values from a CustomFactor object
     */
    public void setCustomFactor(CustomFactor factor) {
        nameField.setText(factor.getName());
        levelSpinner.setValue(factor.getLevel());
        effectCombo.setSelectedIndex(factor.getEffect() == 1 ? 0 : 1);
    }
}