package roboyard.eclabs;

import android.content.Context;
import android.content.SharedPreferences;

import timber.log.Timber;

/**
 * Singleton-Klasse fu00fcr das Pru00e4ferenzmanagement in Roboyard.
 * Bietet typsicheren Zugriff auf Pru00e4ferenzen ohne Notwendigkeit eines Context-Parameters
 * bei jedem Aufruf.
 */
public class AppPreferences {
    private static final String PREFS_NAME = "RoboYard";
    private static AppPreferences instance;
    private final SharedPreferences prefs;
    
    // Preference keys
    private static final String KEY_ROBOT_COUNT = "robot_count";
    private static final String KEY_TARGET_COLORS = "target_colors";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_BOARD_SIZE_WIDTH = "board_width";
    private static final String KEY_BOARD_SIZE_HEIGHT = "board_height";
    
    // Default values
    private static final int DEFAULT_TARGET_COUNT = 1;
    private static final int DEFAULT_TARGET_COLORS = 4;
    private static final boolean DEFAULT_SOUND_ENABLED = true;
    private static final int DEFAULT_DIFFICULTY = 3;
    private static final int DEFAULT_BOARD_SIZE_WIDTH = 16;
    private static final int DEFAULT_BOARD_SIZE_HEIGHT = 16;
    
    // Private Konstruktor verhindert direkte Instanziierung
    private AppPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Initialisiert die AppPreferences-Instanz.
     * Muss beim App-Start aufgerufen werden, z.B. in Application.onCreate() oder MainActivity.onCreate().
     * 
     * @param context Der Anwendungskontext
     */
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
            Timber.d("AppPreferences initialized");
        }
    }
    
    /**
     * Gibt die Singleton-Instanz zuru00fcck.
     * Wenn die Instanz noch nicht initialisiert wurde, wird versucht, sie mit dem ApplicationContext zu initialisieren.
     * 
     * @return Die AppPreferences-Instanz
     */
    public static AppPreferences getInstance() {
        if (instance == null) {
            try {
                // Versuche, den ApplicationContext zu bekommen, wenn mu00f6glich
                Context applicationContext = null;
                try {
                    // Versuche, den Anwendungskontext u00fcber die MainActivity zu bekommen
                    applicationContext = roboyard.ui.activities.MainActivity.getAppContext();
                } catch (Exception e) {
                    Timber.e(e, "Could not get application context from MainActivity");
                }
                
                if (applicationContext != null) {
                    init(applicationContext);
                } else {
                    throw new IllegalStateException("AppPreferences not initialized. Call init() first.");
                }
            } catch (Exception e) {
                throw new IllegalStateException("AppPreferences not initialized. Call init() first.", e);
            }
        }
        return instance;
    }
    
    /**
     * Gibt die Anzahl der Ziele zuru00fcck.
     * 
     * @return Anzahl der Ziele (1-4)
     */
    public int getRobotCount() {
        int count = prefs.getInt(KEY_ROBOT_COUNT, DEFAULT_TARGET_COUNT); // Default ist 1
        Timber.d("[TARGET COUNT] AppPreferences.getRobotCount(): %d", count);
        return count;
    }
    
    /**
     * Setzt die Anzahl der Ziele.
     * 
     * @param count Anzahl der Ziele (wird auf 1-4 begrenzt)
     */
    public void setRobotCount(int count) {
        // Validierung
        int validCount = Math.max(1, Math.min(4, count));
        prefs.edit().putInt(KEY_ROBOT_COUNT, validCount).apply();
        Timber.d("[TARGET COUNT] AppPreferences.setRobotCount(): %d", validCount);
    }
    
    /**
     * Gibt die Anzahl der verschiedenen Zielfarben zurück.
     * 
     * @return Anzahl der verschiedenen Zielfarben (1-4)
     */
    public int getTargetColors() {
        return prefs.getInt(KEY_TARGET_COLORS, DEFAULT_TARGET_COLORS);
    }
    
    /**
     * Setzt die Anzahl der verschiedenen Zielfarben.
     * 
     * @param count Anzahl der verschiedenen Zielfarben (1-4)
     */
    public void setTargetColors(int count) {
        // Ensure count is within valid range
        count = Math.max(1, Math.min(4, count));
        prefs.edit().putInt(KEY_TARGET_COLORS, count).apply();
    }
    
    /**
     * Pru00fcft, ob Sound aktiviert ist.
     * 
     * @return true wenn Sound aktiviert ist, sonst false
     */
    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED);
    }
    
    /**
     * Aktiviert oder deaktiviert Sound.
     * 
     * @param enabled true um Sound zu aktivieren, false um ihn zu deaktivieren
     */
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    /**
     * Gibt die Spielschwierigkeit zuru00fcck.
     * 
     * @return Schwierigkeitsgrad (1-5)
     */
    public int getDifficulty() {
        return prefs.getInt(KEY_DIFFICULTY, DEFAULT_DIFFICULTY); // Default ist mittlere Schwierigkeit
    }
    
    /**
     * Setzt die Spielschwierigkeit.
     * 
     * @param difficulty Schwierigkeitsgrad (1-5)
     */
    public void setDifficulty(int difficulty) {
        // Validierung
        int validDifficulty = Math.max(1, Math.min(5, difficulty));
        prefs.edit().putInt(KEY_DIFFICULTY, validDifficulty).apply();
    }
    
    /**
     * Gibt die Brettgru00f6u00dfe zuru00fcck.
     * 
     * @return Array mit [Breite, Hu00f6he]
     */
    public int[] getBoardSize() {
        int width = prefs.getInt(KEY_BOARD_SIZE_WIDTH, DEFAULT_BOARD_SIZE_WIDTH);
        int height = prefs.getInt(KEY_BOARD_SIZE_HEIGHT, DEFAULT_BOARD_SIZE_HEIGHT);
        return new int[]{width, height};
    }
    
    /**
     * Setzt die Brettgru00f6u00dfe.
     * 
     * @param width Breite des Spielbretts
     * @param height Hu00f6he des Spielbretts
     */
    public void setBoardSize(int width, int height) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BOARD_SIZE_WIDTH, width);
        editor.putInt(KEY_BOARD_SIZE_HEIGHT, height);
        editor.apply();
        Timber.d("[BOARD_SIZE_DEBUG] AppPreferences.setBoardSize(): %dx%d", width, height);
    }
    
    /**
     * Kompatibilitu00e4tsmethode fu00fcr die alte Preferences-API.
     * Sollte nur wu00e4hrend der Migration verwendet werden.
     * 
     * @param key Pru00e4ferenzschlu00fcssel
     * @return Wert als String oder leerer String wenn nicht gefunden
     */
    public String getPreferenceValue(String key) {
        return prefs.getString(key, "");
    }
    
    /**
     * Kompatibilitu00e4tsmethode fu00fcr die alte Preferences-API.
     * Sollte nur wu00e4hrend der Migration verwendet werden.
     * 
     * @param key Pru00e4ferenzschlu00fcssel
     * @param value Zu speichernder Wert
     */
    public void setPreferenceValue(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
}
