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

8.
mach einen Espresso Test für 1000 History Einträge + Pagination. also erst die app daten löschen, dann 1000 einträge erstellen (50x auf add 20 drücken) erhöhe dazu den erlaubten espresso timeout auf 1h

führe ihn aus ind finde heraus, wann der speicher voll ist, wenn voll, dann versuche eine lösung zu finden, dass weniger speicher verwendet wird

höre erst auf, wenn du all diese punkte ausgeführt hast und stelle keine fragen


9. verhindere, das bei den map generatoren walls im inneren des mittleren carrees generiert werden
10. einen Memory-Gate der wartet bis genug Speicher frei ist, bevor der Solver initialisiert wird.
---
# last prompt:

Das Problem ist: der Hint button checked status kontrolliert hja ob der rein slided oder upslided. Der Hint Button wird aber zu oft checked false (upslide) gesetzt. solange noch keine lösung gefunden ist, soll der auch nicht unchecken. erst, wenn die lösung akzeptiert ist, soll er unchecken. suche, wo der auf checked false gesetzt wird obwohl die lösung noch gar nicht akzeptiert wurde, er soll zwischen den versuchen nicht uncheken

----



1.
erstelle auch eine helper funktion in testhelper um die logs zu sehen und zu analysieren

2.
merke dir: bevor du einen neuen test schreibst immer die TestHelper klasse analysieren was du benutzen kannst

3.
@GameLogic.java?%7B%22path%22%3A%22%2Fvar%2Fwww%2FRoboyard%2Fapp%2Fsrc%2Fmain%2Fjava%2Froboyard%2Flogic%2Fcore%2FGameLogic.java%22%2C%22ref%22%3A%22%22%7D#L311-316 
        boolean isMultiTargetMode = (targetColors > 1);
scheint mir falsch zusein, das ist doch immer true, oder? 
das multitarget ist -1 und wenn es dabei ist, dann ist die anzahl tagets > NUM_ROBOTS , so könnte man das testen, oder? korrigiere mich, wenn ich falsch liege

4.
fahre fort mit dem OOM testing mit dem ImpossibleDifficultyNewGameTest.java durch ohne die max_depth zu reduzieren. Es müsste doch genauso möglich sein wie bei einem target, rätsel die 25 züge brauchen zu finden, also  den weg uu berechnen ohne dass die möglichkeiten explodieren. vieleicht is die lösung für mutli-tarets nicht optimal? 

---



2.
use collectLogcatLines(), dumpLogcat(), clearLogcat() from TestHelper in ImpossibleDifficultyNewGameTest where applicable

3.
erhöhe die wartezeit in ImpossibleDifficultyNewGameTest zwischen den klicks auf "New game" auf 60s und zähle in dem test, wie oft er eine lösung mit mind. 17 (bei insane difficulty) findet. der test failt, wenn er innerhalb 5 minuten keine lösung gefunden hat. teste den test aber noch nciht, den er scheint keine komplizierten llösungen mehr zu finden auch ohne multi-target mode . Aber
erstelle zuerst einen test in ImpossibleDifficultyNewGameTest ohne multi-target-mode mit insane und lasse den 5x laufen mit new game und beweise, dass er lösungen findet 

4. er findet jetzt keine lösungen mahr über 12 züge, auch ncint im normal mode ohne multi-target! irgendwas an deinen änderungen in den letztten 2 commits am solver lässt den nach ein paar verrworfenen maps hängen. schau dir die commits an was an dem solver algorythmus geaendert wurde und entferne die änderung, die um hängen führt und teste mit dem unittest bis das wieder läuft. erhängt jetzt bei dieser message:

[MEMORY] Stopping search: memory was critically low during depth 12

dann bricht er aber nicht so ab, dass die map verworfen werden kann und eine neue ausprobierrt wird, sondern hängt dann fest und macht nicht weiter

löse das zuerst, dass er die map dann errfolgreich verrwirft und weiter probiert