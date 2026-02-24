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
        String saveData = FileReadWrite.loadAbsoluteData(mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            try {
                GameState gameState = GameState.parseFromSaveData(saveData, context);
                if (gameState != null) {
                    return MinimapGenerator.getInstance().generateMinimap(context, gameState, width, height);
                }
            } catch (Exception e) {
                Timber.e(e, "Error creating minimap from save data");
            }
        }
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
        sb.append("Completions: ").append(entry.getCompletionCount()).append("\n");
        sb.append("First played: ").append(sdf.format(new Date(entry.getTimestamp()))).append("\n");
        if (entry.getLastCompletionTimestamp() > 0) {
            sb.append("Last played: ").append(sdf.format(new Date(entry.getLastCompletionTimestamp()))).append("\n");
        }
        List<Long> timestamps = entry.getCompletionTimestamps();
        if (timestamps != null && timestamps.size() > 1) {
            sb.append("\nAll completions:\n");
            for (int i = 0; i < timestamps.size(); i++) {
                sb.append("  ").append(i + 1).append(". ")
                  .append(sdf.format(new Date(timestamps.get(i)))).append("\n");
            }
        }
        sb.append("\nBest time: ");
        int bestTime = entry.getBestTime();
        if (bestTime > 0) sb.append(bestTime / 60).append("m ").append(bestTime % 60).append("s");
        else sb.append("\u2014");
        sb.append("\n");
        sb.append("Best moves: ");
        int bestMoves = entry.getBestMoves();
        sb.append(bestMoves > 0 ? bestMoves : "\u2014").append("\n");
        sb.append("Optimal moves: ");
        int optimalMoves = entry.getOptimalMoves();
        if (optimalMoves > 0) {
            sb.append(optimalMoves);
            if (bestMoves > 0 && bestMoves == optimalMoves) sb.append(" \u2713 (perfect!)");
            else if (bestMoves > 0) sb.append(" (+").append(bestMoves - optimalMoves).append(" extra)");
        } else sb.append("\u2014");
        sb.append("\n");
        sb.append("\nHint usage (this session): ");
        int maxHint = entry.getMaxHintUsed();
        if (maxHint < 0) sb.append("No hints used");
        else if (maxHint == 0) sb.append("Pre-hint viewed (robot colors revealed)");
        else sb.append("Up to hint #").append(maxHint + 1).append(" viewed");
        sb.append("\n");
        sb.append("Hints ever used (all attempts): ")
          .append(entry.isEverUsedHints() ? "Yes" : "No").append("\n");
        sb.append("Qualifies for no-hints achievement: ")
          .append(entry.qualifiesForNoHintsAchievement() ? "Yes" : "No").append("\n");
        if (entry.getBoardSize() != null && !entry.getBoardSize().isEmpty()) {
            sb.append("\nBoard: ").append(entry.getBoardSize()).append("\n");
        }
        sb.append("Map: ").append(entry.getMapName()).append("\n");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(entry.getMapName())
            .setMessage(sb.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show();
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
