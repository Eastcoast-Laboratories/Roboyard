package roboyard.eclabs.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;

import roboyard.eclabs.MainActivity;
import timber.log.Timber;

/**
 * A proxy class that emulates MainActivity for use within the FragmentHostActivity.
 * This allows legacy code that expects MainActivity to function correctly in the new UI.
 */
public class ProxyMainActivity extends MainActivity {
    
    private final FragmentActivity hostActivity;
    private final GameStateManager gameStateManager;
    private Context appContext;
    
    public ProxyMainActivity(FragmentActivity hostActivity, GameStateManager gameStateManager) {
        // Note: This calls MainActivity's constructor which may do some initialization
        this.hostActivity = hostActivity;
        this.gameStateManager = gameStateManager;
        this.appContext = hostActivity.getApplicationContext();
    }
    
    @Override
    public Context getApplicationContext() {
        return appContext;
    }
    
    @Override
    public Context getBaseContext() {
        return hostActivity.getBaseContext();
    }
    
    @Override
    public Resources getResources() {
        return hostActivity.getResources();
    }
    
    @Override
    public Object getSystemService(String name) {
        return hostActivity.getSystemService(name);
    }
    
    @Override
    public void startActivity(Intent intent) {
        hostActivity.startActivity(intent);
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        hostActivity.startActivityForResult(intent, requestCode);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return hostActivity.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Do not call super as we don't want to actually initialize MainActivity
        Timber.d("ProxyMainActivity onCreate called (no-op)");
    }
    
    @Override
    public void finish() {
        hostActivity.finish();
    }
    
    @Override
    public void setContentView(int layoutResID) {
        // No-op to avoid changing the host activity's content view
        Timber.d("ProxyMainActivity setContentView called (no-op)");
    }
    
//    @Override
//    public void gotoScreen(int id) {
//        // Redirect to GameStateManager for modern navigation
//        switch(id) {
//            case 0: // Constants.SCREEN_START
//                gameStateManager.navigateToMainMenu();
//                break;
//            case 1: // Constants.SCREEN_LEVEL_BEGINNER
//                gameStateManager.navigateToLevelScreen(1);
//                break;
//            case 2: // Constants.SCREEN_LEVEL_INTERMEDIATE
//                gameStateManager.navigateToLevelScreen(2);
//                break;
//            case 3: // Constants.SCREEN_LEVEL_ADVANCED
//                gameStateManager.navigateToLevelScreen(3);
//                break;
//            case 4: // Constants.SCREEN_LEVEL_EXPERT
//                gameStateManager.navigateToLevelScreen(4);
//                break;
//            case 5: // Constants.SCREEN_GAME
//                // Already in game screen
//                break;
//            case 6: // Constants.SCREEN_SETTINGS
//                gameStateManager.navigateToSettings();
//                break;
//            case 7: // Constants.SCREEN_SAVE
//                gameStateManager.navigateToSaveScreen(true);
//                break;
//            default:
//                Timber.w("Unknown screen ID: %d", id);
//                break;
//        }
//    }
    
    // Methods for dealing with the application context safely
    public void setBoardSize(Context context, int width, int height) {
        // Proxy to GameStateManager or appropriate context
        Timber.d("Setting board size: %dx%d", width, height);
        // Implementation depends on how this is used in the game
    }
    
    public void toggleSound(boolean enabled) {
        gameStateManager.setSoundEnabled(enabled);
    }
}
