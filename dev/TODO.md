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


# pre-hints:
1. For random games:
  ◦ Show numPreHints (2-4) hints saying "The AI found a solution in less than [solution+numPreHints] moves" and decreasing
  ◦ Show a final exact hint "The AI found a solution in X moves" with toast notification
  ◦ Then show all actual move hints numbered "1/X", "2/X", etc.
  ◦ In the example: if solution is 7 moves and numPreHints is 4, they should see:
      ▪ "The AI found a solution in less than 11 moves" (7+4)
      ▪ "The AI found a solution in less than 10 moves" (7+3)
      ▪ "The AI found a solution in less than 9 moves" (7+2)
      ▪ "The AI found a solution in less than 8 moves" (7+1)
      ▪ "The AI found a solution in 7 moves" (with toast)
      ▪ Then all 7 move hints
      ▪ Then "All hints are shown"
        
2. For level games 1 - 10:
  ◦ Always show the first 2 normal hints (no pre-hints)
  ◦ Then diable the hint button
    
3. For level games > 10:
  ◦ No pre-hints
  ◦ No normal hints
  ◦ diable the hint button
    
4. After resetting hint count (after seeing all hints):
  ◦ Only show normal hints, no pre-hints in a random game
  ◦ in level games nothing more
