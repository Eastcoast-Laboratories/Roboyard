# Google Play Games Services Setup Guide

Diese Anleitung erklärt, wie du Google Play Games Services (PGS) für Roboyard in der Google Play Console einrichtest.

## Voraussetzungen

- Google Play Developer Account
- App bereits in der Google Play Console erstellt
- SHA-1 Fingerprint deines Signing Keys

## Schritt 1: Play Games Services aktivieren

1. Öffne die [Google Play Console](https://play.google.com/console)
2. Wähle deine App "Roboyard"
3. Navigiere zu: **Grow > Play Games Services > Setup and management > Configuration**
4. Klicke auf **Set up Play Games Services**
5. Wähle **No, my game doesn't use Google APIs** (falls noch nicht eingerichtet)
6. Gib einen Namen für dein Spiel ein (z.B. "Roboyard")

## Schritt 2: OAuth 2.0 Client-ID erstellen

### Was ist Google Cloud Console?

Die **Google Cloud Console** ist Googles zentrale Verwaltungsplattform für alle Cloud-Services und APIs. Für Play Games Services wird sie benötigt, um:
- OAuth 2.0 Credentials zu erstellen (für die Authentifizierung deiner App)
- Die Verknüpfung zwischen deiner App und Play Games Services herzustellen
- Berechtigungen und Zugriff zu verwalten

**Wichtig**: Für Roboyard brauchst du **kein kostenpflichtiges Google Cloud-Konto**! 

- **Kostenlos**: Neue Google Cloud-Konten erhalten **$300 kostenlosen Credit** für 90 Tage
- **Danach**: Play Games Services ist kostenlos - es fallen nur Kosten an, wenn du zusätzliche Cloud-Services nutzt (z.B. Cloud Firestore für Leaderboards)
- **Für Roboyard**: Nur die OAuth 2.0 Credentials werden benötigt - das ist kostenlos

**Was wird in der Cloud gespeichert?**
- Nur die **OAuth 2.0 Client-ID** (ein Identifikator für deine App)
- Die **SHA-1 Fingerprints** deines Signing Keys (für Sicherheit)
- Keine Spielerdaten oder Achievements - diese werden von Play Games Services verwaltet

### In der Google Cloud Console:

1. Öffne die [Google Cloud Console](https://console.cloud.google.com)
2. Falls noch nicht vorhanden: Erstelle ein neues Projekt
   - Klicke auf **Projekt auswählen** oben links
   - Klicke auf **NEUES PROJEKT**
   - Gib einen Namen ein (z.B. "Roboyard")
3. Wähle dein Projekt aus
4. Navigiere zu: **APIs und Dienste > Anmeldedaten**
5. Klicke auf **Anmeldedaten erstellen > OAuth-Client-ID**
6. Falls noch nicht konfiguriert: Konfiguriere den **OAuth-Zustimmungsbildschirm**
   - Wähle **Extern** als Nutzertyp
   - Fülle die erforderlichen Felder aus (App-Name, Support-E-Mail, etc.)
7. Zurück zu Anmeldedaten: Klicke auf **Anmeldedaten erstellen > OAuth-Client-ID**
8. Wähle **Android** als Anwendungstyp
9. Gib folgende Informationen ein:
   - **Name**: Roboyard Android Client
   - **Paketname**: `roboyard.eclabs`
   - **SHA-1-Zertifikat-Fingerabdruck**: (siehe unten)

### SHA-1 Fingerprint ermitteln:

```bash
# Für Debug-Keystore:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Für Release-Keystore:
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

## Schritt 3: Achievements automatisch importieren

### Automatisierter Bulk Import (empfohlen)

Die Bulk-Import-Dateien sind bereits generiert und enthalten:

```
dev/pgs_import/
├── AchievementsMetadata.csv          (67 Achievements mit Metadaten)
├── AchievementsLocalizations.csv     (Deutsche Übersetzungen)
├── AchievementsIconMappings.csv      (Icon-Zuordnungen)
└── achievements_bulk_import.zip      (Fertig zum Upload)
```

### Import in Play Console

1. Gehe zu: **Play Console > Grow > Play Games Services > Achievements**
2. Klicke auf **Import achievements**
3. Lade die Datei hoch: `dev/pgs_import/achievements_bulk_import.zip`
4. Überprüfe die Vorschau (sollte 67 Achievements zeigen)
5. Klicke **Save as draft**

**Das ZIP-Datei enthält automatisch:**
- ✅ Alle 67 Achievement-Namen (Englisch)
- ✅ Alle 67 Beschreibungen (Englisch)
- ✅ Deutsche Übersetzungen für alle Achievements
- ✅ Icon-Mappings (generisch - können später angepasst werden)

### Was bleibt noch zu tun?

Nach dem Import in Play Console:

1. **Icons hochladen** (134 Aktionen)
   - Für jedes Achievement: Icon (512x512 PNG/JPEG)
   - Du kannst die Icons später hinzufügen oder ein Script schreiben, das sie automatisch hochlädt

2. **Projekt-Metadaten** (4 Aktionen)
   - Beschreibung
   - Kategorie
   - Symbol
   - Vorstellungsgrafik

3. **Achievement IDs kopieren**
   - Nach dem Import erhält jedes Achievement eine eindeutige ID (z.B. `CgkI...`)
   - Diese IDs werden in `app/src/main/res/values/games-ids.xml` eingetragen

## Schritt 4: Achievement IDs aus Play Console

✅ **Status:** Die 67 Achievement-IDs sind bereits vollständig aus der Play Console importiert und in `app/src/main/res/values/games-ids.xml` eingetragen.

### Dateistruktur

Die Datei enthält alle Achievement-IDs mit `pgs_` Präfix:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Google Play Games Services App ID -->
    <string name="app_id" translatable="false">525205624802</string>
    <string name="package_name" translatable="false">de.z11.roboyard</string>
    
    <!-- Achievement IDs (67 total) -->
    <string name="pgs_first_game" translatable="false">CgkI4q_HxaQPEAIQAQ</string>
    <string name="pgs_level_1_complete" translatable="false">CgkI4q_HxaQPEAIQCQ</string>
    <string name="pgs_level_10_complete" translatable="false">CgkI4q_HxaQPEAIQAg</string>
    <!-- ... weitere 64 Achievements ... -->
</resources>
```

### Naming-Konvention

- **In `games-ids.xml`:** `pgs_` Präfix (Play Games Services IDs)
- **In `strings.xml`:** `achievement_` Präfix (lokale Achievement-Namen und Beschreibungen)
- **In `PlayGamesManager.java`:** Mapping von lokal zu `pgs_` IDs

Dies verhindert Duplikate zwischen den Dateien.

## Schritt 5: Tester hinzufügen

1. In der Play Console: **Grow > Play Games Services > Setup and management > Testers**
2. Klicke auf **Add testers**
3. Füge E-Mail-Adressen der Tester hinzu
4. Tester können Achievements testen, bevor sie veröffentlicht werden

## Schritt 6: Play Games Services veröffentlichen

**WICHTIG**: Play Games Services muss separat von der App veröffentlicht werden!

1. In der Play Console: **Grow > Play Games Services > Setup and management > Publishing**
2. Überprüfe alle Einstellungen
3. Klicke auf **Publish**

⚠️ **Hinweis**: Nach der Veröffentlichung können Achievements nicht mehr gelöscht werden!

## Schritt 7: App-Konfiguration

### build.gradle (app)

```gradle
android {
    defaultConfig {
        // ...
    }
    
    buildTypes {
        debug {
            buildConfigField "boolean", "ENABLE_PLAY_GAMES", "true"
        }
        release {
            buildConfigField "boolean", "ENABLE_PLAY_GAMES", "true"
        }
    }
    
    // Optional: Build Flavors für F-Droid
    flavorDimensions "store"
    productFlavors {
        playstore {
            dimension "store"
            buildConfigField "boolean", "ENABLE_PLAY_GAMES", "true"
        }
        fdroid {
            dimension "store"
            buildConfigField "boolean", "ENABLE_PLAY_GAMES", "false"
        }
    }
}

dependencies {
    // Google Play Games Services
    implementation 'com.google.android.gms:play-services-games-v2:20.1.2'
}
```

### AndroidManifest.xml

```xml
<application>
    <!-- Google Play Games Services App ID -->
    <meta-data
        android:name="com.google.android.gms.games.APP_ID"
        android:value="@string/app_id" />
</application>
```

## Fehlerbehebung

### "Sign-in failed" Fehler

1. Überprüfe, ob der SHA-1 Fingerprint korrekt ist
2. Stelle sicher, dass der Package Name übereinstimmt
3. Prüfe, ob der Tester zur Testerliste hinzugefügt wurde

### Achievements werden nicht angezeigt

1. Stelle sicher, dass Play Games Services veröffentlicht ist
2. Überprüfe, ob die Achievement IDs korrekt sind
3. Teste mit einem Tester-Account

### "API not enabled" Fehler

1. Gehe zur Google Cloud Console
2. Aktiviere die "Google Play Games Services" API

## Nächste Schritte

Nach der Einrichtung in der Play Console:

1. Erstelle die `games-ids.xml` Datei mit den Achievement IDs
2. Implementiere den `PlayGamesManager` (bereits im Code vorbereitet)
3. Teste mit einem Tester-Account
4. Veröffentliche die App mit Play Games Services Integration

## Ressourcen

- [Google Play Games Services Dokumentation](https://developer.android.com/games/pgs)
- [Achievements Integration Guide](https://developer.android.com/games/pgs/android/achievements)
- [Play Console Help](https://support.google.com/googleplay/android-developer/answer/2990418)
