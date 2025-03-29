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

yes, but not a preview, the 141+ levels should be added to the level screen then in an unlocked state already at the bottom of the scereen with a new headline "ciustom levels" also we need a view where the user can select the whole level as text output in the sae format, i want to use it to design new levels and need the same text format as the existing levels

next run.....

this looks fine, but i cannot edit the level, when i select level 1 it just sais "tap to edit" but nothing happens. add some debug and fix it.

- and i cannot select the output in the textbox to copy paste it.
- add a button to send the content to https://ronboyard.z11.de/share/data=<here the urlencoded content>

- also make the text color 666666 in the edit and in the export box.

next run.....

i corrected the link, but ther should be a textbox to copy paste the content of the data for the map first, underneath there should be the share link.

also preselect and load level 1 when opening the editor

and the editor still dont work: inothing happens when click on the editor field, this is the  log:

next run.....

the editor is now showing an empty grid and nochting visual happens, when clickin in it. it shludl show the current level 1 but it seems empty.

- pressing the APPLY button generates successfull some walls in the dimensions, but thetop wall at 0 is missing and the bottom walls are one too far north.

- loading another level changes nothing (tested level 10)

- copy to clipboard works fine now

- add more debug when clicking the edit buttons and the erase button.

next run.....



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
- winning a level this still shows only 0 stars earned if there was already saved one star in the settings from an older version of the game. It also still shows the old stars

backgrounnd: i plan to migrate soo to another framework, that needs UI and logic separated
- search through all java files  and classes in the eclabs/ and the ricochet/ folder (ignore /var/www/Roboyard/app/src/main/java/driftingdroids completely, because it is a foreign class)
- make a list of all complete files that could be moved into the logic/ folder
- make a list of classes, that has to be separated from UI files into new classes in the logic/ folder.
- at the moment there is a ui/ folder and a util/ folder, but those are not sorted good, i need to resort everything before migrating

continue and create a full document, that shows:
- what files i have to move
- what files have to get which methods extracted
- which files can be deleted completely.
also explain problems or mixed classes, where it is not clear

