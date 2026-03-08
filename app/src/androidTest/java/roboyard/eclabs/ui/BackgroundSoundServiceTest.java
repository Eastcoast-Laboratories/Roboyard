package roboyard.eclabs.ui;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.logic.core.Preferences;
import roboyard.ui.activities.MainActivity;
import timber.log.Timber;

import static org.junit.Assert.assertTrue;

/**
 * Espresso test to verify background sound service starts automatically on app launch.
 * This test analyzes the sound service lifecycle and ensures sound plays when app opens.
 */
@RunWith(AndroidJUnit4.class)
public class BackgroundSoundServiceTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Ensure background sound volume is set to a non-zero value
        Preferences.initialize(context);
        if (Preferences.backgroundSoundVolume == 0) {
            Preferences.setBackgroundSoundVolume(50);
            Timber.d("[SOUND_TEST] Set background sound volume to 50 for testing");
        }
        
        Timber.d("[SOUND_TEST] ========== TEST SETUP COMPLETE ==========");
        Timber.d("[SOUND_TEST] Background sound volume: %d", Preferences.backgroundSoundVolume);
    }

    @After
    public void tearDown() {
        Timber.d("[SOUND_TEST] ========== TEST TEARDOWN ==========");
    }

    @Test
    public void testBackgroundSoundStartsOnAppLaunch() throws InterruptedException {
        Timber.d("[SOUND_TEST] ========== TEST START: testBackgroundSoundStartsOnAppLaunch ==========");
        
        // Wait for activity to fully start
        Thread.sleep(2000);
        
        activityRule.getScenario().onActivity(activity -> {
            Timber.d("[SOUND_TEST] Activity is running: %s", activity.getClass().getSimpleName());
            Timber.d("[SOUND_TEST] Background sound volume from Preferences: %d", Preferences.backgroundSoundVolume);
            
            // Check if sound service is running
            boolean serviceRunning = isServiceRunning(context, "roboyard.SoundService");
            Timber.d("[SOUND_TEST] SoundService running: %b", serviceRunning);
            
            if (!serviceRunning) {
                Timber.e("[SOUND_TEST] FAILURE: SoundService is NOT running!");
                Timber.e("[SOUND_TEST] Expected: Service should be running with volume %d", Preferences.backgroundSoundVolume);
            } else {
                Timber.d("[SOUND_TEST] SUCCESS: SoundService is running");
            }
        });
        
        // Wait a bit more to see if service starts delayed
        Timber.d("[SOUND_TEST] Waiting 3 more seconds to check if service starts delayed...");
        Thread.sleep(3000);
        
        activityRule.getScenario().onActivity(activity -> {
            boolean serviceRunning = isServiceRunning(context, "roboyard.SoundService");
            Timber.d("[SOUND_TEST] SoundService running after 3s wait: %b", serviceRunning);
            
            // Log final verdict
            if (serviceRunning) {
                Timber.d("[SOUND_TEST] ========== TEST PASSED: Sound service is running ==========");
            } else {
                Timber.e("[SOUND_TEST] ========== TEST FAILED: Sound service is NOT running ==========");
                Timber.e("[SOUND_TEST] Check logs above for:");
                Timber.e("[SOUND_TEST]   - [SOUND_SERVICE] onCreate() - should be called");
                Timber.e("[SOUND_TEST]   - [SOUND_SERVICE] onStartCommand() - should be called");
                Timber.e("[SOUND_TEST]   - [SOUND_SERVICE] Playback STARTED - should appear");
                Timber.e("[SOUND_TEST]   - MainActivity.onCreate/onResume - check if startService was called");
            }
            
            assertTrue("Background sound service should be running on app launch", serviceRunning);
        });
        
        Timber.d("[SOUND_TEST] ========== TEST END ==========");
    }

    @Test
    public void testBackgroundSoundStartsAfterRestart() throws InterruptedException {
        Timber.d("[SOUND_TEST] ========== TEST START: testBackgroundSoundStartsAfterRestart ==========");
        
        // Wait for initial activity start
        Thread.sleep(2000);
        
        // Close and restart activity
        Timber.d("[SOUND_TEST] Closing activity...");
        activityRule.getScenario().close();
        
        Timber.d("[SOUND_TEST] Waiting 1 second before restart...");
        Thread.sleep(1000);
        
        Timber.d("[SOUND_TEST] Restarting activity...");
        ActivityScenario<MainActivity> newScenario = ActivityScenario.launch(MainActivity.class);
        
        // Wait for activity to start
        Thread.sleep(2000);
        
        newScenario.onActivity(activity -> {
            Timber.d("[SOUND_TEST] Activity restarted: %s", activity.getClass().getSimpleName());
            
            boolean serviceRunning = isServiceRunning(context, "roboyard.SoundService");
            Timber.d("[SOUND_TEST] SoundService running after restart: %b", serviceRunning);
            
            if (serviceRunning) {
                Timber.d("[SOUND_TEST] ========== TEST PASSED: Sound service running after restart ==========");
            } else {
                Timber.e("[SOUND_TEST] ========== TEST FAILED: Sound service NOT running after restart ==========");
            }
            
            assertTrue("Background sound service should be running after app restart", serviceRunning);
        });
        
        newScenario.close();
        Timber.d("[SOUND_TEST] ========== TEST END ==========");
    }

    /**
     * Check if a service is currently running
     */
    private boolean isServiceRunning(Context context, String serviceClassName) {
        android.app.ActivityManager manager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            Timber.e("[SOUND_TEST] ActivityManager is null, cannot check service status");
            return false;
        }
        
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClassName.equals(service.service.getClassName())) {
                Timber.d("[SOUND_TEST] Found service: %s, started: %b, foreground: %b", 
                        service.service.getClassName(), service.started, service.foreground);
                return true;
            }
        }
        return false;
    }
}
