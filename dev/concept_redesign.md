# Concept for redesigning the game to use native Android UI elements:

## Core Architecture Changes
- Replace GameScreen with Fragment-based Screens:
  - Convert each GameScreen to an Android Fragment
  - Use FragmentManager for screen transitions
  - Leverage Android's navigation component for screen flow
- Replace GameButton with Native Buttons:
  - Use standard Android Button or MaterialButton components
  - Leverage Android's built-in accessibility support
  - Use ConstraintLayout for positioning instead of absolute coordinates
- Game Rendering Approach:
  - Use a hybrid approach where game elements are in native Views
  - Game grid/board could be a custom View inside a standard layout
  - UI controls would be standard Android widgets

## Complete One-Time Conversion Strategy

### 1. Preparation Phase
- Utilize existing screen inventory from [/docs/ARCHITECTURE.md](ARCHITECTURE.md) and [/docs/screens.md](screens.md)
- Map all GameScreen classes to equivalent Fragment designs
- Design XML layouts for all screens in advance
- Create a comprehensive navigation graph for all screen transitions
- Develop a state management system to replace the current GameManager

### 2. Core Infrastructure Development
- Create base classes that will be used across the application:
  - BaseGameFragment: Common functionality for all game screens
  - GameStateManager: Centralized state management (replacing GameManager)
  - ResourceProvider: Asset loading and resource management
  - NavigationController: Handle screen transitions

### 3. UI Component Development
- Create all XML layouts for every screen
- Develop custom Views for specialized game elements:
  - GameGridView: For rendering the game board/grid
  - MiniMapView: For rendering minimaps in save slots
  - AnimatedGameElement: For game objects that need animation

### 4. Full Conversion Implementation
- Implement all Fragment classes in parallel:
  - MainMenuFragment (replacing MainMenuGameScreen)
  - GamePlayFragment (replacing GridGameScreen)
  - SaveGameFragment (replacing SaveGameScreen)
  - SettingsFragment (replacing SettingsGameScreen)
  - All other game screens
- Implement a new MainActivity that uses the Navigation component
- Create adapters for any data structures that need to be displayed in RecyclerViews

### 5. Game Logic Integration
- Move game logic from current implementation to new architecture
- Separate game state from rendering completely
- Implement event listeners for UI interactions
- Create a unified event bus for communication between components

### 6. Data Migration
- Ensure save game compatibility with new implementation
- Create data converters if needed for existing save files
- Implement serialization/deserialization for game state

## Button Conversion Strategy

### 1. Button Mapping
```java
// Current GameButton properties to map to native buttons
public class ButtonMapping {
    String contentDescription;  // Maps directly to Android's contentDescription
    float x, y;                // Convert to ConstraintLayout constraints
    float width, height;       // Convert to layout_width, layout_height
    int textColor;             // Maps to textColor attribute
    String text;               // Maps to text attribute
    boolean enabled;           // Maps to enabled attribute
    Bitmap background;         // Convert to background drawable resource
    
    // GameButton click handlers map to OnClickListener
    public interface OnClickAction {
        void onClick(GameManager gameManager);
    }
}
```

### 2. Automated Button Conversion Process

1. **Extract Button Properties**
   ```java
   // For each GameButton instance in the codebase
   Map<String, ButtonProperties> buttonMap = new HashMap<>();
   
   // Extract properties from each GameButton
   for (GameButton button : allGameButtons) {
       ButtonProperties props = new ButtonProperties();
       props.id = generateUniqueId(button);
       props.text = button.getText();
       props.contentDescription = button.getContentDescription();
       props.x = button.getPositionX();
       props.y = button.getPositionY();
       props.width = button.getWidth();
       props.height = button.getHeight();
       props.enabled = button.isEnabled();
       props.clickAction = button.getOnClickAction();
       
       buttonMap.put(props.id, props);
   }
   ```

2. **Generate XML Layout**
   ```xml
   <!-- Example generated button in ConstraintLayout -->
   <Button
       android:id="@+id/button_play"
       android:layout_width="120dp"
       android:layout_height="48dp"
       android:text="Play"
       android:contentDescription="Start a new game"
       app:layout_constraintStart_toStartOf="parent"
       app:layout_constraintTop_toTopOf="parent"
       app:layout_constraintWidth_percent="0.3"
       android:layout_marginStart="16dp"
       android:layout_marginTop="120dp" />
   ```

3. **Convert Click Handlers**
   ```java
   // In Fragment's onCreateView
   Button playButton = view.findViewById(R.id.button_play);
   playButton.setOnClickListener(v -> {
       // Convert original GameButton click action
       gameStateManager.startNewGame();
       // Navigate to game screen
       NavDirections action = MainMenuFragmentDirections.actionMainMenuToGamePlay();
       Navigation.findNavController(view).navigate(action);
   });
   ```

### 3. Special Button Types

1. **GameButtonGotoSavedGame Conversion**
   ```java
   // Convert to RecyclerView with custom adapter
   public class SavedGameAdapter extends RecyclerView.Adapter<SavedGameViewHolder> {
       private List<SavedGameInfo> savedGames;
       private GameStateManager gameStateManager;
       
       @Override
       public void onBindViewHolder(SavedGameViewHolder holder, int position) {
           SavedGameInfo game = savedGames.get(position);
           holder.nameText.setText(game.getName());
           holder.dateText.setText(game.getDate());
           holder.miniMapView.setMapData(game.getMapData());
           
           holder.itemView.setOnClickListener(v -> {
               gameStateManager.loadGame(game.getId());
               // Navigate to game screen
               NavDirections action = SaveGameFragmentDirections.actionSaveGameToGamePlay();
               Navigation.findNavController(holder.itemView).navigate(action);
           });
       }
   }
   ```

2. **Custom Styled Buttons**
   ```xml
   <!-- styles.xml -->
   <style name="GameButton">
       <item name="android:background">@drawable/game_button_background</item>
       <item name="android:textColor">@color/game_button_text</item>
       <item name="android:textSize">18sp</item>
       <item name="android:padding">12dp</item>
   </style>
   
   <!-- Custom button background -->
   <drawable name="game_button_background">
       <shape android:shape="rectangle">
           <corners android:radius="8dp" />
           <solid android:color="@color/primary" />
           <stroke android:width="2dp" android:color="@color/accent" />
       </shape>
   </drawable>
   ```

## Implementation Details

### MainActivity Implementation
```java
public class MainActivity extends AppCompatActivity {
    private GameStateManager gameStateManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize the game state manager
        gameStateManager = new GameStateManager(this);
        
        // Make the state manager available to all fragments
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(GameStateManager.class)) {
                    return (T) gameStateManager;
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        };
        
        // Set up the NavController
        NavHostFragment navHostFragment = 
            (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
    }
}
```

### Fragment Implementation Example
```java
public class MainMenuFragment extends BaseGameFragment {
    private GameStateManager gameStateManager;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        
        // Set up buttons with proper accessibility
        Button newGameButton = view.findViewById(R.id.new_game_button);
        Button loadGameButton = view.findViewById(R.id.load_game_button);
        Button settingsButton = view.findViewById(R.id.settings_button);
        
        newGameButton.setOnClickListener(v -> {
            NavDirections action = MainMenuFragmentDirections.actionMainMenuToGamePlay();
            Navigation.findNavController(view).navigate(action);
        });
        
        loadGameButton.setOnClickListener(v -> {
            NavDirections action = MainMenuFragmentDirections.actionMainMenuToSaveGame();
            Navigation.findNavController(view).navigate(action);
        });
        
        settingsButton.setOnClickListener(v -> {
            NavDirections action = MainMenuFragmentDirections.actionMainMenuToSettings();
            Navigation.findNavController(view).navigate(action);
        });
        
        return view;
    }
}
```

### Game Grid Implementation
```java
public class GameGridView extends View {
    private GameStateManager gameStateManager;
    private Paint cellPaint;
    private Paint robotPaint;
    private float cellSize;
    
    // Constructor and initialization
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Get current game state
        GameState state = gameStateManager.getCurrentState();
        
        // Draw grid cells
        for (int y = 0; y < state.getBoardHeight(); y++) {
            for (int x = 0; x < state.getBoardWidth(); x++) {
                drawCell(canvas, x, y, state.getCellType(x, y));
            }
        }
        
        // Draw robots and other game elements
        for (GameElement element : state.getGameElements()) {
            drawGameElement(canvas, element);
        }
    }
    
    // Implement accessibility for the game grid
    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Use Android's accessibility framework
        return AccessibilityHelper.dispatchHoverEvent(this, event, gameStateManager) || 
               super.dispatchHoverEvent(event);
    }
    
    // Handle touch events for game interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Convert touch to game coordinates
        int x = (int)(event.getX() / cellSize);
        int y = (int)(event.getY() / cellSize);
        
        // Process game input
        gameStateManager.handleGridTouch(x, y, event.getAction());
        invalidate(); // Redraw
        
        return true;
    }
}
```

## AI-Driven Execution Plan

### Phase 1: Analysis & Design
- Analyze existing codebase structure and dependencies (using Architecture.md)
- Generate complete navigation graph for all screen transitions
- Design XML layouts for all screens
- Create data models for game state management

### Phase 2: Core Implementation
- Generate all Fragment classes and layouts simultaneously
- Implement GameStateManager with full state handling
- Create custom views for specialized game elements
- Develop navigation controller and screen transitions

### Phase 3: Game Logic Integration
- Port game logic from current implementation
- Implement event system for UI-to-game communication
- Create adapters for all data structures
- Implement new save/load system no data migration needed (the users will loose their savegames, that is ok)

### Phase 4: Testing & Refinement
- Generate comprehensive test suite with unittests
- Simulate user interactions across all screens
- Verify accessibility compliance for TalkBack
- Optimize performance bottlenecks
- enhance documentation, using existing files in /docs/

