# Map Generation Lifecycle Concept

- only generate a new game with new walls and positions if one of these:
- the button "New Game" or the dice-button when `generateNewMapEachTime == false` in the game screen is pressed
- the button "Next Game" when `generateNewMapEachTime == true` in the game screen is pressed
- the settings are changed (board size, target count)


# TODO:

1. if you Load a savegame (from load menu) and generateNewMapEachTime == false store walls from the loaded savegame for the ratio of the savegame in the wall storage 
When pressed "Next Game" and  generateNewMapEachTime == false** → reuse walls if the loaded map has the same ratio as in settings

2. when you Press "Play" with existing autosave
- **If settings changed** (board size, target count) → start new game with new settings
- **If settings unchanged** → load autosave, keep positions
- Detection: check autosave metadata (boardW, boardH, targetCount)
