package roboyard.ui.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import roboyard.eclabs.R;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameState;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.MinimapGenerator;
import roboyard.ui.util.FontScaleUtil;
import roboyard.logic.core.Preferences;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * Base fragment class for all game screens.
 * Provides common functionality and access to the GameStateManager.
 */
public abstract class BaseGameFragment extends Fragment {
    
    protected GameStateManager gameStateManager;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply language settings before creating the fragment
        applyLanguageSettings();
        
        super.onCreate(savedInstanceState);
        // Get the GameStateManager from the activity
        gameStateManager = new ViewModelProvider(requireActivity()).get(GameStateManager.class);
    }
    
    /**
     * Override onAttach to apply fixed font scaling
     * This ensures consistent text sizes regardless of system settings
     */
    @Override
    public void onAttach(Context context) {
        // Apply fixed font scaling to ensure consistent UI
        Context fixedContext = FontScaleUtil.createFixedFontScaleContext(context);
        super.onAttach(fixedContext);
    }
    
    /**
     * Navigate to another screen using direct fragment transaction
     * This method should be used instead of Navigation component when mixing navigation approaches
     * @param fragment The fragment to navigate to
     * @param addToBackStack Whether to add the transaction to the back stack
     * @param tag Optional tag for the fragment transaction
     */
    protected void navigateToDirect(Fragment fragment, boolean addToBackStack, String tag) {
        try {
            // Create the transaction
            androidx.fragment.app.FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment);
            
            // Add to back stack if requested
            if (addToBackStack) {
                transaction.addToBackStack(tag);
            }
            
            // Commit the transaction
            transaction.commit();
            
            // Log success
            Timber.d("Navigation to %s completed using fragment transaction", 
                    fragment.getClass().getSimpleName());
        } catch (Exception e) {
            // Log error
            Timber.e(e, "Error navigating to %s", fragment.getClass().getSimpleName());
        }
    }
    
    /**
     * Simplified version of navigateToDirect that adds to back stack with null tag
     * @param fragment The fragment to navigate to
     */
    protected void navigateToDirect(Fragment fragment) {
        navigateToDirect(fragment, true, null);
    }
    
    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    public abstract String getScreenTitle();

    /**
     * Creates a minimap bitmap from a map file path.
     * Shared between SaveGameFragment and LevelSelectionFragment (DRY).
     */
    protected Bitmap createMinimapFromPath(Context context, String mapPath, int width, int height) {
        Timber.d("[MINIMAP] Loading minimap from path: %s", mapPath);
        
        // If path is just a filename (e.g. "history_0.txt"), construct full path
        String fullPath = mapPath;
        if (mapPath != null && !mapPath.startsWith("/")) {
            // Relative path - construct full path in files directory
            java.io.File file = new java.io.File(context.getFilesDir(), mapPath);
            fullPath = file.getAbsolutePath();
            Timber.d("[MINIMAP] Converted relative path '%s' to absolute: %s", mapPath, fullPath);
        }
        
        String saveData = FileReadWrite.loadAbsoluteData(fullPath);
        if (saveData == null || saveData.isEmpty()) {
            Timber.e("[MINIMAP] Failed to load data from path: %s (data is null or empty)", mapPath);
        } else {
            Timber.d("[MINIMAP] Loaded %d bytes from %s", saveData.length(), mapPath);
            try {
                GameState gameState = GameState.parseFromSaveData(saveData, context);
                if (gameState == null) {
                    Timber.e("[MINIMAP] GameState.parseFromSaveData returned null for path: %s", mapPath);
                } else {
                    Timber.d("[MINIMAP] Successfully parsed GameState, generating minimap");
                    Bitmap minimap = MinimapGenerator.getInstance().generateMinimap(context, gameState, width, height);
                    if (minimap != null) {
                        Timber.d("[MINIMAP] Successfully generated minimap %dx%d", minimap.getWidth(), minimap.getHeight());
                    } else {
                        Timber.e("[MINIMAP] MinimapGenerator returned null");
                    }
                    return minimap;
                }
            } catch (Exception e) {
                Timber.e(e, "[MINIMAP] Error creating minimap from save data for path: %s", mapPath);
            }
        }
        
        Timber.w("[MINIMAP] Returning dummy placeholder for path: %s", mapPath);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(200, 230, 255));
        Paint paint = new Paint();
        paint.setColor(Color.rgb(150, 200, 240));
        paint.setStrokeWidth(2);
        for (int i = 0; i < width; i += width / 10) canvas.drawLine(i, 0, i, height, paint);
        for (int i = 0; i < height; i += height / 10) canvas.drawLine(0, i, width, i, paint);
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawCircle(width / 3f, height / 3f, width / 15f, paint);
        paint.setColor(Color.rgb(100, 255, 100));
        canvas.drawRect(width / 2f, height / 2f, width / 2f + width / 10f, height / 2f + height / 10f, paint);
        return bitmap;
    }

    /**
     * Shows a popup dialog with detailed map/history info for a GameHistoryEntry.
     * Shared between SaveGameFragment and LevelSelectionFragment (DRY).
     */
    protected void showMapInfoPopup(GameHistoryEntry entry) {
        if (entry == null || getContext() == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.history_detail_completions)).append(" ").append(entry.getCompletionCount()).append("\n");
        sb.append(getString(R.string.history_detail_first_started)).append(" ").append(sdf.format(new Date(entry.getTimestamp()))).append("\n");
        if (entry.getLastCompletionTimestamp() > 0) {
            sb.append(getString(R.string.history_detail_last_played)).append(" ").append(sdf.format(new Date(entry.getLastCompletionTimestamp()))).append("\n");
        }
        List<Long> timestamps = entry.getCompletionTimestamps();
        if (timestamps != null && timestamps.size() > 1) {
            sb.append("\n").append(getString(R.string.history_detail_all_completions)).append("\n");
            for (int i = 0; i < timestamps.size(); i++) {
                sb.append("  ").append(i + 1).append(". ")
                  .append(sdf.format(new Date(timestamps.get(i)))).append("\n");
            }
        }
        sb.append("\n").append(getString(R.string.history_detail_best_time)).append(" ");
        int bestTime = entry.getBestTime();
        if (bestTime > 0) sb.append(bestTime / 60).append("m ").append(bestTime % 60).append("s");
        else sb.append("\u2014");
        sb.append("\n");
        sb.append(getString(R.string.history_detail_best_moves)).append(" ");
        int bestMoves = entry.getBestMoves();
        sb.append(bestMoves > 0 ? bestMoves : "\u2014").append("\n");
        sb.append(getString(R.string.history_detail_optimal_moves)).append(" ");
        int optimalMoves = entry.getOptimalMoves();
        if (optimalMoves > 0) {
            sb.append(optimalMoves);
            if (bestMoves > 0 && bestMoves == optimalMoves) sb.append(" \u2713 (").append(getString(R.string.history_detail_perfect)).append(")");
            else if (bestMoves > 0) sb.append(" (").append(getString(R.string.history_detail_extra_moves, bestMoves - optimalMoves)).append(")");
        } else sb.append("\u2014");
        sb.append("\n");
        sb.append("\n").append(getString(R.string.history_detail_hint_usage_last)).append(" ");
        int maxHint = entry.getMaxHintUsed();
        if (maxHint < 0) sb.append(getString(R.string.history_detail_no_hints_used));
        else if (maxHint == 0) sb.append(getString(R.string.history_detail_pre_hint_viewed));
        else sb.append(getString(R.string.history_detail_up_to_hint, maxHint + 1));
        sb.append("\n");
        sb.append(getString(R.string.history_detail_hints_ever_used)).append(" ")
          .append(entry.isEverUsedHints() ? getString(R.string.history_detail_yes) : getString(R.string.history_detail_no)).append("\n");
        sb.append(getString(R.string.history_detail_qualifies_no_hints)).append(" ")
          .append(entry.qualifiesForNoHintsAchievement() ? getString(R.string.history_detail_yes) : getString(R.string.history_detail_no)).append("\n");
        sb.append(getString(R.string.history_detail_qualifies_no_hints_perfect)).append(" ")
          .append(entry.qualifiesForPerfectNoHintsAchievement() ? getString(R.string.history_detail_yes) : getString(R.string.history_detail_no)).append("\n");
        long lastNoHints = entry.getLastSolvedWithoutHints();
        sb.append(getString(R.string.history_detail_last_solved_no_hints)).append(" ")
          .append(lastNoHints > 0 ? sdf.format(new Date(lastNoHints)) : "\u2014").append("\n");
        long lastPerfect = entry.getLastPerfectlySolvedWithoutHints();
        sb.append(getString(R.string.history_detail_last_perfect_no_hints)).append(" ")
          .append(lastPerfect > 0 ? sdf.format(new Date(lastPerfect)) : "\u2014").append("\n");
        if (entry.getBoardSize() != null && !entry.getBoardSize().isEmpty()) {
            sb.append("\n").append(getString(R.string.history_detail_board)).append(" ").append(entry.getBoardSize()).append("\n");
        }
        sb.append(getString(R.string.history_detail_map)).append(" ").append(entry.getMapName()).append("\n");
        
        // Create dialog with smaller text size
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(entry.getMapName())
            .setMessage(sb.toString())
            .setPositiveButton(android.R.string.ok, null)
            .create();
        dialog.show();
        
        // Set smaller text size for message
        android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        }
    }
    
    /**
     * Setup user profile button with click listener
     * Handles both initialization and click listener setup in one call
     * Shows user initials in circle when logged in, or user icon when logged out
     * This is a reusable method for any fragment that needs to display the login circle
     */
    protected void setupUserProfileButton(android.widget.Button userProfileButton) {
        if (userProfileButton == null) {
            Timber.w("setupUserProfileButton: userProfileButton is null");
            return;
        }
        
        // Update the button UI based on login state
        updateUserProfileButton(userProfileButton);
        
        // Set up click listener
        userProfileButton.setOnClickListener(v -> {
            roboyard.ui.components.RoboyardApiClient apiClient = roboyard.ui.components.RoboyardApiClient.getInstance(requireContext());
            if (apiClient.isLoggedIn()) {
                // Open profile in browser with auto-login token
                String url = apiClient.buildAutoLoginUrl("https://roboyard.z11.de/profile");
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                startActivity(intent);
            } else {
                // Show login dialog
                roboyard.ui.components.LoginDialogHelper.showLoginDialog(requireContext(), new roboyard.ui.components.LoginDialogHelper.LoginCallback() {
                    @Override
                    public void onLoginSuccess(roboyard.ui.components.RoboyardApiClient.LoginResult result) {
                        updateUserProfileButton(userProfileButton);
                    }
                    
                    @Override
                    public void onLoginError(String error) {
                        // Error handling is done in LoginDialogHelper
                    }
                });
            }
        });
    }
    
    /**
     * Update user profile button based on login state
     * Shows user initials in circle when logged in, or user icon when logged out
     * This is a reusable method for any fragment that needs to display the login circle
     */
    protected void updateUserProfileButton(android.widget.Button userProfileButton) {
        if (userProfileButton == null) {
            Timber.w("updateUserProfileButton: userProfileButton is null");
            return;
        }
        
        roboyard.ui.components.RoboyardApiClient apiClient = roboyard.ui.components.RoboyardApiClient.getInstance(requireContext());
        if (apiClient.isLoggedIn()) {
            String userName = apiClient.getUserName();
            if (userName == null) userName = apiClient.getUserEmail();
            if (userName != null && !userName.isEmpty()) {
                String initials = String.valueOf(userName.charAt(0)).toUpperCase();
                userProfileButton.setText(initials);
                userProfileButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                userProfileButton.setGravity(android.view.Gravity.CENTER);
                userProfileButton.setContentDescription(initials);
            }
        } else {
            userProfileButton.setText("");
            userProfileButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_user_profile, 0, 0);
            userProfileButton.setGravity(android.view.Gravity.CENTER);
            userProfileButton.setPadding(8, 14, 8, 8);
        }
    }
    
    /**
     * Applies the language settings to the application context
     */
    private void applyLanguageSettings() {
        try {
            // Get saved language setting
            String languageCode = Preferences.appLanguage;
            Timber.d("ROBOYARD_LANGUAGE: Loading saved language in fragment: %s", languageCode);
            
            if (languageCode != null && !languageCode.isEmpty()) {
                // Apply language change
                Locale locale = new Locale(languageCode);
                Locale.setDefault(locale);
                
                Resources resources = requireContext().getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale);
                
                resources.updateConfiguration(config, resources.getDisplayMetrics());
                Timber.d("ROBOYARD_LANGUAGE: Successfully applied language %s in fragment", languageCode);
            }
        } catch (Exception e) {
            Timber.e(e, "ROBOYARD_LANGUAGE: Error loading language settings in fragment");
        }
    }
}
