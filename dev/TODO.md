# TODO
- show a big 90% transparent blue robot in the background of the whole app
- walls on menu are not tiled 
- add outer walls on level selection 
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

# levels 
- the back button at the bottom does not work
- auto scroll to the latest unlocked level when entering the level selection screen
- add a "retry" button to the level game screen

# current
- the solver is sometimes not working any more (always shows cancel)
- winning a level: this overwrites the old saved stars and data, so you can "loose" stars this way if you play a level again with less stars

- auto save to history after 60 seconds (2s for now) seems to work, but nothing shown in the histroy tab

- new map each time is not working. if set, there is generated a new map anyway, all the walls should stay the same, just new robot and target positions on "new game"

- when loading a game, the map name is not shown

- win stop timer also in random game

- @ModernGameFragment.java#L622-624 hintbutton.setenabled(false) hat keinen effect

- randomize number of pre-hints before showing the solution not only on each app start but on each new game 