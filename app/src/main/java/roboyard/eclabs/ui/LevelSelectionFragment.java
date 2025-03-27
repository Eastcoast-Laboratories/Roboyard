package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_level_selection, container, false);
        
        // Set up UI elements
        titleTextView = view.findViewById(R.id.level_selection_title);
        levelRecyclerView = view.findViewById(R.id.level_recycler_view);
        
        // Set up RecyclerView with grid layout (3 columns)
        levelRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        
        // Load available levels
        loadAvailableLevels();
        
        // Set up adapter
        levelAdapter = new LevelAdapter(availableLevels, this::onLevelSelected);
        levelRecyclerView.setAdapter(levelAdapter);
        
        return view;
    }
    
    /**
     * Load available levels from assets
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
     * Handle level selection
     */
    private void onLevelSelected(int levelId) {
        Timber.d("Selected level: %d", levelId);
        
        // Start a new game with the selected level
        gameStateManager.startLevelGame(levelId);
        
        // Create a new ModernGameFragment instance
        ModernGameFragment gameFragment = new ModernGameFragment();
        navigateToDirect(gameFragment);
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.level_selection_title);
    }
    
    /**
     * Adapter for level selection grid
     */
    private static class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {
        
        private final List<Integer> levels;
        private final OnLevelSelectedListener listener;
        
        public interface OnLevelSelectedListener {
            void onLevelSelected(int levelId);
        }
        
        public LevelAdapter(List<Integer> levels, OnLevelSelectedListener listener) {
            this.levels = levels;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level, parent, false);
            return new LevelViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
            int levelId = levels.get(position);
            holder.bind(levelId, listener);
        }
        
        @Override
        public int getItemCount() {
            return levels.size();
        }
        
        static class LevelViewHolder extends RecyclerView.ViewHolder {
            private final Button levelButton;
            
            public LevelViewHolder(@NonNull View itemView) {
                super(itemView);
                levelButton = itemView.findViewById(R.id.level_button);
            }
            
            public void bind(int levelId, OnLevelSelectedListener listener) {
                levelButton.setText(String.valueOf(levelId));
                levelButton.setContentDescription("Level " + levelId);
                levelButton.setOnClickListener(v -> listener.onLevelSelected(levelId));
            }
        }
    }
}
