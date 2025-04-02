# Verbessertes Präferenzmanagement für Roboyard

## Problembeschreibung

Die aktuelle Implementierung des Präferenzmanagements in Roboyard weist folgende Probleme auf:

1. **Redundanz**: Jede Klasse, die auf Präferenzen zugreifen möchte, muss eine neue `Preferences`-Instanz erstellen
2. **Kontextabhängigkeit**: Es wird immer ein gültiger `Context` benötigt, was zu Fehlern führen kann
3. **Fehleranfälligkeit**: Jeder Zugriff erfordert Fehlerbehandlung (null-Checks, NumberFormatExceptions)
4. **Inkonsistenz**: Verschiedene Teile der Anwendung könnten unterschiedliche Werte verwenden
5. **Wartbarkeit**: Änderungen an Präferenzschlüsseln müssen an mehreren Stellen vorgenommen werden

## Lösungskonzept: Singleton-Präferenzmanager

### Architektur

Ein Singleton-Pattern für den Präferenzmanager bietet folgende Vorteile:

- **Globaler Zugriff**: Von überall im Code ohne Context-Parameter zugänglich
- **Zentralisierung**: Einmalige Initialisierung beim App-Start
- **Typsicherheit**: Stark typisierte Getter und Setter statt String-basierter Zugriffe
- **Standardwerte**: Zentrale Definition von Standardwerten
- **Caching**: Werte können im Speicher gehalten werden für schnelleren Zugriff

### Implementierungsdetails

```java
public class AppPreferences {
    private static AppPreferences instance;
    private SharedPreferences prefs;
    
    // Private Konstruktor verhindert direkte Instanziierung
    private AppPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("roboyard_prefs", Context.MODE_PRIVATE);
    }
    
    // Initialisierung beim App-Start
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
    }
    
    // Globaler Zugriffspunkt
    public static AppPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppPreferences not initialized. Call init() first.");
        }
        return instance;
    }
    
    // Typsichere Getter und Setter für jede Präferenz
    
    // Zielanzahl (1-4)
    public int getTargetCount() {
        return prefs.getInt("target_count", 1); // Default ist 1
    }
    
    public void setTargetCount(int count) {
        // Validierung
        int validCount = Math.max(1, Math.min(4, count));
        prefs.edit().putInt("target_count", validCount).apply();
    }
    
    // Weitere Präferenzen hier hinzufügen...
    public boolean getSoundEnabled() {
        return prefs.getBoolean("sound_enabled", true);
    }
    
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply();
    }
    
    // Weitere Methoden für andere Präferenzen
}
```

### Integration

1. **Initialisierung**: In der `Application`-Klasse oder im `MainActivity.onCreate()`:
   ```java
   AppPreferences.init(getApplicationContext());
   ```

2. **Verwendung**: In jeder Klasse ohne Context-Abhängigkeit:
   ```java
   int targetCount = AppPreferences.getInstance().getTargetCount();
   ```

3. **Migration**: Schrittweise Ersetzung der alten `Preferences`-Aufrufe durch die neue API

## Vorteile

1. **Vereinfachter Code**: Keine redundanten Instanziierungen und Context-Übergaben
2. **Verbesserte Typsicherheit**: Keine String-basierten Schlüssel mehr im Code verstreut
3. **Zentrale Wartung**: Änderungen an Präferenzen nur an einer Stelle nötig
4. **Konsistenz**: Garantiert einheitliche Werte in der gesamten Anwendung
5. **Leichtere Testbarkeit**: Mocking des Präferenzmanagers für Tests möglich

## Migration

Die Migration kann schrittweise erfolgen:

1. Implementierung der `AppPreferences`-Klasse
2. Initialisierung in der Application-Klasse
3. Schrittweise Ersetzung der alten Präferenzaufrufe
4. Entfernung der alten `Preferences`-Klasse, wenn nicht mehr verwendet

## Kompatibilität mit zukünftiger Kotlin-Migration

Dieses Design ist kompatibel mit einer zukünftigen Migration zu Kotlin:

- Kann leicht zu einem Kotlin-Objekt (`object`) umgewandelt werden
- Unterstützt Kotlin-Properties mit getters/setters
- Kann mit Kotlin-Erweiterungsfunktionen erweitert werden

## Nächste Schritte

1. Implementierung der `AppPreferences`-Klasse
2. Anpassung der Initialisierungslogik in der Application-Klasse
3. Migration eines ersten Features (z.B. target_count) zur Validierung
4. Schrittweise Migration weiterer Präferenzen
