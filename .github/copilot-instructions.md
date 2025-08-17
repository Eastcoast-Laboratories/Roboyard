# Roboyard - Android Puzzle Game
Roboyard is an Android puzzle game inspired by "Ricochet Robots" written in Java. Players control four robots in a maze to guide them to targets. The game features an AI solver, 140 levels, and supports 6 languages.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Critical Build Limitations
- **NETWORK DEPENDENCY**: Build requires internet access to download dependencies from Google Maven and other repositories
- **BUILD FAILURE**: `./gradlew build` and `./gradlew build --offline` both fail due to missing cached dependencies 
- **NO ONLINE ACCESS**: The development environment has no internet connectivity (dl.google.com and google.com are unreachable)
- **WORKAROUND**: Use alternative validation methods described below

### Bootstrap and Environment Setup
- Java 17 is pre-installed at `/usr/lib/jvm/temurin-17-jdk-amd64`
- Android SDK is available at `/usr/local/lib/android/sdk` with platforms android-33 through android-36
- Gradle 8.13 downloads automatically on first run
- Add Android tools to PATH: `export PATH=$PATH:$ANDROID_HOME/platform-tools`
- ADB is available after adding to PATH but no devices are connected

### Project Structure Validation
- **Source Code**: 92 Java files in `app/src/main/java/` (no Kotlin files)
- **Main Package**: `roboyard.eclabs` - core game logic and UI
- **AI Package**: `roboyard.pm.ia` - artificial intelligence for solving puzzles  
- **Solver Package**: `driftingdroids.model` - integrated high-performance Ricochet Robots solver
- **Game Levels**: 140 level files in `app/src/main/assets/Maps/` (level_1.txt through level_140.txt)
- **Resources**: Multi-language support (English, German, Spanish, French, Korean, Chinese)
- **Tests**: 8 Android instrumentation test files in `app/src/androidTest/`

### Working Commands That Function Without Network
```bash
# Check project structure
find app/src/main/java -name "*.java" | wc -l  # Should return 92
ls app/src/main/assets/Maps/ | wc -l           # Should return 140

# Validate Android environment  
echo $ANDROID_HOME                             # Should show /usr/local/lib/android/sdk
ls $ANDROID_HOME/platforms/                    # Should show android-33 through android-36

# Check Java version
javac -version                                 # Should show javac 17.0.16

# Run translation analysis (WORKS - takes ~10 seconds)
mkdir -p /tmp/translation_check
sed 's|/var/www/Roboyard|'$(pwd)'|g' dev/simple_check.sh > /tmp/translation_check/modified_check.sh
sed -i 's|/tmp/translation_check|/tmp/translation_output|g' /tmp/translation_check/modified_check.sh
chmod +x /tmp/translation_check/modified_check.sh
/tmp/translation_check/modified_check.sh
```

### Commands That DO NOT WORK (Network Required)
```bash
# These commands fail due to network dependency - DO NOT USE:
./gradlew build                    # Fails: cannot resolve dependencies from dl.google.com
./gradlew build --offline         # Fails: no cached dependencies available
./gradlew test                     # Fails: same network dependency issues
./gradlew assembleDebug           # Fails: same network dependency issues
```

## Development Workflow

### Key Source Files
- `app/src/main/java/roboyard/eclabs/GameManager.java` - Main game state management
- `app/src/main/java/roboyard/eclabs/ui/MainActivity.java` - Main Android activity
- `app/src/main/java/roboyard/eclabs/solver/SolverDD.java` - AI solver integration
- `app/src/main/java/roboyard/logic/core/Constants.java` - Game constants and screen IDs
- `app/build.gradle` - Android build configuration (API 21-36, version 28, build 93)

### Level File Format
Level files use a custom text format with:
- `board:width,height;` - defines grid dimensions
- `mh[x],[y];` - horizontal walls  
- `mv[x],[y];` - vertical walls
- `target_[color][x],[y];` - goal targets
- `robot_[color][x],[y];` - robot starting positions

### Validation Without Building
```bash
# Validate level files exist and are properly formatted
head -5 app/src/main/assets/Maps/level_1.txt   # Should show board: dimensions and walls
wc -l app/src/main/assets/Maps/level_1.txt     # Should show ~114 lines

# Check translation completeness (generates report)
# German: 85% complete (304/354 strings)
# Spanish: 80% complete (286/354 strings)  
# French: 49% complete (174/354 strings)
# Korean: 36% complete (129/354 strings)
# Chinese: 69% complete (245/354 strings)

# Validate main Java class structure
grep -n "class GameManager" app/src/main/java/roboyard/eclabs/GameManager.java
grep -n "package roboyard.eclabs" app/src/main/java/roboyard/eclabs/*.java | head -5
```

## Testing Strategy

### Manual Testing Required
Since automated builds fail, you must manually validate:
1. **Code Structure**: Verify Java syntax and imports are correct
2. **Resource References**: Check that XML resources match Java references
3. **Level Format**: Validate game level files follow the expected format
4. **Translation Keys**: Ensure string resources are properly referenced

### Available Test Files
- `app/src/androidTest/java/roboyard/eclabs/ui/SaveGameFragmentTest.java` - UI testing for save/load
- `app/src/androidTest/java/roboyard/eclabs/ui/MiniMapViewTest.java` - Minimap display testing
- `app/src/androidTest/java/roboyard/eclabs/ui/MainMenuFragmentTest.java` - Menu navigation testing

### Validation Scenarios
After making changes, manually verify:
1. **Java Compilation**: Check syntax with `javac -version` and examine imports
2. **Resource Integrity**: Validate XML resources exist for new string references
3. **Level Loading**: Check level file format matches expected structure
4. **Translation Coverage**: Run translation analysis to verify new strings are translatable

## Development Scripts

### Working Scripts (No Network Required)
```bash
# Translation analysis - WORKS, takes ~10 seconds
./dev/simple_check.sh  # (after path modification as shown above)

# Level management scripts
./dev/scripts/reorder_levels.sh     # Reorders level files
./reorder_levels.py                 # Python script for level reordering

# String analysis  
./dev/remove_unused_strings_from_all.sh    # Cleans unused string resources
```

### Development Tools Location
- `dev/` - Development scripts and documentation
- `dev/scripts/` - Level management and build helper scripts
- `tools/` - Additional utility scripts
- `docs/` - Comprehensive project documentation

## Architecture Notes

### Core Components
1. **Game Logic Layer** (`roboyard.logic.*`) - Platform-independent game rules
2. **UI Layer** (`roboyard.ui.*`) - Android-specific user interface  
3. **AI Layer** (`roboyard.pm.ia.*`) - Game AI and solver interface
4. **DriftingDroids** (`driftingdroids.model.*`) - High-performance puzzle solver

### Key Design Patterns
- **Bridge Pattern**: `SolverDD` bridges Roboyard's game logic with DriftingDroids solver
- **Screen Management**: Game screens managed through `Constants.java` screen IDs (0-9)
- **Resource Management**: Multi-language support with fallback to English

## Common Tasks Reference

### Repository Root Structure
```
.
├── app/                    # Android application module
├── build.gradle           # Root Gradle build file
├── docs/                  # Project documentation  
├── dev/                   # Development tools and scripts
├── fastlane/             # Deployment automation
├── gradle/               # Gradle wrapper files
├── gradlew               # Gradle wrapper script (Unix)
├── gradlew.bat           # Gradle wrapper script (Windows)
└── README.md             # Project overview
```

### Package Structure
```
app/src/main/java/
├── driftingdroids/       # Integrated solver library
├── roboyard/
│   ├── eclabs/          # Main game package (9 Java files)
│   ├── logic/           # Game logic (platform-independent)
│   ├── pm/              # Project management and AI
│   └── ui/              # Android UI components
```

### Critical Limitations
- **NEVER** attempt `./gradlew build` - it will fail due to network restrictions
- **NO** automated testing available - requires manual validation
- **NO** APK generation possible in this environment
- **ALWAYS** validate changes through code inspection and resource checking
- **USE** translation analysis script to verify internationalization changes

Focus on code analysis, documentation updates, and resource validation since build automation is not available in this environment.