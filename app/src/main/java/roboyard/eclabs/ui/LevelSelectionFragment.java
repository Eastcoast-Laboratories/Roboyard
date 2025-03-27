package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.Constants;
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
    private List<Integer> availableLevels = new ArrayList<>();
    private LevelCompletionManager completionManager;
    
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
        levelRecyclerView = view.findViewById(R.id.level_recycler_view);
        
        // Set up RecyclerView with grid layout (3 columns)
        levelRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        
        // Get the level completion manager
        completionManager = LevelCompletionManager.getInstance(requireContext());
        
        // Load available levels
        loadAvailableLevels();
        
        // Set up adapter
        levelAdapter = new LevelAdapter(availableLevels, this::onLevelSelected, completionManager);
        levelRecyclerView.setAdapter(levelAdapter);
        
        return view;
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
        // Refresh the adapter to update completion stars when returning to this screen
        if (levelAdapter != null) {
            levelAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Loads available levels from the assets/Maps directory.
     * Looks for files with the pattern "level_X.txt" where X is the level number.
     * The loaded level IDs are stored in the availableLevels list and sorted numerically.
     * 
     * This method is called during fragment initialization to populate the level selection grid.
     */
    private void loadAvailableLevels() {
        availableLevels.clear();
        
        try {
            // List all files in the Maps directory
            String[] files = requireContext().getAssets().list("Maps");
            
            if (files != null) {
                for (String file : files) {
                    // Check if the file is a level file (format: level_X.txt)
                    if (file.startsWith("level_") && file.endsWith(".txt")) {
                        // Extract level number
                        String levelNumStr = file.substring(6, file.length() - 4);
                        try {
                            int levelNum = Integer.parseInt(levelNumStr);
                            availableLevels.add(levelNum);
                        } catch (NumberFormatException e) {
                            Timber.e(e, "Invalid level number format: %s", levelNumStr);
                        }
                    }
                }
            }
            
            // Sort levels by number
            java.util.Collections.sort(availableLevels);
            
        } catch (IOException e) {
            Timber.e(e, "Error loading levels from assets");
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
    private void onLevelSelected(int levelId) {
        Timber.d("Selected level: %d", levelId);
        
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
     * Adapter for the level selection grid.
     * This adapter is responsible for creating and binding ViewHolders that display level buttons
     * and completion stars for levels that have been completed.
     * 
     * The completion status is determined by querying the LevelCompletionManager,
     * which loads data from SharedPreferences using Gson serialization.
     */
    private static class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {
        
        private final List<Integer> levels;
        private final OnLevelSelectedListener listener;
        private final LevelCompletionManager completionManager;
        
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
         * Constructor for the LevelAdapter.
         * 
         * @param levels List of level IDs to display in the grid
         * @param listener Listener to handle level selection events
         * @param completionManager Manager that tracks which levels have been completed
         */
        public LevelAdapter(List<Integer> levels, OnLevelSelectedListener listener, 
                          LevelCompletionManager completionManager) {
            this.levels = levels;
            this.listener = listener;
            this.completionManager = completionManager;
        }
        
        /**
         * Creates a new ViewHolder for a level item.
         * 
         * @param parent The parent ViewGroup
         * @param viewType The view type (not used in this implementation)
         * @return A new LevelViewHolder instance
         */
        @NonNull
        @Override
        public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level, parent, false);
            return new LevelViewHolder(view);
        }
        
        /**
         * Binds data to a ViewHolder at the specified position.
         * This method:
         * 1. Gets the level ID from the levels list
         * 2. Checks if the level is completed using the LevelCompletionManager
         * 3. Calls the ViewHolder's bind method to update the UI accordingly
         * 
         * @param holder The ViewHolder to bind data to
         * @param position The position in the dataset
         */
        @Override
        public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
            int levelId = levels.get(position);
            boolean isCompleted = completionManager.isLevelCompleted(levelId);
            Timber.d("Binding level %d, completed: %s", levelId, isCompleted);
            holder.bind(levelId, listener, isCompleted);
        }
        
        /**
         * Returns the total number of items in the data set.
         * 
         * @return The size of the levels list
         */
        @Override
        public int getItemCount() {
            return levels.size();
        }
        
        /**
         * ViewHolder for level items in the RecyclerView.
         * Contains a button for the level and an ImageView for the completion star.
         */
        static class LevelViewHolder extends RecyclerView.ViewHolder {
            private final Button levelButton;
            private final ImageView completedStar;
            
            /**
             * Constructor for the LevelViewHolder.
             * Finds and stores references to the level button and completion star views.
             * 
             * @param itemView The root view of the level item
             */
            public LevelViewHolder(@NonNull View itemView) {
                super(itemView);
                levelButton = itemView.findViewById(R.id.level_button);
                completedStar = itemView.findViewById(R.id.level_completed_star);
            }
            
            /**
             * Binds data to this ViewHolder.
             * This method:
             * 1. Sets the level number on the button
             * 2. Sets a click listener to handle level selection
             * 3. Shows or hides the completion star based on the level's completion status
             * 
             * If stars are not showing when they should be, check:
             * - That the star drawable resource exists in the drawable folder
             * - That LevelCompletionManager is properly initialized and loading data
             * - That GameStateManager is correctly saving completion data when levels are completed
             * - That the Gson library is included in the project dependencies
             * 
             * @param levelId The ID of the level to display
             * @param listener The listener to handle level selection events
             * @param isCompleted Whether the level has been completed
             */
            public void bind(int levelId, OnLevelSelectedListener listener, boolean isCompleted) {
                levelButton.setText(String.valueOf(levelId));
                levelButton.setContentDescription("Level " + levelId);
                levelButton.setOnClickListener(v -> listener.onLevelSelected(levelId));
                
                // Show star if level is completed
                Timber.d("Setting star visibility for level %d: %s", levelId, 
                        isCompleted ? "VISIBLE" : "GONE");
                completedStar.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
            }
        }
    }
}
