# TODO
- show a big 90% transparent blue robot in the background of the whole app
- walls on menu are not tiled 
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
  - five lines allowed in the same row/column
- Impossible mode
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
- add a "retry" button to the level game screen

# current
- winning a level: this overwrites the old saved stars and data, so you can "loose" stars this way if you play a level again with less stars

- auto save to history after 60 seconds (2s for now) seems to work, but nothing shown in the histroy tab

- new map each time is not working. if set, there is generated a new map anyway, all the walls should stay the same, just new robot and target positions on "new game"

- @ModernGameFragment.java#L622-624 hintbutton.setenabled(false) hat keinen effect


- for debugging: add a skip button to levels, which will just move the robot to its target in one move
- multi-color target is gone

- Beginner
  - show any puzzles with solutions with at least 4-6 moves (max 6 not always works )

- when loading a map, the mapname is not shown
- when saving a game and directly load it again, then save again and the next time it loads, it has no target. strangely, the solver still works, as if the target were there where it was before saving.

- the same: if oyu start the app and directly load the auto save game, it has no target

- In den Leveln, wenn man die schon mal gespielt hat, dann die optimale Zahl gleich anzeigen

- Die Maps die verworfen wurden zählen beim generieren, also einen counter bei "AI calculating silution..."

- im start menu schon die Mini-Maps cachen

- Beim HintText einen kleinen Pfeil links für zurück

- Wenn Map ration sehr gross, dann die Buttons unten alle kleiner und nur mit Icons statt text und alle in eine Reihe anstatt 2

- optimale Zahl größer und Margin rechts grösser

- Wenn man hint drückt und bei der optimalen Move-Zahl angekommen ist, steht auch schon gleich der erste Move da, das ist doof. ein Hint dazwischen mit "Optimal solution: X"


- Hinttextfeld erstmal unsichtbar starten

- die Roboter Ihren Weg als Linie anzeigen würden

- Bug: Beim Wischen kein Sound, beim Tappen Sound, obwohl ausgeschaltet

- drück mal 10 x reset hintereinander

Stars oben das Wort weg

Einen Würfel Button für die Maps, wenn newmapeachtime no ist

Walls etwas kürzer

- Hintergrund tile zufällig drehen

when enabling Accessibility mode in settings automatically:
- mapsize auf 8x8 stellen
- difficulty auf Beginner stellen
- new map each time auf "no" stellen
- targetcolor auf "1" stellen
- show this message below the radio buttons:
"To enable the accessibility mode, you have to enable TalkBack in your settings"
- Open automatically the android settings app where you can enablt TalkBack mode


