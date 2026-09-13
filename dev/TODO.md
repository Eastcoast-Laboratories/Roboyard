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
  
4. für die achievements ist der entscheindende hint der wo gesagt wird, welche farben der robots benutzt werden müssen, ab dann oder wenn live move aktiv war gilt die map nicht merh als ohne hints gelöst. das muss dauerhaft iin dem history eintrag der map gespeihert werden, auch wenn man die selbe map später noch mal ohne hints löst, gilt diese trothzdem nur als mit hints gelöst
5. teste ob wenn der live move aktiv ist, gilt eine map immer sofort, wenn ein live move angezeigt wird als nicht ohne hint gelöst, das muss sofort in der history gespeichert werden (auch der grund)

6. teste die Kommunikation eine Version ein (ver=1) die wird erwartet. Z.b. Beim deep Link der map erwartet der Ver 1 aber wenn die höher ist, dann macht er statt die map mit unerwarteten Daten zu starten einen Toast "your app needs an update" und lenkt zum Menü; Auch die Login communication und die Synchronisation entsprechend. So kann ich in Zukunft breaking changes in Laravel ausrollen ohne dass die alten Apps abgestürzen

8.
mach einen Espresso Test für 1000 History Einträge + Pagination. also erst die app daten löschen, dann 1000 einträge erstellen (50x auf add 20 drücken) erhöhe dazu den erlaubten espresso timeout auf 1h

führe ihn aus ind finde heraus, wann der speicher voll ist, wenn voll, dann versuche eine lösung zu finden, dass weniger speicher verwendet wird

höre erst auf, wenn du all diese punkte ausgeführt hast und stelle keine fragen


2.  einen Memory-Gate der wartet bis genug Speicher frei ist, bevor der Solver initialisiert wird.


-  achievements: die schrift muss bolder sein, die überschriften brauchen die selbe weise shadow wie die beschreibungen. alle schriften in den a hievements bolder


- wenn man eine  externe map lädt, dann startet der solver nicht richtig, er findet keine lösung und es bleibt "ai calaculating..." stehen
- 
---
# last prompt:

- Oft, speichert er die neue "best moves time" nicht am Ende wenn man im Level 3 Sterne erreicht hat, es ist dann manchmal immer noch nur ein dash oder die zeit wird nicht aktualisiert. auch die anzahl moves ist dann nicht gespeichert und er zeigt z.b. immer noch +1 moves an obwohl man 3 sterne hatte und diese drei auch ini der übersicht angezeigt werden. nur in dem imfo popup dann hal noch nicht korrekt

- Level 39 zu einfach

- Dies kann nach ein paar Tagen erneut: 
    - Welcome Back. You logged in 2 days in a row
    - Welcome!
    - Play your first game
    - First Steps Complete Level 1
    - Getting Started
    - Und solve 5 Games with optimal moves auch

- Vielleicht sollte man noch irgendwie recorden, wenn man eine map besonders lange angeschaut hat, dass man sieht dann wahrscheinlich auch im Kopf gelöst hat


- in dem popup beim savegame sollte der anzeigen, ob eine lösung in dem game gespeichert ist, wenn ja, soll diese im errorlog erscheinen. Wir brachen einen weg, eine nicht funktionierende lösung aus einem savegame zu löschen oder eine neue lösung zu genereieren, obwohl eine falsche im savegame gespeichert ist

- Wenn man in einem history Eintrag schon einmal die Tipps benutzt hat, um sich die Anzahl optimale anzeigen zu lassen. Dann soll dies auch gleich beim Start eines history Games angezeigt werden

- wenn man auf "view acheivements" im Popup  drückt soll auch der spinner kommen, der im main menu kommt, wenn man auuf den achievements button drückt