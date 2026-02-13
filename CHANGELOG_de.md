Changelog Deutsch
=================


## Version 38 (2026-02-13)
- Live Move Counter Button, der die optimale Anzahl der Züge von der aktuellen Position zeigt


## Version 37 (2026-02-13)
- Dynamische Roboterkollisionsgeräusche - die Tonhöhe ändert sich je nach beteiligten Roboter
- Verbesserte Sichtbarkeit des grünen Ziels im Hohen Kontrast Modus
- Anfänger-Levels überarbeitet: Der User wird nun in alle wichtigen Spieltaktiken hineingeführt 

## Version 36 (2026-02-11)
- bei Gespeicherten Spielen findet die K.I. wieder Lösungen
- In der History werden alle Karten nach 30s gespeichert
- Bugfixes


## Version 35 (2026-02-10)
- Schwierigkeitsanpassungen für viele Levels
- Mehr Feinschliff für Anfänger-Levels
- die ersten 10 abgeschlossenen Levels geben jetzt immer mindestens einen Stern
- Landschaftsmodus nutzt jetzt den Bildschirm besser
- Accessibility-Steuerung für blinden Spieler kompakter gestaltet


## Version 34 (2026-02-07)
- Achievements
- Login-Streaks
- Dynamische Roboter-Kollisionsgeräusche - die Tonhöhe ändert sich basierend auf den beteiligten Robotern
- Zuletzt gespieltes Level wird in der Levelauswahl hervorgehoben
- Menü-Button führt in Level-Spielen jetzt zurück zur Levelauswahl statt zum Hauptmenu
- Fehler behoben, der ungültige Lösungswege vorschlug
- Die Sprachausgaben bei der Steuerung über Barrierefreiheits-Buttons wurde optimiert
- Synchronisation des Spielfortschritts und Achievements mit dem online server


## Version 33 - 31
- unreleased

## Version 30 (2026-01-19)
- Die maximale und minimale Anzahl Züge ist jetzt beliebig einstellbar
- Das mehrfarbige Ziel ist jetzt einstellbar
- Neuer Hoher-Kontrast-Modus
- Verbesserte Schriftgrößen für unterschiedliche Displays
- Ein Schließen-Button im Info-Display in der Spielanzeige um zu verhindern, dass es die Spielknöpfe überlappt
- Fix: alle Levels werden jetzt korrekt gelöst
- Fix: Solver startet jetzt korrekt nach Laden eines Spielstands


## Version 29 (2025-09-29)
- Landscape-Modus hinzugefügt mit Toggle-Button für die Ausrichtung des Spielfeldes
- Fix: Accessibility Nachrichten korrigiert, wo das Ziel ein Feld daneben angekündet wurde
- UI verbessert mit modernen Material Design Elementen

## Version 28 (2025-08-07)
- Neuer extra Hinweis, welche Roboter in der Lösung eine Rolle spielen.
- Überarbeitete UI für verschiedene Display-Größen


## Version 27 (2025-06-25)
- mehrfarbiges Ziel wieder aktiviert
- Ansagen für blinde Spieler repariert (Talkback) und optimiert


## Version 26 (2025-05-11)
- Verbesserte Accessibility-Übersetzungen
- Alle Koordinaten in der Accessibility-Announcement von 1,1 bis 8,8 statt von 0,0 bis 7,7
- Sound-Einstellung funktioniert jetzt korrekt
- Gewinnbedingung bei mehreren Zielen prüft jetzt korrekt, ob die gewählte Anzahl an Robotern auf ihren Zielen steht
- Mehrfarben-Ziele wiederhergestellt im Beginner Mode

## Version 25 (2025-05-03)
- Fullscreen Option in Settings
- Verbesserte swipe-to-move Steuerung
- 5 Roboter aus externen Maps erlaubt
- Fix: fehlende rechtwinklige Wände an den Rändern auf 8x8 und 8x12 Maps


## Version 24 (2025-04-23)
- Mehrsprachigkeit hinzugefügt: Deutsch, Spanisch, Französisch, Chinesisch und Koreanisch
- Automatische Auswahl des Roboters in der Zielfarbe beim Spielstart
- Verbesserte Barrierefreiheit:
  - Steuerungselemente im Accessibility-Modus bleiben jetzt immer sichtbar
  - Fehler bei der Erkennung von Wandpositionen in der Sprachausgabe behoben
  - mehr Sprachausgaben
- Fehlende Außenwände im Süden und Osten wurden behoben
- Roboterpfade werden beim Start eines neuen Spiels zurückgesetzt
- Squares Moved wird bei neuem Spiel zurückgesetzt
- Roboteranimationen sind jetzt schneller
- Den nächsten Hint nur anzeigen, wenn der Roboter den passenden Zug gemacht hat
- Verhindern von Karten mit Lösung in nur einem Zug
- Neuer Multi-Target Mode (beta) in Settings
- Im Save Screen wird nun die Kartengröße und der Spielfortschritt gespeichert.
- Im Fortgeschrittenen Modus werden nun Lösungen mit mindestens 6-10 Zügen erwartet
- Share button: Man kann Karten mit Lösungen teilen
- Man kann das Spiel von externen Seiten starten mit dem URL Schema 'roboyard://'


## Version 23 (2025-04-07)
- Roboter bewegen sich jetzt flüssiger und natürlicher
- Die Pfade, die Roboter gelaufen sind, werden jetzt angezeigt
- Optimal-Move-Button erscheint eher in den Hints
- Fehler bei einigen Geräten behoben im Einstellungsbildschirm
- Share Button im Save Screen


## Version 22 (2025-04-05)
- Behobene TalkBack-Ankündigungen für blinde Spieler mit korrigierten Wandpositionen.
- Roboter hinterlassen jetzt farbige Spuren.
- Neue Option, um die gleichen Wände auf einem Spielbrett über Neustarts hinweg zu erhalten.
- Verbesserte Schriftgrößen.
- Neuer Hintergrund im Hauptmenu


## Version 21 (2025-04-03)
- Überarbeitete Benutzeroberfläche mit klareren Icons und besserer Lesbarkeit
- Vollständige unterstützung des Accessibility Modus für blinde Spieler mit TalkBack Unterstützung
- Neuer Spielmodus: bis zu 4 Targets in einem Spiel
- Neue Tap-to-move-Methode, um Roboter schneller zu bewegen
- Level Game Auswahl-Screen überarbeitet
- Neue Boardgrößen: 8x8, 8x12, 10x10, 10x12, 10x14


## Version 20.1 (2025-03-17)
- Gespeicherte Spiele werden nun sofort im Ladebildschirm als Minimap angezeigt.
- Performance-Optimierungen: Roboter bewegen sich jetzt schneller.
- Roboter stoppen ihre Bewegungen mit einem „Feder“-Effekt am Ende.
- Korrekte Z-Reihenfolge für Wände und Roboter


## Version 20
- Es gibt jetzt einen Cancel button, wenn die KI länger als 10s an der lösung rechnet
- Spielhistorie wird nun automatisch gespeichert, bevor der Speichern-Bildschirm geöffnet wird.
- Speicher- und Ladebuttons sind nun getrennt.
- Der „Speichern“-Button im Spielbildschirm wird immer wieder aktiviert.
- Ein Lade-Indikator wird nun im Spielfeld angezeigt, wenn die KI rechnet.
- Option in den Einstellungen, ob jedes mal eine neue Karte generiert wird
- Sound Effekte für Roboterbewegungen

### Version 16

- Verhindert das Einfrieren des Solvers bei schnellem Klicken, während die Hinweisnachricht angezeigt wird oder der Solver läuft.

### Version 15

- Neues Design für die Roboter, Ziele und das Spielfeld
- Zusatzliche Berücknungstoleranz, wenn alle Roboter mehr als ein Quadrat voneinander entfernt sind

### Version 14

- Level-Games repariert
- Eindeutige Zeichenfolge über jedem Spielstand und eine eindeutige Hintergrundfarbe hinzugefügt

### Version 13.2

- `distributionSha256Sum` zu den Build-Optionen hinzugefügt

### Version 13.1

- Nicht benötigte INTERNET permission entfernt

### Version 13

- Beim Anzeigen der Lösung die verschiedenen Lösungen durchlaufen, wenn mehr als eine Lösung gefunden wurde

### Version 12

- Lock Screen disabled

### Version 10.3

- Link zum Impressum und Datenschutz ergänzt
- Upgrade auf SdkVersion 34

### Version 10.2

- Zeigt die Anzahl der verschiedenen Lösungen an, die von der KI gefunden wurden

### Version 11 (beta)

- Der Solver in dieser Version ist fehlerhaft! Benutzung auf eigene Gefahr! ;)
- Beim Anzeigen der Lösung die verschiedenen Lösungen durchlaufen, wenn mehr als eine Lösung gefunden wurde

### Version 10.1

- Grüner Roboter geschrumpft, damit er nicht mehr die Wände bedeckt
- Weiße Kreise im Hintergrund von Robotern entfernt

### Version 9.0

- Impossible-Modus mit mindestens 17 Zügen hinzugefügt
- Beginnerlevels können maximal nur eine Sekunde dauern, um sie zu berechnen
- Fix: Leveleinstellung wurde nicht gespeichert, wenn der Beginner-Modus ausgewählt wurde
- Fix: LevelGame-Auswahl wird nicht mehr neu generiert, kann also jetzt gelöst werden
- Standardstufe ist jetzt "Beginner"

### Version 8.1

- Große Popup-Nachrichten wieder nach unten verschoben
- Neues Launcher-Symbol
- Fehler behoben bei Rätseln mit dem Ziel in direkter Linie des Roboters

### Version 8.0

- Es wird jetzt die Anzahl der gezogenen Felder neben der Anzahl der Züge angezeigt
- Richtungspfeile halb transparent

### Version 7.1

- Neues Launcher-Symbol

### Version 7.0

- Angepasste Auflösung an Android 4.1.1 mit 480px Breite

### Version 6.1

- Sound ein/aus in den Spieleinstellungen hinzugefügt (Symbole von freeiconspng [1] (https://www.freeiconspng.com/img/40963), [2] (https://www.freeiconspng.com/img/40944) )
- Roboyard in der Mitte des Spielfelds hinzugefügt

### Version 6.0

- Lösung als 2. bis 5. Hinweis anzeigen
- Einstellungen dauerhaft speichern
- Der (langsamere) BFS-Solver-Algorithmus entfernt
- Fehler behoben, der beim Starten eines neuen Levels zusätzliches automatisches gespeichern verursachte

### Version 5.4

- Mehr Toleranz gegenüber dem Berühren eines Roboters hinzugefügt

### Version 5.3

- Hintergrundgeräusche hinzugefügt
- Grüne Wände sind jetzt eher wie Gartenhecken
- Wände auf dem rechten Bildschirm sind jetzt sichtbar
- Im Beginnerlevel jedes Mal eine neue Karte generieren

### Version 5.2

- In Roboyard umbenennen
- Wände sind grün und etwas dicker
- Anfängliche Bewegungsgeschwindigkeit von Robotern erhöht, mit linearer Verlangsamung

### Version 5.1

- Carré immer wieder in der Mitte (fixt fehlerhafte Roboterpositionen durch Beibehaltung des ursprünglichen Spielfeldes)

### Version 5.0

- Beim Start des nächsten Spiels das anfängliche Spielfeld beibehalten
- Spielfeld beibehalten, wenn ein gespeichertes Spiel geladen wird

### Version 4.0

- Mehr Komplexität zu Advanced und Insane Level hinzugefügt

Neu in Advanced:

- Das Quadrat darf nicht in der Mitte sein
- Drei Wände in derselben Zeile / Spalte erlaubt
- Kein mehrfarbiges Ziel

Neu in Insane:

- Lösungen mit mind. 10 Zügen reichen aus
- 50% Chance, dass das Ziel irgendwo auf der Karte statt in einer Ecke steht

### Version 3.2

- An unterschiedliche Bildschirmauflösungen anpassen

### Version 3.1

- Kugeln sind jetzt Roboter
- Schaltfläche zum nächsten Spiel ändern

### Version 3.0

- Neues Design

### Version 2.5

- 35 Spielstände und Level pro Seite
- Automatisches Speichern des aktuellen Spiels nach 40s in Speicherslot 0

### Version 2.4

- Fehler behoben: kein Speichern-Button beim Spielen eines gespeicherten Spiels (das Spiel stürzte ab)

### Version 2.3

- Einstellungen: Benutzerlevel so einstellen, dass nur Rätsel mit Mindestanzahl Zügen angezeigt werden
  - Beginner: 4-6 Züge
  - Advanced: 6-8 Züge
  - Insane: 14 Züge (10 Züge seit v4.0)
- Warnung, wenn auf langsames BFS und Insane Level eingestellt

### Version 2.2

- Zeige 3 bis 5 Hinweise, bevor die optimale Lösung gezeigt wird

### Version 1.0

- Letzte französische Version

# Dies sind alle relevanten Änderungen seit Version 1.0:

- Impossible-Modus mit mindestens 17 Zügen hinzugefügt
- Fehler behoben bei Rätseln mit dem Ziel in direkter Linie des Roboters
- Es wird jetzt die Anzahl der gezogenen Felder neben der Anzahl der Züge angezeigt
- Neues Launcher-Symbol
- Angepasste Auflösung an Android 4.1.1 mit 480px Breite
- Ton in den Spieleinstellungen an/aus hinzugefügt
- Lösung als 2. bis 5. Hinweis anzeigen
- Einstellungen dauerhaft speichern
- Entfernen Sie den (langsameren) BFS-Solver-Algorithmus
- Mehr Toleranz gegenüber dem Berühren eines Roboters hinzugefügt
- Hintergrundgeräusche hinzugefügt
- Wände sind grüne Hecken (besser sichtbar)
- Anfängliche Bewegungsgeschwindigkeit von Robotern erhöht, mit linearer Verlangsamung
- Beginner-, Advanced-, Insane- und Impossible-Modus mit Mindestanzahl Züge für die Lösungen erlaubt
- Kugeln sind jetzt Roboter
- 35 Spielstände und Level pro Seite
- Automatisches Speichern des aktuellen Spiels nach 40s in Speicherslot 0
- Fehler behoben: kein Speichern-Button beim Spielen eines gespeicherten Spiels (das Spiel stürzte ab)
