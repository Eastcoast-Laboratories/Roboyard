# TODO



# image creation:

Bild 1 (erste Szene mit Hecke, noch ohne spezielle Blumen oder Wurzeln):
A traditional cel-shaded anime-style painting depicting a lush green hedge maze with a warm, soft lighting atmosphere. The perspective is slightly elevated, showcasing the winding paths and intersections of the maze. The environment feels magical and inviting, with intricate details in the leaves and hedges, giving it a hand-painted Studio Ghibli-inspired look.

Bild 2 (erste schräg oben Perspektive):
A traditional-style anime painting portrays a small, glowing blue robot speeding through a winding hedge maze. The hedges are lush and detailed, featuring intertwined blue roots and bright yellow flowers. The perspective is from a slightly elevated angle, showing the depth of the maze. At the end of one of the paths, a mysterious swirling wormhole-like portal glows softly, inviting curiosity. The scene is inspired by Studio Ghibli’s hand-painted style, with a dreamlike atmosphere and a warm color palette.

Bild 3 (zweite schräg oben Perspektive mit blauen Wurzeln und gelben Blumen):
A hand-drawn and painted illustration in a Studio Ghibli-style shows a small blue robot racing through a hedge maze. The hedges are richly detailed, with bright yellow flowers and strange blue roots woven into the foliage. The perspective is from above at an angle, revealing the complexity of the maze’s pathways. The glowing wormhole at the end of the maze shimmers with an inviting, mysterious energy. The scene is vibrant, with soft shadows and an animated film-like texture.

Bild 4 (dritte schräg oben Perspektive, mit mehr Ghibli-Detail):
A hand-painted animation still in the Studio Ghibli style, depicting a small glowing blue robot zipping through a beautifully overgrown hedge maze. The hedges are thick and full of life, with twisted blue roots and patches of yellow flowers scattered throughout. The view is from an overhead diagonal perspective, emphasizing the intricate maze structure. At the end of the winding path, a surreal glowing wormhole pulses with soft energy. The color palette is warm and earthy, with a painterly touch adding depth and charm to the scene.

Bild 5 (dritte schräg oben Perspektive, mit 4 robotern):
A traditional animation, hand-painted in a Studio Ghibli-style, depicting a lush hedge maze with visible paths, intersections, and vibrant greenery. The hedges have intertwined blue roots, bright yellow flowers, and some red flowers for added contrast. Three small, cute robots with a glowing faceplate navigate the maze. The main character is a blue robot leading the way, followed by a green and a yellow robot exploring different paths. The scene is immersive, with soft lighting, a fantasy atmosphere, and detailed, textured foliage.

neu fuer openai:
A traditional cel-shaded anime-style painting depicting a lush green hedge maze with a warm, soft lighting atmosphere. portrays a small, glowing blue robot speeding through a winding hedge maze in the background three glowing pink, green and yellow robots. The hedges are thick and full of life, with twisted blue roots and patches of yellow and also a few red flowers scattered throughout. The perspective is from a slightly elevated angle, showing the depth of the maze and showcasing the winding paths and intersections of the maze. At the end of one of the paths, a mysterious swirling wormhole-like portal glows softly, inviting curiosity. The main character is a blue robot, the other three are the pink, green and yellow robots. use the last four robot images, i uploaded. The environment feels magical and inviting, with intricate details in the leaves and hedges, giving it a hand-painted Studio Ghibli-inspired look.

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

ergänze das achievement konzept, wie man sicherstellen kannn, dass man für die achievements wirklich unterschiedliche maps macht umdie achievements zu erreichen, also es muss irgendwie jede map gespeicherrt werden für immer in der geschafft-liste damit man diese nicht doppelt zählt , bei jedemeintrag muss noch gespeichert werden, wie oft und wann man die geschafft hat, damit man hinterher auswerten kann, was fr maps man wann geschafft hat,. man braucht keine hashes, da die maps schon sehr wenig daten haben kann man die gesamten maps in dieser neen history speichern. dadurch wird verhindert, dass man z.b. immer wieder ein savegame lädt und das immer wieder löst. auch wird dann verhindert, dass man die slebe map imer rwieder löst. die achievements sollen nur wirklich verrschiedene maps zählen für achievements, die auf die anzahl maps hinauss sind. auch die speed achievements dürfen nur triggern, wenn man eine map das erste mal schafft, denn danach ist es ja kein kunststck mehr. gehe alle achievements durch auf diese aspekte und ergänze solche bedingungen, die für das erreichen notwendig sind.

DRY! 
@achievements.md#L185-190 
Benutze die bestehende history.
baue die bestehende history so um,
- eine einträge mehr löschen kann
- die history nur noch einen eintrag pro verschiedene maps speichert
- bei jedem eintrag speicherrt wann die alles gelöst wurde,  
- sortierung in der histroy ansicht dann danach, wann jeweisl zuletzt gelöst.
- es sollen alle zeitpunkte, wann sie gelöst wurde gespeichert werden, damit man hinterher verschiedene auswertungen darüber rmachen kann, auch für streaks und so
- die walls sollen getrennt von den position s gespeichert werden für neue achievements, die sich auf die selben wall-storage einträge beziehen mit verschiedenen robot positions aber gleichen walls.
- beue unittests, die die eaenderte history testen und ältere einträge daraus aufrufen und testen ob man die noch spielen kann.
- wenn die history funktionierrt, dann die achievements logik bearbeiten um die neuen bedingungen in den achievements zu testen mit unittests und espresso tests, die die achievements testen und baue neue espresso tests, wenn es noch keinen gibt, der die einzelnen neuen aspekte  wirklich testst

- entferne den delete button bei den history einträgen (die logik dahinter kann ergalten bleiben)
- für die 2. phase fehlen noch, dass die hint nutzung richtig gespeichert wurde in der history. beim ersten mal spielen der map muss schon sofort immer mit gespeichtert werden, bus zu welchem hint die hints benutzt wurden. für die achievements ist der entscheindende hint der wo gesagt wird, welche farben der robots benutzt werden müssen, ab dann gild die map nicht merh als ohne hints gelöst. das muss dauerhaft iin dem history eintrag der map gespeihert werden, auch wenn man die selbe map später noch mal ohne hints löst, gilt diese trothzdem nur als mit hints gelöst
- wenn der live move aktiv ist, gilt eine map immer sofort, wenn ein live move angezeigt wird als nicht ohne hint gelöst, das muss sofort in der history gespeichert werden