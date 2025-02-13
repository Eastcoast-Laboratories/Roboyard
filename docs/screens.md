# Roboyard App Screens Overview

This document provides an overview of all screens in the Roboyard Android app and their navigation flow.

## Screen Structure

### 1. Main Menu (SCREEN_START)
The entry point of the app, providing navigation to all other screens.

**Features:**
- Navigation buttons to all main sections
- Quick access to random games
- Access to saved games
- Access to level games

### 2. Game Screen (SCREEN_GAME)
The main gameplay screen where the actual game takes place.

### 3. Settings (SCREEN_SETTINGS)
Configure game settings and preferences.

**Features:**
- Difficulty settings
- Board size selection (12x14, 14x14, 14x16)
- Sound settings (On/Off)

### 4. Credits (SCREEN_CREDITS)
Information about developers and contributors.

### 5. Level Selection Screens
A series of screens organizing levels by difficulty.

#### 5.1 Beginner Levels (SCREEN_LEVEL_BEGINNER)
- Levels 1-35
- Simple puzzles for beginners
- most maps are solveable in less than 10 moves

#### 5.2 Intermediate Levels (SCREEN_LEVEL_INTERMEDIATE)
- Levels 36-70
- Medium difficulty puzzles

#### 5.3 Advanced Levels (SCREEN_LEVEL_ADVANCED)
- Levels 71-105
- Advanced puzzles

#### 5.4 Expert Levels (SCREEN_LEVEL_EXPERT)
- Levels 106-140
- Most difficult puzzles

**Features:**
- Game board display
- Robot controls
- Move counter
- Timer
- AI assistance/solution hints
- Save game option
- Restart and back buttons

### 6. Save/Load Screen (SCREEN_SAVE_GAMES)
Manage game saves and loads.

**Features:**
- Autosave slot
- 34 save slots
- Save current game
- Load saved games
- Unique visual identifier-strings for saved games

## Navigation Flow

### Main Navigation
- Main Menu → Game Screen (in random game Mode)
- Main Menu → Level Selection Screens
- Main Menu → Save/Load Screen (in load-mode)
- Main Menu → Credits Screen
- Main Menu → Settings Screen
- All screens → Back to Main Menu screen

### Level Selection Navigation
- Level Selection → Game Screen (in level-mode)
- Level Selection → Adjacent difficulty levels
- Level Selection → Back to Main Menu

### Game Navigation
- Game Screen → Save/Load Screen (in save-mode)
- Game Screen → Main Menu (via system back button)
- Save/Load Screen → Game Screen (when loading)
- Save/Load Screen → Previous Screen (when saving)

### Mode-Specific Behavior
- When Accessed via Game Screen:
  - Save/Load Screen shows "Select slot to save map"
  - Save-mode functionality enabled
- When accessed via Main Menu:
  - Save/Load Screen shows "Load map"
  - Load-mode functionality enabled

## Screen Constants
All screen constants are defined in `Constants.java`