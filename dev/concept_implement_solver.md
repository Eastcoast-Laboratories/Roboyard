1. Enhance GameStateManager with hint state tracking:
   - Add fields to track: hintCount, currentSolutionIndex, and totalSolutions
   - Add a boolean to track if full solutions are being shown
   
2. Create a new method in GameStateManager:
   - showNextHint() that handles the hint logic progression
   
3. When the hint button is clicked:
  - in the preferences definable, how many hints, you want to see before showing the solution
   - First 2-4 clicks: Show individual random hints: e.g. if the solution is 5:
    - first hint: less than 7
    - second hint: less than 6
    - third hint: exacltly 5
    - Subsequent hint: show the robot that has to move first
    - Subsequent hint: show the first move by highlighting the direction, that robot must move
    - Subsequent hint: show the second robot that has to move
    - Subsequent hint: show the second move by highlighting the direction, that robot must move
    - ...
    - Last click: Show the full solution path by moving the robots one by one move to the target
    - Subsequent clicks: Cycle through alternative solutions if available
   
4. Visually display hints:
   - Add a visual indicator on the GameGridView for highlighted hint cells by altering the saturation of the background graphic
   
5. Update hint button text:
   - Change from "Hint" to "Solution" after showing all hints
   - Change to "Next Solution" when cycling through solutions

# Reusable Components:
- SolverDD algorithm: Already reusable across UI implementations
- GameMove representation: Can be reused to represent solution steps
- Solution Animation Logic: Could be extracted from the old game's playback system
- Spinner Implementation During Solving:
- Braille Block Spinner
    
# Dont reuse (better implement new): 

    - Hint Level Progression
    - SpinnerTalkBack compatibility for blind users

# Reusable Components from the Old Game
    1. SolverDD Algorithm:
        - The SolverDD class implements ISolver interface
        - Already reusable through the GameStateManager which initializes it
        - This component is ready for reuse without modification
    2. GameMove Representation:
        - Already used across both implementations
        - Represents a single robot move with start/end positions, robot ID, direction and distance
        - The old game converts these into visual robot movements

# Implementation Strategy
- dont extract the hint solution tracking, the rest start now. use gradlew to check
- To extract these components without changing the old game:
    1. Create utility classes for reusable components:
        - BrailleSpinner.java - Extract the Braille spinner animation
        - SolutionAnimator.java - Extract the solution animation logic
    2. Use interfaces to standardize communication between UI and logic:
        - SolutionDisplayManager to handle animation across different UIs
    3. Add methods to GameStateManager that use these components:
        - animateSolution() - Display solution animation
        - showSpinner(boolean show) - Toggle Braille spinner