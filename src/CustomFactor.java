// models/CustomFactor.java
// Represents a personalized factor that can be tracked daily
// Effect: +1 = adds stress/load, -1 = protective/reduces load

public class CustomFactor {
    private String name;
    private int level;      // Intensity 1-5
    private int effect;     // Direction: +1 (adds stress) or -1 (protective)
    
    // Default constructor
    public CustomFactor() {
        this.name = "";
        this.level = 1;
        this.effect = 1; // Default to stress-adding
    }
    
    // Full constructor
    public CustomFactor(String name, int level, int effect) {
        this.name = name;
        this.level = Math.max(1, Math.min(5, level)); // Clamp to 1-5
        this.effect = (effect >= 0) ? 1 : -1; // Normalize to +1 or -1
    }
    
    // Getters and Setters
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public int getLevel() { 
        return level; 
    }
    
    public void setLevel(int level) { 
        this.level = Math.max(1, Math.min(5, level)); 
    }
    
    public int getEffect() { 
        return effect; 
    }
    
    public void setEffect(int effect) { 
        this.effect = (effect >= 0) ? 1 : -1; 
    }
    
    // Helper method to get effect as string for UI display
    public String getEffectLabel() {
        return (effect == 1) ? "+1 (Adds Stress)" : "-1 (Protective)";
    }
    
    // Calculate the impact score (level * effect)
    public int getImpact() {
        return level * effect;
    }
    
    @Override
    public String toString() {
        return name + " (Level: " + level + ", Effect: " + getEffectLabel() + ")";
    }
    
    // Helper to create common protective factors
    public static CustomFactor createProtective(String name, int level) {
        return new CustomFactor(name, level, -1);
    }
    
    // Helper to create common stress factors
    public static CustomFactor createStressor(String name, int level) {
        return new CustomFactor(name, level, 1);
    }
}