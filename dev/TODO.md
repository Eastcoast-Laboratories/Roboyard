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


- mainactivity kann weg

- wenn man eine  externe map lädt, dann startet der solver nicht richtig, er findet keine lösung und es bleibt "ai calaculating..." stehen
- 
---
# last prompt:


1. verhindere, dass bei jeglicher map walls im inneren des mittleren carrees angezeigt werden, einfach ignorieren, wenn da walls in der mitte drin sind
2. 
----


1.
merke dir: bevor du einen neuen test schreibst immer die TestHelper klasse analysieren was du benutzen kannst


4.
fahre fort mit dem OOM testing mit dem ImpossibleDifficultyNewGameTest.java durch ohne die max_depth zu reduzieren. Es müsste doch genauso möglich sein wie bei einem target, rätsel die 25 züge brauchen zu finden, also  den weg uu berechnen ohne dass die möglichkeiten explodieren. vieleicht is die lösung für mutli-tarets nicht optimal? 

---
prompt 

vergiss micht die comit message am ende

benutze bestehende unittests oder erstelle einen neuen wenn es keinen gibt, der mit espresso in der ui wirklich kllcikt und das alles durchtestet


höre erst auf, wenn du den fehler gefunden hast und nachbauen konntest und repariert hast

----

merke dir: die roboyard.z11.de laravel app wird deployed mit cd /var/www/roboyard.z11; ./deploy.sh

1. wenn man ausgeloggt wird, dann soll er sofort wieder einloggen und ein neues token erfragen. das passwort soll im localsorage gespeichert bleiben, damit die app sich jederzeit wenn ntig wieder einloggen kann. oder ist das schon genau so?

auch , wenn beim level syncen unauthorized kommt, soll er einmal versuchen sich neu einzuloggen und nur wenn dann immer noch unauthorized kommt , soll er die toast error meldung ausgeben, dass er nicht mehr eingeloggt ist


---
1. ich sehe das problem beiden borders: da ist eine extra border, wenn man den level , der zuletzt gespielt. das soll aber nicht. es soll die vorhandene gelbe border (gelb bei gespielten, blau bei nicht-gespielten grün werden und diese einzige border soll ausfaden, die extra border soll entfernt werden 


4. die nicht geschafften level boxen (freigeschaltete und nicht-frei), die in der selben reihe sind, wie geschaffte, sollen alle etwas mehr height bekommen, so dass sie genau so gross sind wie die boxen mit den geschafften. wenn kein geschaffter in der selben reihe ist, sollen sie nur GENAU halb so hoch sein wie die geschafftenboxen

5. die nicht freigeschaleten sollen die level Zahl kleiner und unterhalb des schlosses haben und das wort "Level" davor, also "Level X" unter das schloss und genau halb so hch wie die geschafften ((im moment sind die viel zu niedrig) und die schrift "Level X" kann man nicht sehen


