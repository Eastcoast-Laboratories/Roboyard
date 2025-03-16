package roboyard.eclabs;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import timber.log.Timber;

/**
 * Represents a button to load a saved game.
 * It displays minimaps 
 */
public class GameButtonLoadGame extends GameButtonGoto {

    private final int defaultImageUp;
    private final int defaultImageDown;
    private final String mapPath;
    private final int buttonNumber;
    private Context context;
    private Activity activity;
    private SaveGameScreen saveGameScreen;
    
    // Static Cache for Minimap Bitmaps, shared with GameButtonGotoSavedGame
    static Map<String, Bitmap> minimapCache = new HashMap<>();

    public GameButtonLoadGame(Context context, int x, int y, int w, int h, int imageUp, int imageDown, String mapPath, int buttonNumber) {
        super(x, y, w, h, imageUp, imageDown, 4); // 4 is the target screen for GridGameScreen
        this.defaultImageUp = imageUp;
        this.defaultImageDown = imageDown;
        this.mapPath = mapPath;
        this.buttonNumber = buttonNumber;
        this.context = context;
        this.activity = (Activity)context;
    }

    /**
     * Handles the click event of the button.
     * @param gameManager The GameManager instance.
     */
    @Override
    public void onClick(GameManager gameManager) {
        // Update activity reference
        activity = gameManager.getActivity();
        
        GridGameScreen gameScreen = (GridGameScreen) gameManager.getScreens().get(Constants.SCREEN_GAME);
        SaveGameScreen saveGameScreen = (SaveGameScreen) gameManager.getScreens().get(Constants.SCREEN_SAVE_GAMES);
        this.saveGameScreen = saveGameScreen;

        // Load the saved game
        SaveManager saver = new SaveManager(gameManager.getActivity());
        if (saver.getMapsStateSaved(mapPath, "mapsSaved.txt")) {
            super.onClick(gameManager);
            gameScreen.setSavedGame(mapPath);
        }
    }

    /**
     * Handles the click event of the button.
     * @deprecated Use onClick(GameManager) instead
     */
    @Deprecated
    public void onClick() {
        // Deprecated method, use onClick(GameManager) instead
        // This is kept for backward compatibility
    }

    @Override
    public void create() {
        super.create();
        
        // Check if save file exists and create minimap if it does
        if (activity == null && context instanceof Activity) {
            activity = (Activity)context;
        }

        if(mapPath == null || mapPath.isEmpty()) {
            Timber.d("[MINIMAP] Empty map path for load button");
            // nothing to do on empty slots
            return;
        }
        
        Timber.d("[MINIMAP] Creating load button for map: %s", mapPath);
        
        // Initialize with default images first to ensure button works correctly
        this.setImageUp(activity.getResources().getDrawable(defaultImageUp));
        this.setImageDown(activity.getResources().getDrawable(defaultImageDown));
        
        // first try to load from cache
        Bitmap cachedBitmap = minimapCache.get(mapPath);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            // Use cached bitmap
            Timber.d("[MINIMAP] Using cached minimap for: %s", mapPath);
            try {
                Drawable minimapDrawable = new BitmapDrawable(context.getResources(), cachedBitmap);
                this.setImageUp(minimapDrawable);
                this.setImageDown(minimapDrawable); // Use same image for down state
                this.setEnabled(true);
                Timber.d("[MINIMAP] Loaded cached minimap for button");
                return;
            } catch (Exception e) {
                Timber.e(e, "[MINIMAP] Error loading cached minimap");
                // Continue to try loading from file
            }
        }
        
        // if not cached, try to load from file
        Timber.d("[MINIMAP] No cached minimap, loading from file: %s", mapPath);
        String saveData = FileReadWrite.readPrivateData(activity, mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            String minimapPath = GameButtonGotoSavedGame.createMiniMap(saveData, context);
            if (minimapPath != null) {
                try {
                    // Load minimap as button image
                    Bitmap bitmap = BitmapFactory.decodeFile(minimapPath);
                    
                    if (bitmap != null && !bitmap.isRecycled()) {
                        // Cache the bitmap for future use
                        minimapCache.put(mapPath, bitmap);
                        
                        Drawable minimapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                        this.setImageUp(minimapDrawable);
                        this.setImageDown(minimapDrawable); // Use same image for down state
                        this.setEnabled(true);
                        Timber.d("[MINIMAP] Successfully created new minimap for: %s", mapPath);
                    } else {
                        Timber.e("[MINIMAP] Generated bitmap is null or recycled");
                        // Fallback to default images and enable if save exists
                        this.setEnabled(true);
                    }
                } catch (Exception e) {
                    Timber.e(e, "[MINIMAP] Failed to load minimap as button image for: %s", mapPath);
                    // Fallback to default images and enable if save exists
                    this.setEnabled(true);
                }
            } else {
                Timber.w("[MINIMAP] Failed to create minimap for: %s", mapPath);
                // Fallback to default images and enable if save exists
                this.setEnabled(true);
            }
        } else {
            Timber.d("[MINIMAP] No save data found for: %s", mapPath);
            // Disable the button if no save data exists
            this.setEnabled(false);
        }
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
    }

    /**
     * Get the button number for this load slot
     * @return The button number
     */
    public int getButtonNumber() {
        return buttonNumber;
    }

    /**
     * Clear the minimap cache for a specific map path
     * @param mapPath The map path to clear from cache
     */
    public static void clearMinimapCache(String mapPath) {
        minimapCache.remove(mapPath);
    }

}