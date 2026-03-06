package roboyard.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboyard.logic.core.Constants;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.eclabs.R;
import roboyard.ui.achievements.AchievementManager;
import roboyard.ui.components.GameHistoryManager;
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
    private TextView progressText;
    private View progressFill;
    private Button userProfileButton;
    private final List<Integer> availableLevels = new ArrayList<>();
    private LevelCompletionManager completionManager;
    private int totalStars = 0;
    private int completedLevelCount = 0;
    /** Maps level file map name (e.g. "level_1") to history entry, for minimap + info-box reuse */
    private final Map<String, GameHistoryEntry> historyByMapName = new HashMap<>();

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
        progressText = view.findViewById(R.id.progress_text);
        progressFill = view.findViewById(R.id.progress_fill);
        levelRecyclerView = view.findViewById(R.id.level_recycler_view);

        // Set title
        titleTextView.setText(getString(R.string.level_selection_title));

        // Set up RecyclerView with grid layout (3 columns in portrait, 6 in landscape)
        int spanCount = getResources().getConfiguration().orientation == 
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 6 : 3;
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        // Make headers span full width
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (levelAdapter != null && levelAdapter.getItemViewType(position) == LevelAdapter.VIEW_TYPE_HEADER) {
                    return spanCount;
                }
                return 1;
            }
        });
        levelRecyclerView.setLayoutManager(layoutManager);

        // Get the level completion manager
        completionManager = LevelCompletionManager.getInstance(requireContext());

        // Get total stars
        totalStars = completionManager.getTotalStars();

        // Load available levels
        loadAvailableLevels();

        // Load history entries mapped by level map name
        loadHistoryByMapName();

        // Count completed levels and update progress
        updateProgressUI();

        // Set up adapter
        levelAdapter = new LevelAdapter(availableLevels, this, completionManager, totalStars, historyByMapName);
        levelRecyclerView.setAdapter(levelAdapter);

        // Add scroll listener to fade out cards earlier when scrolling up (keeps header visible)
        setupScrollFadeEffect();

        // Auto-scroll to the last played level
        scrollToLastPlayedLevel();

        // Set up back button
        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            // Navigate back to the main menu
            MainMenuFragment menuFragment = new MainMenuFragment();
            navigateToDirect(menuFragment);
        });

        // Set up user profile button
        userProfileButton = view.findViewById(R.id.user_profile_button);
        setupUserProfileButton(userProfileButton);

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
        
        // Update user profile button UI
        if (userProfileButton != null) {
            updateUserProfileButton(userProfileButton);
        }
        
        // Stop map regeneration when in level selection screen
        if (gameStateManager != null) {
            gameStateManager.stopRegeneration();
            Timber.d("[SOLVER] Stopped regeneration in level selection screen");
        }
    }
    
    /**
     * Sets up scroll fade effect: level cards fade out earlier when scrolling up
     * to keep header and progress bar always visible.
     */
    private void setupScrollFadeEffect() {
        // Config: fade starts when item is this many pixels below the progress bar
        final int FADE_START_OFFSET_PX = -110;
        final int FADE_DISTANCE_PX = 150;

        levelRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Get the progress container height to know where cards should start fading
                View progressContainer = getView() != null ? getView().findViewById(R.id.progress_container) : null;
                if (progressContainer == null) return;

                int progressBottom = progressContainer.getBottom();
                int fadeStartY = progressBottom + FADE_START_OFFSET_PX;

                // Iterate through visible children and apply fade based on position
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    if (child == null) continue;

                    int childTop = child.getTop();

                    if (childTop < fadeStartY) {
                        // Card is in fade zone
                        float fadeProgress = Math.max(0f, Math.min(1f, 
                                (fadeStartY - childTop) / (float) FADE_DISTANCE_PX));
                        child.setAlpha(1f - fadeProgress);
                    } else {
                        // Card is fully visible
                        child.setAlpha(1f);
                    }
                }
            }
        });
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
        
        // Update user profile button when returning to this screen
        if (userProfileButton != null) {
            updateUserProfileButton(userProfileButton);
        }
        
        // Update total stars
        totalStars = completionManager.getTotalStars();

        // Update progress UI
        updateProgressUI();

        // Reload history entries (may have new entries after playing)
        loadHistoryByMapName();

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
     * Loads history entries and maps them by normalized level key (e.g. "level_1" for levelId=1).
     * History stores mapName as "Level 1" (set in GameStateManager.startLevelGame), so we
     * extract the number and map it to the key used by onBindViewHolder.
     */
    private void loadHistoryByMapName() {
        historyByMapName.clear();
        try {
            List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(requireActivity());
            if (entries != null) {
                for (GameHistoryEntry entry : entries) {
                    String key = extractLevelKey(entry);
                    if (key == null) continue;
                    // Keep the entry with the most completions if there are duplicates
                    GameHistoryEntry existing = historyByMapName.get(key);
                    if (existing == null || entry.getCompletionCount() >= existing.getCompletionCount()) {
                        historyByMapName.put(key, entry);
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "[LEVEL_SELECTION] Error loading history entries for minimap display");
        }
        Timber.d("[LEVEL_SELECTION] Loaded %d history entries into map", historyByMapName.size());
    }

    /**
     * Extracts a normalized level key (e.g. "level_1") from a history entry.
     * History mapName is "Level 1" (set via GameStateManager.startLevelGame).
     * Also falls back to parsing the mapPath basename (e.g. "level_1.txt").
     *
     * @return normalized key like "level_1" or "custom_level_141", or null if not a level entry
     */
    private static String extractLevelKey(GameHistoryEntry entry) {
        // Try mapName "Level N" -> "level_N"
        String mapName = entry.getMapName();
        if (mapName != null) {
            if (mapName.matches("(?i)Level \\d+")) {
                int id = Integer.parseInt(mapName.trim().split("\\s+")[1]);
                return id >= 141 ? "custom_level_" + id : "level_" + id;
            }
        }
        // Fallback: parse mapPath basename, e.g. "level_1.txt" or "level_1"
        String mapPath = entry.getMapPath();
        if (mapPath != null) {
            String base = mapPath.contains("/")
                    ? mapPath.substring(mapPath.lastIndexOf('/') + 1)
                    : mapPath;
            if (base.startsWith("level_") || base.startsWith("custom_level_")) {
                return base.endsWith(".txt") ? base.substring(0, base.length() - 4) : base;
            }
        }
        return null;
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
        levelAdapter = new LevelAdapter(availableLevels, this, completionManager, totalStars, historyByMapName);
        levelRecyclerView.setAdapter(levelAdapter);
    }

    /**
     * Calculate the total number of stars earned across all levels
     */
    private void calculateTotalStars() {
        totalStars = 0;
        completedLevelCount = 0;
        for (Integer levelId : availableLevels) {
            LevelCompletionData data = completionManager.getLevelCompletionData(levelId);
            if (data != null) {
                totalStars += data.getStars();
                if (data.getStars() > 0) {
                    completedLevelCount++;
                }
            }
        }
    }

    /**
     * Updates the progress bar and stars count in the header.
     * Shows "X / Y Level completed" in the progress bar and "X / Y" as star count.
     */
    private void updateProgressUI() {
        calculateTotalStars();
        int totalLevels = availableLevels.size();

        // Update stars count text (right side, large golden text)
        if (starsTextView != null) {
            starsTextView.setText(String.format("%d / %d", completedLevelCount, totalLevels));
        }

        // Update progress text inside progress bar
        if (progressText != null) {
            progressText.setText(String.format("%d / %d %s",
                    completedLevelCount, totalLevels,
                    getString(R.string.level_progress_completed)));
        }

        // Update progress bar fill width
        if (progressFill != null && totalLevels > 0) {
            progressFill.post(() -> {
                View parent = (View) progressFill.getParent();
                int parentWidth = parent.getWidth();
                float fraction = (float) completedLevelCount / totalLevels;
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = (int) (parentWidth * fraction);
                progressFill.setLayoutParams(params);
            });
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
    public void onLevelSelected(int levelId, View clickedCard) {
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

        // Prevent double-clicks during animation
        clickedCard.setClickable(false);

        // Animate the card zooming to fill the upper half of the screen
        animateLevelZoom(clickedCard, levelId);
    }

    /**
     * Animates the clicked level card zooming to match the game board position/size,
     * then navigates to the GameFragment.
     * Creates a bitmap snapshot of the card, places it as an overlay above everything,
     * hides the original card, and animates the overlay.
     */
    private void animateLevelZoom(View card, int levelId) {
        // --- Config ---
        final int ZOOM_DURATION_MS = 999;

        View rootView = getView();
        if (!(rootView instanceof FrameLayout)) return;
        FrameLayout rootFrame = (FrameLayout) rootView;

        // Create a bitmap snapshot of the card
        card.setDrawingCacheEnabled(true);
        card.buildDrawingCache();
        Bitmap snapshot = Bitmap.createBitmap(card.getDrawingCache());
        card.setDrawingCacheEnabled(false);

        // Get the card's position relative to the root FrameLayout
        int[] cardLocation = new int[2];
        int[] rootLocation = new int[2];
        card.getLocationOnScreen(cardLocation);
        rootFrame.getLocationOnScreen(rootLocation);

        int startX = cardLocation[0] - rootLocation[0];
        int startY = cardLocation[1] - rootLocation[1];

        // Create an ImageView overlay with the snapshot, placed exactly over the original card
        ImageView overlay = new ImageView(requireContext());
        overlay.setImageBitmap(snapshot);
        overlay.setScaleType(ImageView.ScaleType.FIT_XY);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(card.getWidth(), card.getHeight());
        params.leftMargin = startX;
        params.topMargin = startY;
        overlay.setLayoutParams(params);
        overlay.setElevation(100f);

        // Add overlay on top of everything and hide the original card
        rootFrame.addView(overlay);
        card.setVisibility(View.INVISIBLE);

        // Target: match the game board position in GameFragment (full width, top-aligned, square)
        // The game board in portrait mode is full-width starting at Y=0, height = width (square boards)
        int screenWidth = rootFrame.getWidth();
        float targetWidth = screenWidth;
        float targetHeight = screenWidth; // Square board assumption (most levels are ~square)
        float targetX = targetWidth / 2f;  // Center X = half screen width
        float targetY = targetHeight / 2f; // Center Y = half of board height (top-aligned)

        float overlayCenterX = startX + card.getWidth() / 2f;
        float overlayCenterY = startY + card.getHeight() / 2f;

        // Scale to match game board size
        float scaleX = targetWidth / card.getWidth();
        float scaleY = targetHeight / card.getHeight();

        // Translation to move overlay center to board center
        float translateX = targetX - overlayCenterX;
        float translateY = targetY - overlayCenterY;

        // Check if this is the last played level (has yellow border)
        int lastPlayedLevel = LevelCompletionManager.getInstance(requireContext()).getLastPlayedLevel();
        boolean hasYellowBorder = (levelId == lastPlayedLevel);

        // Create a separate border overlay if this level has the yellow border
        ImageView borderOverlay = null;
        if (hasYellowBorder) {
            borderOverlay = new ImageView(requireContext());
            borderOverlay.setImageDrawable(requireContext().getDrawable(R.drawable.bg_level_card_last_played));
            borderOverlay.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(card.getWidth(), card.getHeight());
            borderParams.leftMargin = startX;
            borderParams.topMargin = startY;
            borderOverlay.setLayoutParams(borderParams);
            borderOverlay.setElevation(101f); // Above the main overlay
            rootFrame.addView(borderOverlay);
        }

        // Animate the overlay
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(overlay, "scaleX", 1f, scaleX);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(overlay, "scaleY", 1f, scaleY);
        ObjectAnimator transXAnim = ObjectAnimator.ofFloat(overlay, "translationX", 0f, translateX);
        ObjectAnimator transYAnim = ObjectAnimator.ofFloat(overlay, "translationY", 0f, translateY);

        animatorSet.playTogether(scaleXAnim, scaleYAnim, transXAnim, transYAnim);

        // If yellow border exists, animate it too (same scale/translation) + fade out
        if (hasYellowBorder && borderOverlay != null) {
            ImageView finalBorderOverlay = borderOverlay;
            ObjectAnimator borderScaleX = ObjectAnimator.ofFloat(finalBorderOverlay, "scaleX", 1f, scaleX);
            ObjectAnimator borderScaleY = ObjectAnimator.ofFloat(finalBorderOverlay, "scaleY", 1f, scaleY);
            ObjectAnimator borderTransX = ObjectAnimator.ofFloat(finalBorderOverlay, "translationX", 0f, translateX);
            ObjectAnimator borderTransY = ObjectAnimator.ofFloat(finalBorderOverlay, "translationY", 0f, translateY);
            ObjectAnimator borderFadeOut = ObjectAnimator.ofFloat(finalBorderOverlay, "alpha", 1f, 0f);

            animatorSet.playTogether(scaleXAnim, scaleYAnim, transXAnim, transYAnim,
                    borderScaleX, borderScaleY, borderTransX, borderTransY, borderFadeOut);
        }

        animatorSet.setDuration(ZOOM_DURATION_MS);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

        ImageView finalBorderOverlayForCleanup = borderOverlay;
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset achievement game session flags for new game
                AchievementManager.getInstance(requireContext()).onNewGameStarted();

                // Start a new game with the selected level
                gameStateManager.startLevelGame(levelId);

                // Navigate to GameFragment BEFORE removing overlay to avoid flash-back glitch
                GameFragment gameFragment = new GameFragment();
                navigateToDirect(gameFragment);

                // Clean up overlay after navigation (post to ensure fragment transaction started)
                rootFrame.post(() -> {
                    rootFrame.removeView(overlay);
                    if (finalBorderOverlayForCleanup != null) {
                        rootFrame.removeView(finalBorderOverlayForCleanup);
                    }
                    card.setVisibility(View.VISIBLE);
                    card.setClickable(true);
                });
            }
        });

        animatorSet.start();
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
        private final Map<String, GameHistoryEntry> historyByMapName;
        private int totalStars;

        public LevelAdapter(List<Integer> levels, LevelSelectionFragment fragment,
                          LevelCompletionManager completionManager, int totalStars,
                          Map<String, GameHistoryEntry> historyByMapName) {
            this.levels = levels;
            this.fragment = fragment;
            this.completionManager = completionManager;
            this.totalStars = totalStars;
            this.historyByMapName = historyByMapName;
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

                    // Look up history entry for this level (e.g. "level_1" for levelId=1)
                    String mapKey = levelId < CUSTOM_LEVEL_START_ID
                            ? "level_" + levelId
                            : "custom_level_" + levelId;
                    GameHistoryEntry historyEntry = historyByMapName.get(mapKey);

                    // Bind the level data
                    levelHolder.bind(levelId, fragment, isCompleted, starsEarned, isUnlocked, historyEntry);
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
            // Hide "Standard Levels" header, show "Custom Levels" header
            if ("Standard Levels".equals(headerText)) {
                itemView.setVisibility(View.GONE);
            } else {
                itemView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * ViewHolder for level items in the RecyclerView.
     * Displays level cards in 3 states: gold (completed), blue (playable), gray (locked).
     */
    private static class LevelViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout levelCard;
        private final TextView levelNumberText;
        private final TextView levelNameText;
        private final ImageView starOne;
        private final ImageView starTwo;
        private final ImageView starThree;
        private final ImageView starFour;
        private final ImageView minimapView;
        private final ImageView lockIcon;
        private final ImageView playArrow;
        private final ImageButton infoButton;

        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            levelCard = itemView.findViewById(R.id.level_card);
            levelNumberText = itemView.findViewById(R.id.level_number_text);
            levelNameText = itemView.findViewById(R.id.level_name_text);
            starOne = itemView.findViewById(R.id.level_star_1);
            starTwo = itemView.findViewById(R.id.level_star_2);
            starThree = itemView.findViewById(R.id.level_star_3);
            starFour = itemView.findViewById(R.id.level_star_4);
            minimapView = itemView.findViewById(R.id.level_minimap_view);
            lockIcon = itemView.findViewById(R.id.lock_icon);
            playArrow = itemView.findViewById(R.id.play_arrow);
            infoButton = itemView.findViewById(R.id.level_info_button);
        }

        /**
         * Binds data to this ViewHolder.
         * Three visual states:
         * - GOLD card: completed level with stars, minimap preview, and "Level X" label
         * - BLUE card: playable level with large number and play arrow
         * - GRAY card: locked level with lock icon
         */
        public void bind(int levelId, LevelSelectionFragment fragment, boolean isCompleted,
                        int starsEarned, boolean isUnlocked, GameHistoryEntry historyEntry) {

            levelCard.setContentDescription("Level " + levelId);

            if (isCompleted && starsEarned > 0) {
                // === GOLD CARD: Completed level ===
                levelCard.setBackgroundResource(R.drawable.bg_level_card_gold);

                // Show stars
                starOne.setVisibility(starsEarned >= 1 ? View.VISIBLE : View.GONE);
                starTwo.setVisibility(starsEarned >= 2 ? View.VISIBLE : View.GONE);
                starThree.setVisibility(starsEarned >= 3 ? View.VISIBLE : View.GONE);
                starFour.setVisibility(starsEarned >= 4 ? View.VISIBLE : View.GONE);

                // Show info button if history entry exists
                if (historyEntry != null && infoButton != null) {
                    infoButton.setVisibility(View.VISIBLE);
                    infoButton.setOnClickListener(v -> fragment.showMapInfoPopup(historyEntry));
                } else {
                    if (infoButton != null) infoButton.setVisibility(View.GONE);
                }

                // Show minimap if history entry exists
                if (historyEntry != null && minimapView != null) {
                    String mapPath = historyEntry.getMapPath();
                    String absolutePath = (mapPath != null && !mapPath.startsWith("/"))
                            ? itemView.getContext().getFileStreamPath(mapPath).getAbsolutePath()
                            : mapPath;
                    if (absolutePath != null) {
                        Bitmap minimap = fragment.createMinimapFromPath(
                                itemView.getContext(), absolutePath, 120, 120);
                        minimapView.setImageBitmap(minimap);
                        minimapView.setVisibility(View.VISIBLE);
                        levelNumberText.setVisibility(View.GONE);
                    } else {
                        minimapView.setVisibility(View.GONE);
                        levelNumberText.setText(String.valueOf(levelId));
                        levelNumberText.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (minimapView != null) minimapView.setVisibility(View.GONE);
                    levelNumberText.setText(String.valueOf(levelId));
                    levelNumberText.setVisibility(View.VISIBLE);
                }

                // Show level name at bottom
                levelNameText.setText("Level " + levelId);
                levelNameText.setVisibility(View.VISIBLE);

                // Hide lock & play arrow
                lockIcon.setVisibility(View.GONE);
                playArrow.setVisibility(View.GONE);

                levelCard.setAlpha(1.0f);

            } else if (isUnlocked) {
                // === BLUE CARD: Playable but not yet completed ===
                levelCard.setBackgroundResource(R.drawable.bg_level_card_blue);

                // Hide stars, minimap, level name, info button
                starOne.setVisibility(View.GONE);
                starTwo.setVisibility(View.GONE);
                starThree.setVisibility(View.GONE);
                starFour.setVisibility(View.GONE);
                if (minimapView != null) minimapView.setVisibility(View.GONE);
                if (infoButton != null) infoButton.setVisibility(View.GONE);
                levelNameText.setVisibility(View.GONE);
                lockIcon.setVisibility(View.GONE);

                // Show large level number
                levelNumberText.setText(String.valueOf(levelId));
                levelNumberText.setVisibility(View.VISIBLE);

                // Show play arrow
                playArrow.setVisibility(View.VISIBLE);

                levelCard.setAlpha(1.0f);

            } else {
                // === GRAY CARD: Locked level ===
                levelCard.setBackgroundResource(R.drawable.bg_level_card_locked);

                // Hide stars, minimap, level name, play arrow, info button
                starOne.setVisibility(View.GONE);
                starTwo.setVisibility(View.GONE);
                starThree.setVisibility(View.GONE);
                starFour.setVisibility(View.GONE);
                if (minimapView != null) minimapView.setVisibility(View.GONE);
                if (infoButton != null) infoButton.setVisibility(View.GONE);
                levelNameText.setVisibility(View.GONE);
                playArrow.setVisibility(View.GONE);

                // Show level number (dimmed) and lock icon
                levelNumberText.setText(String.valueOf(levelId));
                levelNumberText.setVisibility(View.VISIBLE);
                levelNumberText.setAlpha(0.3f);
                lockIcon.setVisibility(View.VISIBLE);

                levelCard.setAlpha(0.8f);
            }

            // Highlight the last played level with yellow border
            int lastPlayedLevel = LevelCompletionManager.getInstance(
                    itemView.getContext()).getLastPlayedLevel();
            if (levelId == lastPlayedLevel) {
                // Apply yellow border as foreground for last played level
                levelCard.setForeground(itemView.getContext().getDrawable(R.drawable.bg_level_card_last_played));
                levelCard.setElevation(8f);
            } else {
                levelCard.setForeground(null);
                levelCard.setElevation(2f);
            }

            // Reset alpha on levelNumberText for non-locked
            if (isUnlocked) {
                levelNumberText.setAlpha(1.0f);
            }

            // Set click listener on the card
            View.OnClickListener clickListener = v -> fragment.onLevelSelected(levelId, levelCard);
            levelCard.setOnClickListener(clickListener);

            // Disable click for locked levels
            levelCard.setClickable(isUnlocked);
            levelCard.setFocusable(isUnlocked);
        }
    }
}
