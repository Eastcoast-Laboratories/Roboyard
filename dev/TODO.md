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


# Accessibility
- Add a preference setting to customize the order of information in TalkBack announcements
- Create custom button sounds to provide audio feedback for different actions
- Add a high-contrast mode for better visibility
- Remove "AI calculating solution..." on game start.
- die ganzen buttons unten sollen, wenn accessibility acitive, viel niedriger, damit sie unter die navigation buttons noch passen

# achievements
- add achievement system

# levels 
- winning a level: this overwrites the old saved stars and data, so you can "loose" stars this way if you play a level again with less stars
- for debugging: add a skip button to levels, which will just move the robot to its target in one move
- In den Leveln, wenn man die schon mal gespielt hat, dann die optimale Zahl gleich anzeigen

# Sonstige Features & Fixes
- show the big 90% transparent blue robot image in the background of the level screen too
- walls around level selection screen are not tiled 
- add sound effects for buttons
- add sound effect background for game
- Back-Undo Button should also undo the last painted robot path 


- im start menu schon die Mini-Maps cachen

- Wenn Map ratio sehr gross, dann die Buttons unten alle kleiner und nur mit Icons statt text und alle in eine Reihe anstatt 2

- Bug: Beim Wischen no Sound, beim Tappen Sound, obwohl ausgeschaltet

- drück mal 10 x reset hintereinander

- Einen Würfel Button für die Maps, wenn generateNewmapeachtime no ist

- settings: when setting a higher robotCount than targets, then automatically set the targetCount to the same value



- if Preferences.robotCount is > 1, find out, which robot can get to its target the fastest by a loop through all tartets:
 - suggestion: create a temporary map for the solver, where you delete all other targets and let the solver run with only one target at the time. store the solutions and take only the shortest solution

- new Second pre-hint - show the involved robot colors:

            // pre-hint - show the involved robot colors
            message.append("Hint 2/").append(totalPossibleHints).append(": ");
            message.append("The solution involves moving: ");
            
            // Look at first 5 moves (or all if fewer) to see which robots are involved
            ArrayList<String> robotsInvolved = new ArrayList<>();
            List<IGameMove> moves = solution.getMoves().subList(0, Math.min(5, solution.getMoves().size()));
            for (IGameMove move : moves) {
                if (move instanceof RRGameMove) {
                    RRGameMove rrMove = (RRGameMove) move;
                    String robotColor = getRobotColorName(rrMove.getColor());
                    // Only add if not already in the list
                    if (!robotsInvolved.contains(robotColor)) {
                        robotsInvolved.add(robotColor);
                    }
                }
            }
            
            // Format the list of robots with commas and "and" for the last item
            for (int i = 0; i < robotsInvolved.size(); i++) {
                if (i == robotsInvolved.size() - 1 && robotsInvolved.size() > 1) {
                    message.append("and ").append(robotsInvolved.get(i)).append(" robots");
                } else if (i == robotsInvolved.size() - 1) {
                    message.append(robotsInvolved.get(i)).append(" robot");
                } else {
                    message.append(robotsInvolved.get(i)).append(", ");
                }
            }
            
            // Display the hint in the status text view
            updateStatusText(message.toString(), true);

# current

- multi-color target is gone

- fdroid entry enhancements
- make all accessibility coordinate announcements one more, so on a 8x8 board they go from 1,1 to 8,8 instead of 0,0 to 7,7

- multiple sources of truth concept ausführen
- walls missing: in ensureOuterWalls() the return should just return data and instead we should search, where the outer walls are missing to be generated in the first place

- deep link support

- the green target color on a shared map is yellow

- im save slot anzeigen, wieviel die optimal moves sind und wieviel man gebraucht hat, wenn man den level completed hat

- wenn man einen level ohne hints geschafft hat, anzeigen, wieviel die optimal moves wären

- in der history steht immer 16x16 statt der richtigen grösse

