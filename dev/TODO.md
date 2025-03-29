# TODO
- the back button for one move does not work properly, only the first move is moved back, than nothing happens any more
- show a big 90% transparent blue robot in the background of the whole app
- walls on menu are not tiled 
- add outer walls on level selection 

- add the "level complete" function also to the random games, as in level games, but label the button "new game" to start another random game

# levels 
- the back button at the bottom does not work
- hide the "new game" button in the game screen
- max 2 hints in level game are allowed, then hide the hint button
- in the level selection screen, levels are disabled if you dont have enough stars. each star unlocks one level
- show more levels when you have enough stars
- auto scroll to the latest unlocked level when entering the level selection screen
- show the number of stars you have for each level in the overview
- get one more star each time you solve a level in a level game when:
  - solved with the optimal solution and no hints were shown (3 stars)
  - solved with one move more than the optimal solution and no hints were shown (2 stars)
  - solved with the minimum possible moves and one hint was shown (2 stars)
  - solved with the minimum possible moves and two hints were shown (1 star)
  - solved with two moves more than the optimal solution and no hints were shown (1 star)
  - solved with two moves more than the optimal solution and one hint was shown (0 stars)
  - solved with three moves more than the optimal solution and no hints were shown (0 stars)
 - on level complete, save:
    - the Time needed to complete
    - the Moves needed
    - the Optimal moves from the solver
    - the Number of different robots you used for your solution
    - the Number of squares surpassed
    - the Number of hints used


- auto save to history after 60 seconds
- win message and stop timer also in random game
- add sound effects for buttons
- add sound effect background for game
# Difficulty
- Beginner
  - show any puzzles with solutions with at least 4-6 moves (max 6 not always works )
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
4. get rid of the accessibility texts of the walls and the logo in the menu


# Sonstige Features & Fixes
- Rückgängig-Funktion ("Back"-Methode) reparieren:  
  - im Moment Geht nur so viele Schritte zurück, wieviel verschiedene Roboter bereits bewegt wurden, also pro roboter auswahl geht ein back schritt verloren im moment.  

# achievements
- add achievement system

# current
- stalls, when youplay level 2 and 
- winning a level this still shows only 0 stars earned if there was already saved one star in the settings from an older version of the game