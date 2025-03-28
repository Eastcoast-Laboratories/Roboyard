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

# Accessibility-Verbesserungen
- TalkBack-Unterstützung sprache einstellbar machen (Englisch ermöglichen).  
1. Add a preference setting to customize the order of information in TalkBack announcements
2. Create custom button sounds to provide audio feedback for different actions
3. Add a high-contrast mode for better visibility


# Sonstige Features & Fixes
- Rückgängig-Funktion ("Back"-Methode) reparieren:  
  - im Moment Geht nur so viele Schritte zurück, wieviel verschiedene Roboter bereits bewegt wurden, also pro roboter auswahl geht ein back schritt verloren im moment.  

