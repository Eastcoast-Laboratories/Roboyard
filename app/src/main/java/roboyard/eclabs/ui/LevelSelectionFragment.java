package roboyard.eclabs.ui;

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
import timber.log.Timber;

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

        // Set up RecyclerView with grid layout (3 columns)
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        levelRecyclerView.setLayoutManager(layoutManager);

        // Add a custom ItemDecoration to reduce spacing between grid items
        levelRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, 
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                // Set very minimal margins (1dp) to maintain minimal separation
                int spacing = 1; // 1dp
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
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
            starsTextView.setText(totalStars + "/420");
        }

        // Set up adapter
        levelAdapter = new LevelAdapter(availableLevels, this, completionManager, totalStars);
        levelRecyclerView.setAdapter(levelAdapter);

        // Auto-scroll to the latest unlocked level
        scrollToLatestUnlockedLevel();

        // Add secret button for Level Editor (long press on title)
        titleTextView.setOnLongClickListener(v -> {
            Timber.d("Title long pressed, opening Level Design Editor");
            openLevelDesignEditor(0); // Open with new level
            return true;
        });
        
        // Set up back button
        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            // Navigate back to the main menu
            MainMenuFragment menuFragment = new MainMenuFragment();
            navigateToDirect(menuFragment);
        });

        return view;
    }

    /**
     * Scroll to the latest unlocked level automatically
     */
    private void scrollToLatestUnlockedLevel() {
        for (int i = 0; i < availableLevels.size(); i++) {
            int levelId = availableLevels.get(i);
            if (levelId > totalStars + 1) {
                // This is the first locked level, scroll to the previous one (the last unlocked)
                if (i > 0) {
                    final int position = Math.max(0, i - 1);
                    levelRecyclerView.post(() -> levelRecyclerView.smoothScrollToPosition(position));
                }
                return;
            }
        }

        // If we reached here, all levels are unlocked, scroll to the last one
        if (!availableLevels.isEmpty()) {
            final int lastPosition = availableLevels.size() - 1;
            levelRecyclerView.post(() -> levelRecyclerView.smoothScrollToPosition(lastPosition));
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
            starsTextView.setText(totalStars + "/420");
        }

        // Refresh the adapter to update completion stars when returning to this screen
        if (levelAdapter != null) {
            levelAdapter.updateTotalStars(totalStars);
            levelAdapter.notifyDataSetChanged();
        }

        // Auto-scroll to the latest unlocked level
        scrollToLatestUnlockedLevel();
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
     * 2. Navigates to the ModernGameFragment to display the game
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
            Toast.makeText(requireContext(), 
                    getString(R.string.level_locked, starsNeeded),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Start a new game with the selected level
        gameStateManager.startLevelGame(levelId);

        // For modern UI, use ModernGameFragment
        ModernGameFragment gameFragment = new ModernGameFragment();
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
     * Open the Level Design Editor fragment
     * @param levelId Level ID to edit, or 0 for a new level
     */
    private void openLevelDesignEditor(int levelId) {
        // Create a new instance of LevelDesignEditorFragment
        LevelDesignEditorFragment editorFragment = LevelDesignEditorFragment.newInstance(levelId);

        // Navigate to the editor fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, editorFragment)
                .addToBackStack(null)
                .commit();
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
                String movesRobots = String.format("%d/%d robots:%d", 
                        completionData.getOptimalMoves(),
                        completionData.getMovesNeeded(),
                        completionData.getRobotsUsed());
                movesText.setText(movesRobots);
                movesText.setVisibility(View.VISIBLE); // Ensure visibility

                // Format time/squares: "0:12 squares:10"
                long timeMs = completionData.getTimeNeeded();
                long seconds = timeMs / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                String timeSquares = String.format("%d:%02d squares:%d", 
                        minutes, seconds, 
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

            // Set click listener for level button
            levelButton.setOnClickListener(v -> fragment.onLevelSelected(levelId));

            // Enable/disable button based on unlock status
            levelButton.setEnabled(isUnlocked);
            levelButton.setAlpha(isUnlocked ? 1.0f : 0.5f); // Visual feedback for locked levels
        }
    }
}
