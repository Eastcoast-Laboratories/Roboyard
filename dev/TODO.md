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
5. für die achievements ist der entscheindende hint der wo gesagt wird, welche farben der robots benutzt werden müssen, ab dann oder wenn live move aktiv wargilt die map nicht merh als ohne hints gelöst. das muss dauerhaft iin dem history eintrag der map gespeihert werden, auch wenn man die selbe map später noch mal ohne hints löst, gilt diese trothzdem nur als mit hints gelöst
6. teste ob wenn der live move aktiv ist, gilt eine map immer sofort, wenn ein live move angezeigt wird als nicht ohne hint gelöst, das muss sofort in der history gespeichert werden (auch der grund)

7. teste die Kommunikation eine Version ein (ver=1) die wird erwartet. Z.b. Beim deep Link der map erwartet der Ver 1 aber wenn die höher ist, dann macht er statt die map mit unerwarteten Daten zu starten einen Toast "your app needs an update" und lenkt zum Menü; Auch die Login communication und die Synchronisation entsprechend. So kann ich in Zukunft breaking changes in Laravel ausrollen ohne dass die alten Apps abgestürzen

# last prompt:

da ist was gannz doll im argen: einige files werden anscheinend nicht gespeichert:
File does not exist: history_2.txt
File does not exist: history_1.txt
File does not exist: history_20.txt
File does not exist: history_19.txt
File does not exist: history_18.txt
File does not exist: history_17.txt

anscheinennd sind nicht die minimaps an sich das problem, sondern der speicher insgesamt. ich habe de 10000 einträge gelöscht, jetzt ist wieder alles flüssig, aber die minimaps werden jetzt gar nicht mehr geladen, dein map cache funktioniert nch  nicht!  

baue einen test, der die history lädt und die minimap prüft und sieht, dass  sie nicht geladen wird (falls kein eintrag in der histry ist, erzeuge einen indem du ein random game startest und mindestens einen move machst), dann fixe das und teste erenuet.

wenn das minimap cachen geht, dann committe das .

dann baue einige sachen ein:
1. In dem History Screen die pagination erweitern um Seite x von y und ausblenden, wenn unter 20 Einträge vorhanden sind.
Immer 20 Einträge pro Seite anzeigen und ab Seite 2 auch oben die pagination buttons anzeigen. Die Buttons sollen im selben Container sein, wie die Einträge und mit scrollen. 

1. Im debug screen anzeigen wie viel Daten im Speicher sind, total und anteilig Level, achievements, history,   ....

2. Ruben button einbauen um 100 Dummy History Einträge zu generieren, benutze das die Level Maps und tausche den Namen durch Test aus also aus Level 1 wird"Test 1"

Baue einen ui Espresso Test, der 5s in settings auf den Titel drückt um in den debug screen to kommen (durchsuche die bestehenden Tests dazu), dort den neuen 100 History Einträge 2x drückt und dann zum save screen navigiert und dort die pagination tester und die responsiveness bei so vielen Einträgen, falls das einen oom erzeugt, baue die History so um, dass nur die aktuellen sichtbaren Einträge in den Speicher geladen werden

----
die maps werden immer noch nicht geladen, man sieht nur die dummy map überall

die 100 history entries werden nicht erstellt "Levels directory not found" suche beim levels screen wie die levels geladen werden

erstelle einen nuen debug unittest, dass er sichtbar und genau so wie im DebugSettingsNavigationTest funktinoinrt,
1. 
 die debug 100 histroy entries  mit espresso in der UI sichtbar in der debug seite erstellt 
2. verifizieren dass kein fehler ist
3.  die memory einträge verifiziert (im moment steht da überall 0MB)

wenn die dummy entries funktionieren, dann committen

dann mach weiter mit pagination fix, die funktiniert nämlich noch nciht
1.. die pagination soll nur in dem scrollbaren bereich der history einträge mit drin sein und nur ganz unten innerhalb des scrollbaren bereichs sein, wenn man ganz runter scrollt. 
2. der next button funktioniert nicht, es bleiben die ersten 20 sichtbar und die top paginatin erscheint auch nicht

mach einen Espresso Test für 200 History Einträge + Pagination.

führe ihn aus und reparier alles

höre erst auf, wenn du all diese punkte ausgeführt hast und stelle keine fragen