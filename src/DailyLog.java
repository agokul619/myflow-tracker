// models/DailyLog.java
// Represents a complete daily log entry with all tracked metrics

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyLog {
    private LocalDate date;
    private PhysiologicalData physiological;
    private CognitiveLoad cognitiveLoad;
    private EmotionalData emotional;
    private Symptoms symptoms;
    private ScreenData screen;
    private SocialData social;
    private List<CustomFactor> customFactors;
    private String journal;
    
    // Constructor
    public DailyLog() {
        this.date = LocalDate.now();
        this.physiological = new PhysiologicalData();
        this.cognitiveLoad = new CognitiveLoad();
        this.emotional = new EmotionalData();
        this.symptoms = new Symptoms();
        this.screen = new ScreenData();
        this.social = new SocialData();
        this.customFactors = new ArrayList<>();
        this.journal = "";
    }
    
    public DailyLog(LocalDate date) {
        this();
        this.date = date;
    }
    
    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public PhysiologicalData getPhysiological() { return physiological; }
    public void setPhysiological(PhysiologicalData physiological) { 
        this.physiological = physiological; 
    }
    
    public CognitiveLoad getCognitiveLoad() { return cognitiveLoad; }
    public void setCognitiveLoad(CognitiveLoad cognitiveLoad) { 
        this.cognitiveLoad = cognitiveLoad; 
    }
    
    public EmotionalData getEmotional() { return emotional; }
    public void setEmotional(EmotionalData emotional) { 
        this.emotional = emotional; 
    }
    
    public Symptoms getSymptoms() { return symptoms; }
    public void setSymptoms(Symptoms symptoms) { 
        this.symptoms = symptoms; 
    }
    
    public ScreenData getScreen() { return screen; }
    public void setScreen(ScreenData screen) { 
        this.screen = screen; 
    }
    
    public SocialData getSocial() { return social; }
    public void setSocial(SocialData social) { 
        this.social = social; 
    }
    
    public List<CustomFactor> getCustomFactors() { return customFactors; }
    public void setCustomFactors(List<CustomFactor> customFactors) { 
        this.customFactors = customFactors; 
    }
    
    public void addCustomFactor(CustomFactor factor) {
        this.customFactors.add(factor);
    }
    
    public void removeCustomFactor(int index) {
        if (index >= 0 && index < customFactors.size()) {
            customFactors.remove(index);
        }
    }
    
    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }
    
    @Override
    public String toString() {
        return "DailyLog{" +
                "date=" + date +
                ", ticCount=" + symptoms.getTicCount() +
                ", stress=" + emotional.getStress() +
                ", customFactors=" + customFactors.size() +
                '}';
    }
}

// Nested data models
class PhysiologicalData {
    private double sleepHours;
    
    public PhysiologicalData() {
        this.sleepHours = 8.0; // Default healthy sleep
    }
    
    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { 
        this.sleepHours = Math.max(0, Math.min(24, sleepHours)); // 0-24 range
    }
}

class CognitiveLoad {
    private int studyMinutes;
    
    public CognitiveLoad() {
        this.studyMinutes = 0;
    }
    
    public int getStudyMinutes() { return studyMinutes; }
    public void setStudyMinutes(int studyMinutes) { 
        this.studyMinutes = Math.max(0, studyMinutes); 
    }
}

class EmotionalData {
    private int stress; // 0-10 scale
    
    public EmotionalData() {
        this.stress = 0;
    }
    
    public int getStress() { return stress; }
    public void setStress(int stress) { 
        this.stress = Math.max(0, Math.min(10, stress)); // Clamp to 0-10
    }
}

class Symptoms {
    private int ticCount; // 0-10 scale as shown in UI
    
    public Symptoms() {
        this.ticCount = 0;
    }
    
    public int getTicCount() { return ticCount; }
    public void setTicCount(int ticCount) { 
        this.ticCount = Math.max(0, Math.min(10, ticCount)); 
    }
}

class ScreenData {
    private double screenTimeHours;
    
    public ScreenData() {
        this.screenTimeHours = 0;
    }
    
    public double getScreenTimeHours() { return screenTimeHours; }
    public void setScreenTimeHours(double screenTimeHours) { 
        this.screenTimeHours = Math.max(0, screenTimeHours); 
    }
}

class SocialData {
    private boolean socialConflict;
    
    public SocialData() {
        this.socialConflict = false;
    }
    
    public boolean isSocialConflict() { return socialConflict; }
    public void setSocialConflict(boolean socialConflict) { 
        this.socialConflict = socialConflict; 
    }
}