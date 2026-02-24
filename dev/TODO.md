# TODO

# Difficulty
check if this all works:
- Beginner
  - show any puzzles with solutions with at least 4-6 moves (max 6 not always works )
  - goals are always in corners with two walls
- Advanced
  - solutions with at least 6-8 moves
  - three lines allowed in the same row/column
  - no multi-color target
- Insane mode
  - five lines allowed in the same row/column
- Impossible mode
  - five lines allowed in the same row/column

# Accessibility
- Add a preference setting to customize the order of information in TalkBack announcements
- die ganzen buttons unten sollen, wenn accessibility acitive, viel niedriger, damit sie unter die navigation buttons noch passen

# levels 
- winning a level: this overwrites the old saved stars and data, so you can "loose" stars this way if you play a level again with less stars
- In den Leveln, wenn man die schon mal gespielt hat, dann die optimale Zahl gleich anzeigen

# Sonstige Features & Fixes
- show the big 90% transparent blue robot image in the background of the level screen too
- walls around level selection screen are not tiled 
- add sound effects for buttons
- Create custom button sounds to provide audio feedback for different actions

- Wenn Map ratio sehr gross, dann die Buttons unten alle kleiner und nur mit Icons statt text und alle in eine Reihe anstatt 2


- enhance solution in multi-target mode: if Preferences.robotCount is > 1, find out, which robot can get to its target the fastest by a loop through all tartets:
 - suggestion: create a temporary map for the solver, where you delete all other targets and let the solver run with only one target at the time. store the solutions and take only the shortest solution


# current

- im save slot anzeigen, wieviel die optimal moves sind und wieviel man gebraucht hat, wenn man den level completed hat

- choose num_robots in settings (2-5)

- only add the same color at the end of the hint if it is not the same as the last color




# most important


- sidekick_concept.md

1. testen: die achievements "Solution length" müssen alle ohne benutzung  hint gelöst werden um freigeschaltet zu werden

2. der hint container soll sich ausblenden, wenn der solver was gefunden hat und das angenommen wurde, nicht mehr zwischendurch ein-und ausblenden.

3. der hint container soll sich auch wenn der live-move-toggle an ist ausblenden, sobald der solver was gefunden hat und die map angenommen wurde. Im moment blendet der nur aus, wenn der live-move-toggle aus ist
4. add achievements tracking same walls, different positions
  
# last prompt:

baue die bestehende history so um,
- bei jedem eintrag speichert wann die alles gelöst wurde,  
- sortierung in der histroy ansicht dann danach, wann jeweisl zuletzt gelöst.
- es sollen alle zeitpunkte, wann sie gelöst wurde gespeichert werden, damit man hinterher verschiedene auswertungen darüber rmachen kann, auch für streaks und so
- die walls sollen getrennt von den positions gespeichert werden für neue achievements, die sich auf die selben wall-storage einträge beziehen mit verschiedenen robot positions aber gleichen walls.

- für die 2. phase fehlen noch, dass die hint nutzung richtig gespeichert wurde in der history. beim ersten mal spielen der map muss schon sofort immer mit gespeichtert werden, bus zu welchem hint die hints benutzt wurden. für die achievements ist der entscheindende hint der wo gesagt wird, welche farben der robots benutzt werden müssen, ab dann gild die map nicht merh als ohne hints gelöst. das muss dauerhaft iin dem history eintrag der map gespeihert werden, auch wenn man die selbe map später noch mal ohne hints löst, gilt diese trothzdem nur als mit hints gelöst

-  wenn der live move aktiv ist, gilt eine map immer sofort, wenn ein live move angezeigt wird als nicht ohne hint gelöst, das muss sofort in der history gespeichert werden (auch der grund)

- in den savegames soll auch gespeichert werden, wenn noch nciht vorhanden, wieviel züge man gebraucht hat, ob die karte beim ersten mal ohne hints gelöst wurde und wie gut dabei beim ersten mal ohne hints. wenn sie später noch mal besser gelöst wurde auch wieder ob ohne hints. und alle nötigen deatils, die man  für acheivements benötigen könnte (DRY mit der history)

- im savescreen ergänze bei jedem eintrag der savegames und der history (DRY) einen info button (i  im kreis) wenn man darauf tippt ein popup, das  alle details zu der map bzgl. hint benutzung, wie oft und wann gelöst, wei schnell, mit wieviel zuegen undn was die optimalen züge gewesen wären, und ob der movecount aktiv war auflistet gut lesbar aufbereitet und  prasentiert mit allen details bis zu sekunden und welchen hint genau und so weiter. 

-  ergänze logs, die zeigen, wenn er die map aktualisiert mit den daten vorher und nachher, es msste der best mve ja verrbessert werden bei dem speichervorgang. 

verbessere den test, so dass du die logs sehen kannst und berprüfe, dass er wirklich die selbe map daten akualisiert

1. first played ist noch falsch, das ist anscheinend der erste move, den man gemacht hat, wo die map das erste mal gespeichert wurde, aber innerhalb des selben spiels wird dann last played gespeihert, das interressiert in der info box nicht. es interressiert nur, wann ein spiel gelöst wurde, Ich weis nicht ob dieser wert der im moment bei "first played" angezeigt wird überhaupt gespeichert werden muss, es scheint, dass er innerhalb des selben spiels schon 2 einträge erzeugt in der liste in einem history eintrag, enaml beim ersten move und dann immer bei den weiteren mves . er sollte aber in einem history eintrag nur den selben eintrag aktualisieren anstatt einen neuen anzulegen innerhalb des history savegames