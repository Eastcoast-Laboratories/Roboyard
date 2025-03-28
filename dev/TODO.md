# TODO
- the back button for one move does not work properly, only the first move is moved back, than nothing happens any more
- when directly starting the load screen after app restart, loading crashes
- show a big 90% transparent blue robot in the background of the whole app
- walls on menu are not tiled 
- walls on level selection adding
- more stars in levels when depending on
 - How many hints were shown
 - Time needed to complete
 - Moves needed
 - Optimal moves from the solver
 - Number of different robots used
 - Number of squares surpassed
- only show the next level button when solved
- no hints button in level game
- auto save to history after 60 seconds
- win message and stop timer also in random game
- add sound effects for buttons
- add sound effect background for game
# Difficulty
- Beginner
  - show any puzzles with solutions with at least 4-6 moves
  - goals are always in corners with two walls
- Advanced
  - solutions with at least 6-8 moves
  - keep initial playing field when starting the next game
  - keep playing field when loading a saved game
  - three lines allowed in the same row/column
  - no multi-color target
- Insane mode
  - solutions with at least 10 moves
  - five lines allowed in the same row/column
- Impossible mode
  - solutions with at least 17 moves
  - five lines allowed in the same row/column

For future TalkBack enhancements, we could also:
    1. Add a preference setting to customize the order of information in TalkBack announcements
    2. Create custom button sounds to provide audio feedback for different actions
    3. Add a high-contrast mode for better visibility

# Level Design Editor
- Start durch einen **geheimen Knopf** im Level-Auswahl-Screen.
- Auswahl eines der **140 Level** zur Bearbeitung.
- **Bearbeitung über Buttons** für:
  - **Vier targets**  
  - **Vier Roboter (aktuell, aber zukunftssicher für mehr Roboter)**  
  - **Wände**  
- **Speicherung:**  
  - Wahl zwischen **Überschreiben** des Levels oder **Hinzufügen als neuen Level** 141 - ...
  - Speicherung erfolgt im **gleichen Format** wie die bestehenden Levels 1 bis 140.  
- **Vorschau-Funktion** für gespeicherte Levels im selben Level speicher Format als txt

---

merke dir:
- Bei allen entwicklungen berücksichtigen, das wir ev. einmal **Mehr als vier Roboter** in der Zukunft haben könnten.  
- Java-Code beachten, dass wir ev. eine **Umstellung auf Kotlin** planen.  

---

# Accessibility-Verbesserungen
- In den Settings: Konfigurationsvariable für Accessibility-Modus auch Unabhängig davon, ob TalkBack aktiv ist.  
- Design-Änderungen:  
  - Dunklerer Hintergrund für den Accessibility-Bereich.  
  - Grüne Farben aus der Git-Historie abrufen und als Konstanten definieren.  
- Bewegungsprobleme in Accessibility Controls beheben:
 - das ist ein Positionierungsfehler in der Methode, wenn man durch den accessibility modus die roboter bewegt.  bitte fixen:  
  - Senkrechte Wände werden z.z. 1 Feld zu weit rechts angezeigt.  
  - Waagerechte Wände werden z.z. 1 Feld zu weit oben angezeigt.  
- Spielstart-Problem mit Screenreader lösen:  
  - Beim Start wird das Game Board Grid z.z. automatisch selektiert und vorgelesen, nicht selektieren, denn Dadurch wird die Erklärung der Roboter-Positionen unterbrochen.  
  - eine Möglichkeit schaffen, diese Ansage erneut abzurufen.  
- Roboterbewegung sichtbar machen:  
  - Aktuell sind Roboterbewegungen im echten spielfeld, bei Steuerung durch die Accessibility Controls nicht sichtbar - die roboter bleiben z.z. an ihrer position stehen. die sollen aber auch durch die accessibility rihtungs knöpfe in echt bewegt ewrden.
  - Behebung: Sichtbare Bewegung der Roboter beim Drücken der Buttons. 

# spaeter
- TalkBack-Unterstützung sprache einstellbar machen (Englisch ermöglichen).  

---


---

# Sonstige Features & Fixes
- Hinweis-Textbox immer sichtbar (auch wenn leer) + dicker Rand.  
- Rückgängig-Funktion ("Back"-Methode) reparieren:  
  - im Moment Geht nur so viele Schritte zurück, wieviel verschiedene Roboter bereits bewegt wurden, also pro roboter auswahl geht ein back schritt verloren im moment.  
- bei 8x8 boards, immer genau 4 wände an der aussenseite positionieren

