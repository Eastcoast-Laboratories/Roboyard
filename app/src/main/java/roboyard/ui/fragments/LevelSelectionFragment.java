package roboyard.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import roboyard.logic.core.Constants;
import roboyard.eclabs.R;
import roboyard.ui.achievements.AchievementManager;
import timber.log.Timber;
import roboyard.logic.core.LevelCompletionData;
import roboyard.ui.components.LevelCompletionManager;

/**
 * Level selection screen implemented as a Fragment with native Android UI components.
 * Allows users to select predefined levels to play.
 */
public class LevelSelectionFragment extends BaseGameFragment {

    private RecyclerView levelRecyclerView;
    private LevelAdapter levelAdapter;
    private TextView titleTextView;
    private TextView starsTextView;
    private final List<Integer> availableLevels = new ArrayList<>();
    private LevelCompletionManager completionManager;
    private int totalStars = 0;

    // Constants for custom level support
    private static final int CUSTOM_LEVEL_START_ID = 141;
    private static final int STARS_PER_LEVEL = 1; // Number of stars required per level

    /**
     * Interface for handling level selection events.
     * When a level button is clicked, this listener is called with the level ID.
     */
    public interface OnLevelSelectedListener {
        /**
         * Called when a level is selected.
         * @param levelId The ID of the selected level
         */
        void onLevelSelected(int levelId);
    }

    /**
     * Creates the view for this fragment.
     * Sets up the RecyclerView with a grid layout and initializes the LevelCompletionManager
     * to track which levels have been completed.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that this fragment's UI should be attached to
     * @param savedInstanceState Previous state of this fragment, if available
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_level_selection, container, false);

        // Set up UI elements
        titleTextView = view.findViewById(R.id.level_selection_title);
        starsTextView = view.findViewById(R.id.stars_count_text);
        levelRecyclerView = view.findViewById(R.id.level_recycler_view);

        // Set title
        titleTextView.setText(getString(R.string.level_selection_title));

        // Set up RecyclerView with grid layout (3 columns in portrait, 6 in landscape)
        int spanCount = getResources().getConfiguration().orientation == 
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 6 : 3;
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        levelRecyclerView.setLayoutManager(layoutManager);

        // Add a custom ItemDecoration with thin separator lines between grid items
        levelRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            private final android.graphics.Paint paint = new android.graphics.Paint();
            
            {
                paint.setColor(android.graphics.Color.parseColor("#20000000")); // Very light gray (20% opacity)
                paint.setStrokeWidth(1); // 1px thin line
            }
            
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, 
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                // Set minimal margins for separator lines
                int spacing = 1; // 1dp
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
            }
            
            @Override
            public void onDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView parent, 
                              @NonNull RecyclerView.State state) {
                int childCount = parent.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = parent.getChildAt(i);
                    
                    // Draw bottom line
                    c.drawLine(child.getLeft(), child.getBottom(), 
                              child.getRight(), child.getBottom(), paint);
                    
                    // Draw right line
                    c.drawLine(child.getRight(), child.getTop(), 
                              child.getRight(), child.getBottom(), paint);
                }
            }
        });

        // Get the level completion manager
        completionManager = LevelCompletionManager.getInstance(requireContext());

        // Get total stars
        totalStars = completionManager.getTotalStars();

        // Load available levels
        loadAvailableLevels();

        // Display total stars with format X/420
        if (starsTextView != null) {
            starsTextView.setText(String.format("%d/%d", totalStars, 420));
        }

        // Set up adapter
        levelAdapter = new LevelAdapter(availableLevels, this, completionManager, totalStars);
        levelRecyclerView.setAdapter(levelAdapter);

        // Auto-scroll to the last played level
        scrollToLastPlayedLevel();

        // Set up back button
        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            // Navigate back to the main menu
            MainMenuFragment menuFragment = new MainMenuFragment();
            navigateToDirect(menuFragment);
        });

        // Show Level Editor button if all 140 levels (except 139) are unlocked
        Button levelEditorButton = view.findViewById(R.id.level_editor_button);
        if (levelEditorButton != null) {
            updateLevelEditorButtonVisibility(levelEditorButton);
            levelEditorButton.setOnClickListener(v -> {
                Timber.d("[LEVEL_SELECTION] Opening Level Design Editor");
                LevelDesignEditorFragment editorFragment = LevelDesignEditorFragment.newInstance(0);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, editorFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Stop map regeneration when in level selection screen
        if (gameStateManager != null) {
            gameStateManager.stopRegeneration();
            Timber.d("[SOLVER] Stopped regeneration in level selection screen");
        }
    }

    /**
     * Scroll to the last played level automatically, positioning it in the middle of the screen
     */
    private void scrollToLastPlayedLevel() {
        int lastPlayedLevel = completionManager.getLastPlayedLevel();
        
        // Find the position of the last played level in the list
        int position = availableLevels.indexOf(lastPlayedLevel);
        
        if (position >= 0) {
            final int scrollPosition = position;
            levelRecyclerView.post(() -> {
                // Get the LinearLayoutManager to scroll to position with offset
                androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                    (androidx.recyclerview.widget.LinearLayoutManager) levelRecyclerView.getLayoutManager();
                
                if (layoutManager != null) {
                    // Calculate offset to center the item on screen
                    // Get the height of the RecyclerView and item height
                    int recyclerViewHeight = levelRecyclerView.getHeight();
                    int itemHeight = 120; // Approximate height of level item
                    int offset = (recyclerViewHeight / 2) - (itemHeight / 2);
                    
                    // Scroll to position with offset to center it
                    layoutManager.scrollToPositionWithOffset(scrollPosition, offset);
                    Timber.d("Scrolling to last played level %d at position %d with offset %d", 
                            lastPlayedLevel, scrollPosition, offset);
                } else {
                    // Fallback to smooth scroll
                    levelRecyclerView.smoothScrollToPosition(scrollPosition);
                }
            });
        } else {
            // Fallback: scroll to first level
            Timber.d("Last played level %d not found, scrolling to first level", lastPlayedLevel);
        }
    }

    /**
     * Called when the fragment is resumed.
     * This is important for updating the completion stars when returning from a game.
     * If a level was completed during gameplay, we need to refresh the adapter
     * to show the star icon when the user returns to this screen.
     * 
     * Note: If stars are not showing, check that:
     * 1. The LevelCompletionManager is properly initialized
     * 2. The level was actually marked as completed in GameStateManager.setGameComplete()
     * 3. The Gson library is properly included in the build.gradle dependencies
     * 4. The star drawable exists in the drawable folder
     */
    @Override
    public void onResume() {
        super.onResume();
        // Update total stars
        totalStars = completionManager.getTotalStars();

        // Update the stars text view with format X/420
        if (starsTextView != null) {
            starsTextView.setText(String.format("%d/%d", totalStars, 420));
        }

        // Refresh the adapter to update completion stars when returning to this screen
        if (levelAdapter != null) {
            levelAdapter.updateTotalStars(totalStars);
            levelAdapter.notifyDataSetChanged();
        }

        // Auto-scroll to the last played level
        scrollToLastPlayedLevel();

        // Update Level Editor button visibility
        if (getView() != null) {
            Button levelEditorButton = getView().findViewById(R.id.level_editor_button);
            if (levelEditorButton != null) {
                updateLevelEditorButtonVisibility(levelEditorButton);
            }
        }
    }

    /**
     * Shows the Level Editor button if the player has earned at least 140 stars total.
     */
    private void updateLevelEditorButtonVisibility(Button button) {
        int totalStars = completionManager.getTotalStars();
        boolean visible = totalStars >= 140;
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        Timber.d("[LEVEL_SELECTION] Total stars: %d, Level Editor button visible: %b", totalStars, visible);
    }

    /**
     * Loads available levels from the assets/Maps directory.
     * Looks for files with the pattern "level_X.txt" where X is the level number.
     * Also checks for custom levels in internal storage.
     * The loaded level IDs are stored in the availableLevels list and sorted numerically.
     */
    private void loadAvailableLevels() {
        availableLevels.clear();
        try {
            // Load built-in levels (1-140)
            String[] files = getActivity().getAssets().list("Maps");
            for (String file : files) {
                if (file.startsWith("level_") && file.endsWith(".txt")) {
                    try {
                        int levelId = Integer.parseInt(file.substring(6, file.length() - 4));
                        availableLevels.add(levelId);
                    } catch (NumberFormatException e) {
                        // Skip files with invalid level IDs
                    }
                }
            }

            // Check for custom levels (141+) in internal storage
            File internalDir = requireContext().getFilesDir();
            File[] internalFiles = internalDir.listFiles();
            if (internalFiles != null) {
                for (File file : internalFiles) {
                    String fileName = file.getName();
                    if (fileName.startsWith("custom_level_") && fileName.endsWith(".txt")) {
                        try {
                            int levelId = Integer.parseInt(fileName.substring("custom_level_".length(), fileName.length() - 4));
                            availableLevels.add(levelId);
                        } catch (NumberFormatException e) {
                            // Skip files with invalid level IDs
                        }
                    }
                }
            }

            // Sort levels by ID
            Collections.sort(availableLevels);

        } catch (IOException e) {
            Timber.e(e, "Error loading levels");
        }

        // Calculate total stars earned
        calculateTotalStars();

        // Create and set the adapter
        levelAdapter = new LevelAdapter(availableLevels, this, completionManager, totalStars);
        levelRecyclerView.setAdapter(levelAdapter);
    }

    /**
     * Calculate the total number of stars earned across all levels
     */
    private void calculateTotalStars() {
        totalStars = 0;
        for (Integer levelId : availableLevels) {
            LevelCompletionData data = completionManager.getLevelCompletionData(levelId);
            if (data != null) {
                totalStars += data.getStars();
            }
        }
    }

    /**
     * Handles the selection of a level from the grid.
     * When a level is selected, this method:
     * 1. Starts a new level game with the selected level ID
     * 2. Navigates to the GameFragment to display the game
     * 
     * After completing the level, the user will return to this screen,
     * and the onResume method will refresh the adapter to show completion stars.
     * 
     * @param levelId The ID of the selected level
     */
    public void onLevelSelected(int levelId) {
        Timber.d("Selected level: %d", levelId);

        // Custom levels are always unlocked, regular levels have star requirements
        boolean isCustomLevel = levelId >= CUSTOM_LEVEL_START_ID;
        boolean isUnlocked = isCustomLevel || 
                (STARS_PER_LEVEL * (levelId - 1) <= totalStars);

        if (!isUnlocked) {
            int starsNeeded = (levelId - 1) * STARS_PER_LEVEL - totalStars;
            // TODO: this toast is never shown
            Toast.makeText(requireContext(), 
                    getString(R.string.level_locked, starsNeeded),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset achievement game session flags for new game
        AchievementManager.getInstance(requireContext()).onNewGameStarted();
        
        // Start a new game with the selected level
        gameStateManager.startLevelGame(levelId);

        // For UI, use GameFragment
        GameFragment gameFragment = new GameFragment();
        navigateToDirect(gameFragment);
    }

    /**
     * Returns the title for this screen to be displayed in the UI.
     * 
     * @return The screen title from resources
     */
    @Override
    public String getScreenTitle() {
        return getString(R.string.level_selection_title);
    }


    /**
     * Adapter for the level selection grid.
     * This adapter is responsible for creating and binding ViewHolders that display level buttons
     * and completion stars for levels that have been completed.
     */
    private class LevelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_LEVEL = 1;

        private final List<Integer> levels;
        private final LevelSelectionFragment fragment;
        private final LevelCompletionManager completionManager;
        private int totalStars;

        public LevelAdapter(List<Integer> levels, LevelSelectionFragment fragment, 
                          LevelCompletionManager completionManager, int totalStars) {
            this.levels = levels;
            this.fragment = fragment;
            this.completionManager = completionManager;
            this.totalStars = totalStars;
        }

        @Override
        public int getItemViewType(int position) {
            // Check if this position is a header
            if (position == 0) {
                return VIEW_TYPE_HEADER; // "Standard Levels" header
            }

            // Find the position where custom levels start
            int customLevelStartPosition = -1;
            for (int i = 0; i < levels.size(); i++) {
                if (levels.get(i) >= CUSTOM_LEVEL_START_ID) {
                    customLevelStartPosition = i;
                    break;
                }
            }

            // If custom levels exist and this is the position before the first custom level,
            // it's the "Custom Levels" header
            if (customLevelStartPosition >= 0 && position == customLevelStartPosition + 1) {
                return VIEW_TYPE_HEADER;
            }

            return VIEW_TYPE_LEVEL;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {
                View headerView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_level_header, parent, false);
                return new HeaderViewHolder(headerView);
            } else {
                View levelView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_level, parent, false);
                return new LevelViewHolder(levelView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                // Bind header
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

                // First header is always "Standard Levels"
                if (position == 0) {
                    headerHolder.bind("Standard Levels");
                    return;
                }

                // Find the position where custom levels start
                int customLevelStartPosition = -1;
                for (int i = 0; i < levels.size(); i++) {
                    if (levels.get(i) >= CUSTOM_LEVEL_START_ID) {
                        customLevelStartPosition = i;
                        break;
                    }
                }

                // If this is the custom levels header
                if (customLevelStartPosition >= 0 && position == customLevelStartPosition + 1) {
                    headerHolder.bind("Custom Levels");
                }
            } else if (holder instanceof LevelViewHolder) {
                // Calculate the actual level position in the levels list
                int levelIndex = position;

                // Skip the "Standard Levels" header
                levelIndex--;

                // Find the position where custom levels start
                int customLevelStartPosition = -1;
                for (int i = 0; i < levels.size(); i++) {
                    if (levels.get(i) >= CUSTOM_LEVEL_START_ID) {
                        customLevelStartPosition = i;
                        break;
                    }
                }

                // If we've passed the custom levels header, skip that too
                if (customLevelStartPosition >= 0 && position > customLevelStartPosition + 1) {
                    levelIndex--;
                }

                // Make sure we don't go out of bounds
                if (levelIndex >= 0 && levelIndex < levels.size()) {
                    int levelId = levels.get(levelIndex);
                    LevelViewHolder levelHolder = (LevelViewHolder) holder;

                    // Check if the level is completed
                    boolean isCompleted = completionManager.isLevelCompleted(levelId);

                    // Get stars earned for this level
                    int starsEarned = 0;
                    if (isCompleted) {
                        LevelCompletionData completionData = completionManager.getLevelCompletionData(levelId);
                        starsEarned = completionData.getStars();
                    }

                    // Custom levels are always unlocked
                    boolean isUnlocked = levelId >= CUSTOM_LEVEL_START_ID || 
                            // Regular levels unlock based on total stars
                            (STARS_PER_LEVEL * (levelId - 1) <= totalStars);

                    // Bind the level data
                    levelHolder.bind(levelId, fragment, isCompleted, starsEarned, isUnlocked);
                }
            }
        }

        @Override
        public int getItemCount() {
            // Count the regular items
            int count = levels.size();

            // Add the "Standard Levels" header
            count++;

            // Check if we need to add the "Custom Levels" header
            boolean hasCustomLevels = false;
            for (int levelId : levels) {
                if (levelId >= CUSTOM_LEVEL_START_ID) {
                    hasCustomLevels = true;
                    break;
                }
            }

            if (hasCustomLevels) {
                // Add the "Custom Levels" header
                count++;
            }

            return count;
        }

        /**
         * Update the total stars count
         * @param totalStars New total stars count
         */
        public void updateTotalStars(int totalStars) {
            this.totalStars = totalStars;
            notifyDataSetChanged();
        }
    }

    /**
     * ViewHolder for section headers
     */
    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.header_text);
        }

        public void bind(String headerText) {
            headerTextView.setText(headerText);
        }
    }

    /**
     * ViewHolder for level items in the RecyclerView.
     * Contains a button for the level and an ImageView for the completion star.
     */
    private static class LevelViewHolder extends RecyclerView.ViewHolder {
        private final Button levelButton;
        private final ImageView starOne;
        private final ImageView starTwo;
        private final ImageView starThree;
        private final ImageView starFour;
        private final LinearLayout statsOverlay;
        private final TextView levelNameText;
        private final TextView movesText;
        private final TextView timeText;

        /**
         * Constructor for the LevelViewHolder.
         * Finds and stores references to the level button and completion star views.
         * 
         * @param itemView The root view of the level item
         */
        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            levelButton = itemView.findViewById(R.id.level_button);
            starOne = itemView.findViewById(R.id.level_star_1);
            starTwo = itemView.findViewById(R.id.level_star_2);
            starThree = itemView.findViewById(R.id.level_star_3);
            starFour = itemView.findViewById(R.id.level_star_4);
            statsOverlay = itemView.findViewById(R.id.stats_overlay);
            levelNameText = itemView.findViewById(R.id.level_name_text);
            movesText = itemView.findViewById(R.id.level_moves_text);
            timeText = itemView.findViewById(R.id.level_time_text);
        }

        /**
         * Binds data to this ViewHolder.
         * This method:
         * 1. Sets the level number on the button (only visible for non-completed levels)
         * 2. Sets a click listener to handle level selection
         * 3. Shows the appropriate number of stars for completed levels
         * 4. Shows the statistics directly on the button for completed levels
         * 5. Enables or disables the button based on whether the level is unlocked
         * 6. Highlights the last played level with a light green background
         * 
         * @param levelId The ID of the level to display
         * @param listener The listener to handle level selection events
         * @param isCompleted Whether the level has been completed
         * @param starsEarned Number of stars earned for this level (0-3)
         * @param isUnlocked Whether the level is unlocked
         */
        public void bind(int levelId, LevelSelectionFragment fragment, boolean isCompleted, 
                        int starsEarned, boolean isUnlocked) {
            // Set level number (only visible for non-completed levels)
            levelButton.setText(String.valueOf(levelId));
            levelButton.setContentDescription("Level " + levelId);
            
            // Highlight the last played level with a light green background
            int lastPlayedLevel = LevelCompletionManager.getInstance(itemView.getContext()).getLastPlayedLevel();
            if (levelId == lastPlayedLevel) {
                itemView.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")); // Light green
            } else {
                itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT); // Transparent for others
            }

            // Get the completion data if the level is completed
            final LevelCompletionData completionData = isCompleted ?
                    LevelCompletionManager.getInstance(itemView.getContext())
                        .getLevelCompletionData(levelId) : null;

            // Show stars based on stars earned
            starOne.setVisibility(starsEarned >= 1 ? View.VISIBLE : View.GONE);
            starTwo.setVisibility(starsEarned >= 2 ? View.VISIBLE : View.GONE);
            starThree.setVisibility(starsEarned >= 3 ? View.VISIBLE : View.GONE);
            starFour.setVisibility(starsEarned >= 4 ? View.VISIBLE : View.GONE);

            // Handle stats display
            if (completionData != null) {
                // Hide the level number text when showing stats
                levelButton.setText("");

                // Explicitly set the statsOverlay to VISIBLE
                statsOverlay.setVisibility(View.VISIBLE);

                // Set level name
                levelNameText.setText("Level " + levelId);
                levelNameText.setVisibility(View.VISIBLE); // Ensure visibility

                // Format moves/robots: "optimal/moves robots:count" (swapped from moves/optimal to optimal/moves)
                String movesRobots = String.format("%d/%d %s%d", 
                        completionData.getOptimalMoves(),
                        completionData.getMovesNeeded(),
                        fragment.getString(R.string.level_robots_label),
                        completionData.getRobotsUsed());
                movesText.setText(movesRobots);
                movesText.setVisibility(View.VISIBLE); // Ensure visibility

                // Format time/squares: "0:12 squares:10"
                long timeMs = completionData.getTimeNeeded();
                long seconds = timeMs / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                String timeSquares = String.format("%d:%02d %s%d", 
                        minutes, seconds, 
                        fragment.getString(R.string.level_squares_label),
                        completionData.getSquaresSurpassed());
                timeText.setText(timeSquares);
                timeText.setVisibility(View.VISIBLE); // Ensure visibility

                // Log debug information
                // Timber.d("Level %d stats - Name: %s, Moves: %s, Time: %s", 
                //         levelId, levelNameText.getText(), movesText.getText(), timeText.getText());
            } else {
                // If not completed, make sure the stats overlay is hidden
                statsOverlay.setVisibility(View.GONE);
                levelNameText.setVisibility(View.GONE);
                movesText.setVisibility(View.GONE);
                timeText.setVisibility(View.GONE);
            }

            // Set click listener on the entire item view AND the button
            View.OnClickListener clickListener = v -> fragment.onLevelSelected(levelId);
            levelButton.setOnClickListener(clickListener);
            itemView.setOnClickListener(clickListener);

            // Enable/disable based on unlock status
            levelButton.setEnabled(isUnlocked);
            itemView.setEnabled(isUnlocked);
            itemView.setAlpha(isUnlocked ? 1.0f : 0.5f); // Visual feedback for locked levels
        }
    }
}
