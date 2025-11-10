// storage/DataManager.java
// Handles all file I/O operations for DailyLog data in JSON format
// This is the SINGLE SOURCE OF TRUTH for data persistence

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class DataManager {
    
    public static final String DATA_FILE = "myflow_data.json";
    private static final String BACKUP_DIR = "backups/";
    
    // Singleton pattern for consistent data access
    private static DataManager instance;
    
    private DataManager() {
        ensureDataFileExists();
        ensureBackupDirectoryExists();
    }
    
    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    // === CORE STORAGE METHODS ===
    
    /**
     * Save a single daily log entry. If an entry for the same date exists, it's updated.
     */
    public boolean saveDailyLog(DailyLog log) {
        try {
            List<DailyLog> allLogs = loadAllLogs();
            
            // Remove existing entry for this date if present
            allLogs.removeIf(existingLog -> 
                existingLog.getDate().equals(log.getDate()));
            
            // Add the new/updated log
            allLogs.add(log);
            
            // Sort by date (oldest first)
            allLogs.sort(Comparator.comparing(DailyLog::getDate));
            
            // Write to file
            return saveAllLogs(allLogs);
            
        } catch (Exception e) {
            System.err.println("Error saving daily log: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load all daily log entries from the JSON file
     */
    public List<DailyLog> loadAllLogs() {
        List<DailyLog> logs = new ArrayList<>();
        
        try {
            String jsonContent = Files.readString(Path.of(DATA_FILE));
            JSONArray jsonArray = new JSONArray(jsonContent);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonLog = jsonArray.getJSONObject(i);
                DailyLog log = deserializeLog(jsonLog);
                logs.add(log);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading logs: " + e.getMessage());
        }
        
        return logs;
    }
    
    /**
     * Get logs for a specific date range (inclusive)
     */
    public List<DailyLog> getLogsInRange(LocalDate startDate, LocalDate endDate) {
        List<DailyLog> allLogs = loadAllLogs();
        List<DailyLog> filteredLogs = new ArrayList<>();
        
        for (DailyLog log : allLogs) {
            LocalDate logDate = log.getDate();
            if (!logDate.isBefore(startDate) && !logDate.isAfter(endDate)) {
                filteredLogs.add(log);
            }
        }
        
        return filteredLogs;
    }
    
    /**
     * Get the most recent N days of logs
     */
    public List<DailyLog> getRecentLogs(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        return getLogsInRange(startDate, endDate);
    }
    
    /**
     * Get a specific log by date
     */
    public DailyLog getLogByDate(LocalDate date) {
        List<DailyLog> allLogs = loadAllLogs();
        return allLogs.stream()
                .filter(log -> log.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Delete a log entry by date
     */
    public boolean deleteLog(LocalDate date) {
        try {
            List<DailyLog> allLogs = loadAllLogs();
            boolean removed = allLogs.removeIf(log -> log.getDate().equals(date));
            
            if (removed) {
                return saveAllLogs(allLogs);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error deleting log: " + e.getMessage());
            return false;
        }
    }
    
    // === JSON SERIALIZATION ===
    
    /**
     * Convert DailyLog object to JSON format matching Python API expectations
     */
    private JSONObject serializeLog(DailyLog log) {
        JSONObject json = new JSONObject();
        
        json.put("date", log.getDate().toString());
        
        // Physiological data
        JSONObject physio = new JSONObject();
        physio.put("sleep_hours", log.getPhysiological().getSleepHours());
        json.put("physiological", physio);
        
        // Cognitive load
        JSONObject cognitive = new JSONObject();
        cognitive.put("study_minutes", log.getCognitiveLoad().getStudyMinutes());
        json.put("cognitive_load", cognitive);
        
        // Emotional data
        JSONObject emotional = new JSONObject();
        emotional.put("stress", log.getEmotional().getStress());
        json.put("emotional", emotional);
        
        // Symptoms
        JSONObject symptoms = new JSONObject();
        symptoms.put("tic_count", log.getSymptoms().getTicCount());
        json.put("symptoms", symptoms);
        
        // Screen data
        JSONObject screen = new JSONObject();
        screen.put("screen_time_hours", log.getScreen().getScreenTimeHours());
        json.put("screen", screen);
        
        // Social data
        JSONObject social = new JSONObject();
        social.put("social_conflict", log.getSocial().isSocialConflict());
        json.put("social", social);
        
        // Custom factors
        JSONArray customArray = new JSONArray();
        for (CustomFactor factor : log.getCustomFactors()) {
            JSONObject factorJson = new JSONObject();
            factorJson.put("name", factor.getName());
            factorJson.put("level", factor.getLevel());
            factorJson.put("effect", factor.getEffect());
            customArray.put(factorJson);
        }
        json.put("custom", customArray);
        
        // Journal
        json.put("journal", log.getJournal());
        
        return json;
    }
    
    /**
     * Convert JSON object back to DailyLog
     */
    private DailyLog deserializeLog(JSONObject json) {
        DailyLog log = new DailyLog();
        
        // Date
        log.setDate(LocalDate.parse(json.getString("date")));
        
        // Physiological
        if (json.has("physiological")) {
            JSONObject physio = json.getJSONObject("physiological");
            log.getPhysiological().setSleepHours(
                physio.optDouble("sleep_hours", 8.0));
        }
        
        // Cognitive load
        if (json.has("cognitive_load")) {
            JSONObject cognitive = json.getJSONObject("cognitive_load");
            log.getCognitiveLoad().setStudyMinutes(
                cognitive.optInt("study_minutes", 0));
        }
        
        // Emotional
        if (json.has("emotional")) {
            JSONObject emotional = json.getJSONObject("emotional");
            log.getEmotional().setStress(
                emotional.optInt("stress", 0));
        }
        
        // Symptoms
        if (json.has("symptoms")) {
            JSONObject symptoms = json.getJSONObject("symptoms");
            log.getSymptoms().setTicCount(
                symptoms.optInt("tic_count", 0));
        }
        
        // Screen
        if (json.has("screen")) {
            JSONObject screen = json.getJSONObject("screen");
            log.getScreen().setScreenTimeHours(
                screen.optDouble("screen_time_hours", 0.0));
        }
        
        // Social
        if (json.has("social")) {
            JSONObject social = json.getJSONObject("social");
            log.getSocial().setSocialConflict(
                social.optBoolean("social_conflict", false));
        }
        
        // Custom factors
        if (json.has("custom")) {
            JSONArray customArray = json.getJSONArray("custom");
            for (int i = 0; i < customArray.length(); i++) {
                JSONObject factorJson = customArray.getJSONObject(i);
                CustomFactor factor = new CustomFactor(
                    factorJson.getString("name"),
                    factorJson.getInt("level"),
                    factorJson.getInt("effect")
                );
                log.addCustomFactor(factor);
            }
        }
        
        // Journal
        log.setJournal(json.optString("journal", ""));
        
        return log;
    }
    
    /**
     * Save all logs to file (overwrites existing file)
     */
    private boolean saveAllLogs(List<DailyLog> logs) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (DailyLog log : logs) {
                jsonArray.put(serializeLog(log));
            }
            
            // Write to file with pretty printing
            Files.writeString(Path.of(DATA_FILE), jsonArray.toString(2));
            
            System.out.println("✓ Saved " + logs.size() + " logs to " + DATA_FILE);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error saving logs to file: " + e.getMessage());
            return false;
        }
    }
    
    // === BACKUP & UTILITY METHODS ===
    
    /**
     * Create a timestamped backup of the current data file
     */
    public boolean createBackup() {
        try {
            String timestamp = LocalDate.now().toString();
            String backupFile = BACKUP_DIR + "myflow_backup_" + timestamp + ".json";
            
            Files.copy(Path.of(DATA_FILE), Path.of(backupFile), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("✓ Backup created: " + backupFile);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error creating backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the JSON array string for sending to Python API
     */
    public String getJsonForApi(List<DailyLog> logs) {
        JSONArray jsonArray = new JSONArray();
        for (DailyLog log : logs) {
            jsonArray.put(serializeLog(log));
        }
        return jsonArray.toString();
    }
    
    /**
     * Ensure data file exists (create empty array if not)
     */
    private void ensureDataFileExists() {
        Path filePath = Path.of(DATA_FILE);
        if (!Files.exists(filePath)) {
            try {
                Files.writeString(filePath, "[]");
                System.out.println("Created new data file: " + DATA_FILE);
            } catch (IOException e) {
                System.err.println("Error creating data file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Ensure backup directory exists
     */
    private void ensureBackupDirectoryExists() {
        Path backupPath = Path.of(BACKUP_DIR);
        if (!Files.exists(backupPath)) {
            try {
                Files.createDirectories(backupPath);
                System.out.println("Created backup directory: " + BACKUP_DIR);
            } catch (IOException e) {
                System.err.println("Error creating backup directory: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get statistics about stored data
     */
    public String getDataStats() {
        List<DailyLog> allLogs = loadAllLogs();
        if (allLogs.isEmpty()) {
            return "No data stored yet.";
        }
        
        LocalDate oldest = allLogs.get(0).getDate();
        LocalDate newest = allLogs.get(allLogs.size() - 1).getDate();
        
        return String.format(
            "Total Entries: %d | Date Range: %s to %s",
            allLogs.size(), oldest, newest
        );
    }
}