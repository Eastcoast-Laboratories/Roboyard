CHANGELOG
=========

### Version 19
- Added minimap display on each save game button for better game identification
- Implemented share functionality for saved maps
- Optimized performance by lowering framerate when not actively moving or interacting

### Version 18.2

### New Features
- In beginner and advanced level generate a new map each time
- Board size can now be selected from a dropdown with more options:
 - Added board sizes: 12x12, 12x14, 12x16, 12x18, 14x14, 14x16, 14x18, 16x16, 16x18, 16x20, 16x22, 18x18, 18x20 and 18x22.
- lone walls are now also allowed in advanced random games and harder
- more walls in each difficulty
- Level games now require finding the solution.

### Improvements
- Corrected star display at the top of the level screen.

### Bug Fixes
- Fixed an issue where the first savegame click led to unintended loading.
- Fixed one-move solution bug in level 36.
- Fixed support for larger maps beyond 16x16.
- Fix level 74 and 109 where the target was positioned on a robot
- fix number of hints until solution display (random clicks from 2-5)
- Treat one-move solutions correctly in solution counter


### Version 18.1

- Gameplay Improvements

  -  Enabled intermediate level at 35 stars instead of the previous threshold.
  -  Added "How to Play" section to the credits screen.


- UI Enhancements

  -  Displayed total stars at the top right of the level selection screen.


- Level Adjustments

  - Moved level 30 to the end and shifted all levels from 31 onwards by one down.
  - Adjusted final level 140 to require a minimum of 26 moves.
  - General level rearrangements for better progression.


- Bug Fixes

  -  Fixed a bug when starting a new random game.
  -  Ensured a new map is generated each time the board size changes.


- Performance Enhancements

  -  Implemented file access caching for faster level loading.

### Version 18

- add 3 stars to each level:
- one star if solved
- one star if solved with one move more than the minimum
- one star if solved with the minimum possible moves
- draw small robots underneath each robot to keep track of the original position
- map at the top of the screen and the hints and moves below
- bugfixes and speed improvement

### Version 17

- Unified menu on one page
- New Levels: 35 beginner-levels, 35 intermediate, advanced and expert levels
- UI improvements: Display of level numbers/names in-game
- Save and load functionality: Enhanced handling of save games
- New option for boards with 12x14, 14x14 or 14x16 squares (14x16 is the new default)
- New launcher icon

### Version 16

- Prevent solver freeze on rapid clicking while hint message is showing or solver is running.

### Version 15

- new desiged robots, targets and game area
- add extra touch tolerance if all robots are more than one square apart

### Version 14

- fix game levels
- show a unique string above every savegame and give it a unique background color

### Version 13.2

- add distributionSha256Sum to build options

### Version 13.1

- remove unnecessary INTERNET permission

### Version 13

- cycle through different solutions if more than one solution is found

### Version 12

- disable lock screen

### Version 10.3

- add links to imprint and privacy
- upgrade to SdkVersion 34

### Version 10.2

- Show number of different solutions found by the AI

### Version 11 (beta)

- cycle through different solutions if more than one solution is found
- The solver in this version is buggy! Use at your own risk! ;)

### Version 10.1

- shrink green robot, so it doesn't cover walls anymore
- remove white circles in the background of robots

### Version 9.0

- Add Impossible Mode with at least 17 moves
- Beginner Levels may only take max one second to compute
- fix: Level setting was not saved, if "Beginner" was selected
- fix: LevelGame selection are not re-generated anymore, so they can be solved now
- Default Level is now "Beginner"

### Version 8.1

- Popup messages moved to the bottom area
- New Launcher icon
- fixed puzzles with target in direct line of robot

### Version 8.0

- show number of squares moved next to number of moves
- direction intention arrows half transparent

### Version 7.1

- New Launcher Icon

### Version 7.0

- Adapted resolution to Android 4.1.1 with 480px width

### Version 6.1

- added Sound on/off in Game settings (icons from freeiconspng [1](https://www.freeiconspng.com/img/40963), [2](https://www.freeiconspng.com/img/40944))
- Add roboyard in the middle of the play field

### Version 6.0

- show solution as the 2nd to 5th hint
- persistently store Settings
- remove (slower) BFS Solver algorithm
- fix bug, that was extra autosaving when starting a new level

### Version 5.4

- added more tolerance to touch a robot

### Version 5.3

- Add ambient background sound
- green walls are now more like garden hedges
- walls on the right screen are now visible
- In beginner level generate a new map each time

### Version 5.2

- rename to Roboyard
- Walls are green and a bit thicker
- increase initial movement speed of robots with linear slow-down

### Version 5.1

- carré always in the middle again (fixes wrong robot positions due to keeping the initial playingfield)

### Version 5.0

- keep initial playingfield when starting the next game
- keep playingfield when loading a saved game

### Version 4.0

- added more complexity to Advanced and Insane Level

New in Advanced:

  - The square must not be in the middle
  - three lines allowed in the same row/column
  - no multi-color target

New in Insane:

  - solutions with 10 moves are enough
  - 50% chance that the target is set anywhere on the map instead of in a corner

### Version 3.2

- adapt to different screen resolutions

### Version 3.1

- Spheres are now Robots
- change next game button

### Version 3.0

- New design

### Version 2.5

- 35 savegames and levels per page
- Autosave the current game after 40s in save slot 0

### Version 2.4

- fix bug: no save button when playing a saved game (was crashing the game)

### Version 2.3

- Settings: set user level to show only puzzles with at least
  - Beginner: 4-6 moves
  - Advanced: 6-8 moves
  - Insane: 14 moves (10 moves since v4.0)
- Warning if set to slow BFS and insane level

### Version 2.2

- Show 3 to 5 Hints before showing the optimal solution

### Version 1.0

- last french version

### Änderungen

#### Deutsch
- Neues Design für die Roboter, Ziele und das Spielfeld
- Zusätzliche Berührungstoleranz, wenn alle Roboter mehr als ein Quadrat voneinander entfernt sind
- Verhindert das Einfrieren des Solvers bei schnellem Klicken, während die Hinweisnachricht angezeigt wird oder der Solver läuft.
- Version 16: fix game levels
- Version 16: show a unique string above every savegame and give it a unique background color
- Version 15: add distributionSha256Sum to build options

#### Englisch
- New designed robots, targets, and game area
- Add extra touch tolerance if all robots are more than one square apart
- Prevent solver freeze on rapid clicking while hint message is showing or solver is running.
- Version 16: fix game levels
- Version 16: show a unique string above every savegame and give it a unique background color
- Version 15: add distributionSha256Sum to build options

# These are all relevant changes since Version 1.0:

- Add Impossible Mode with at least 17 moves
- fixed puzzles with target in direct line of robot
- show number of squares moved next to number of moves
- New Launcher Icon
- Adapted resolution to Android 4.1.1 with 480px width
- added Sound on/off in Game settings
- show solution as the 2nd to 5th hint
- persistently store Settings
- remove (slower) BFS Solver algorithm
- added more tolerance to touch a robot
- Add ambient background sound
- Walls are green hedges (better visible)
- increase the initial movement speed of robots with linear slow-down
- Beginner, Advanced, Insane and Impossible Mode with minimum solutions allowed
- Spheres are now Robots
- 35 savegames and levels per page
- Autosave the current game after 40s in save slot 0
- fix bug: no save button when playing a saved game (was crashing the game)
