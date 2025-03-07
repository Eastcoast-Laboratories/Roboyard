package roboyard.eclabs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import androidx.core.content.res.ResourcesCompat;

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
    
    // Static Cache for Minimap Bitmaps, to avoid repeated loading
    private static Map<String, Bitmap> minimapCache = new HashMap<>();

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
            // nothing to do on empty slots
            return;
        }
        
        // first try to load from cache
        Bitmap cachedBitmap = minimapCache.get(mapPath);
        if (cachedBitmap != null) {
            // Use cached bitmap
            Drawable minimapDrawable = new BitmapDrawable(context.getResources(), cachedBitmap);
            this.setImageUp(minimapDrawable);
            this.setImageDown(minimapDrawable); // Use same image for down state
            this.setEnabled(true);
            return;
        }
        
        // if not cached, try to load from file
        String saveData = FileReadWrite.readPrivateData(activity, mapPath);
        if (saveData != null && !saveData.isEmpty()) {
            String minimapPath = GameButtonGotoSavedGame.createMiniMap(saveData, context);
            if (minimapPath != null) {
                try {
                    // Load minimap as button image
                    Bitmap bitmap = BitmapFactory.decodeFile(minimapPath);
                    
                    // Cache the bitmap for future use
                    minimapCache.put(mapPath, bitmap);
                    
                    Drawable minimapDrawable = new BitmapDrawable(context.getResources(), bitmap);
                    this.setImageUp(minimapDrawable);
                    this.setImageDown(minimapDrawable); // Use same image for down state
                    this.setEnabled(true);
                } catch (Exception e) {
                    Timber.e(e, "Failed to load minimap as button image");
                    // Fallback to default images and enable if save exists
                    this.setEnabled(true);
                }
            } else {
                // Fallback to default images and enable if save exists
                this.setEnabled(true);
            }
        } else {
            // Disable the button if no save data exists
            this.setEnabled(false);
        }
    }

    @Override
    public void update(GameManager gameManager) {
        super.update(gameManager);
    }

    /**
     * Set the SaveGameScreen reference
     * @param saveGameScreen The SaveGameScreen instance
     */
    public void setSaveGameScreen(SaveGameScreen saveGameScreen) {
        this.saveGameScreen = saveGameScreen;
    }

    /**
     * Get the button number for this load slot
     * @return The button number
     */
    public int getButtonNumber() {
        return buttonNumber;
    }

    /**
     * Get the map name associated with this button
     * @return the map name
     */
    public String getMapName() {
        return mapPath;
    }
    
    /**
     * Get the slot ID for this button
     * @return the slot ID
     */
    public int getSlotId() {
        return buttonNumber;
    }
    
    /**
     * Clear the minimap cache for a specific map path
     * @param mapPath The map path to clear from cache
     */
    public static void clearMinimapCache(String mapPath) {
        minimapCache.remove(mapPath);
    }
    
    /**
     * Clear all minimap caches
     */
    public static void clearAllMinimapCaches() {
        minimapCache.clear();
    }
}