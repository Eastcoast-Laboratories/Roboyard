package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.eclabs.R;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.logic.core.GameState;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.FileReadWrite;
import roboyard.ui.components.GameHistoryManager;
import roboyard.ui.components.GameStateManager;
import timber.log.Timber;

/**
 * End-to-End test verifying that random game history entries show correct minimaps.
 *
 * Flow:
 * 1. Clear history
 * 2. Start random game
 * 3. Make one move
 * 4. Navigate to Save/Load screen
 * 5. Switch to History tab
 * 6. Verify minimap is displayed correctly (not a dummy blue placeholder)
 *
 * Tags: e2e, history, random-game, minimap, espresso
 * Run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.eclabs.ui.RandomGameHistoryMinimapTest
 */
@RunWith(AndroidJUnit4.class)
public class RandomGameHistoryMinimapTest {

    private static final String TAG = "[RANDOM_HISTORY_MINIMAP]";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private GameStateManager gameStateManager;

    @Before
    public void setUp() throws InterruptedException {
        step("setUp", "Initializing and clearing history");
        activityRule.getScenario().onActivity(a ->
                gameStateManager = ((MainActivity) a).getGameStateManager());
        assertNotNull("GameStateManager must not be null", gameStateManager);
        clearAllHistory();
        Thread.sleep(2000);
    }

    @After
    public void tearDown() {
        clearAllHistory();
    }

    @Test
    public void testRandomGameHistoryShowsCorrectMinimap() throws InterruptedException {
        step("1/8", "Waiting 8 seconds for popup to close");
        Thread.sleep(8000);
        
        step("2/8", "Starting random game");
        startRandomGame();
        Thread.sleep(5000);

        step("3/8", "Waiting for solver solution");
        roboyard.logic.core.GameSolution solution = waitForSolution(15);
        assertNotNull(TAG + " Solution must be available", solution);
        assertTrue(TAG + " Solution must have moves", solution.getMoves().size() > 0);
        step("3/8", "Solution ready: " + solution.getMoves().size() + " moves");
        
        step("4/8", "Executing first move from solution");
        executeMove(solution.getMoves().get(0));
        Thread.sleep(2000);

        step("5/8", "Navigating to Save/Load screen");
        navigateToSaveLoadScreen();
        Thread.sleep(2000);

        step("6/8", "Switching to History tab");
        onView(withText("History")).perform(click());
        Thread.sleep(3000);

        step("7/8", "Verifying history has at least one entry");
        List<GameHistoryEntry> entries = getHistoryEntries();
        assertTrue(TAG + " History must have at least one entry after random game move",
                entries.size() > 0);
        
        GameHistoryEntry entry = entries.get(0);
        step("7/8", "History entry: mapName=" + entry.getMapName() 
                + ", mapPath=" + entry.getMapPath()
                + ", moves=" + entry.getMovesMade());

        step("8/8", "Verifying minimap is displayed correctly (not dummy)");
        verifyMinimapAtPosition(0);

        step("PASS", "testRandomGameHistoryShowsCorrectMinimap PASSED");
    }

    // ==================== HELPERS ====================

    private void step(String step, String msg) {
        String line = TAG + " [" + step + "] " + msg;
        Timber.d(line);
        System.out.println(line);
    }

    private void startRandomGame() throws InterruptedException {
        // Click "New Random Game" button on main menu
        onView(withId(R.id.ui_button)).perform(click());
        Thread.sleep(2000);
        // Verify game grid is displayed
        onView(withId(R.id.game_grid_view)).check(matches(isDisplayed()));
    }

    private roboyard.logic.core.GameSolution waitForSolution(int maxAttempts) throws InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            roboyard.logic.core.GameSolution s = gameStateManager.getCurrentSolution();
            if (s != null && !s.getMoves().isEmpty()) return s;
            step("solver", "Waiting... attempt " + (i + 1) + "/" + maxAttempts);
            Thread.sleep(2000);
        }
        return gameStateManager.getCurrentSolution();
    }

    private void executeMove(roboyard.logic.core.IGameMove move) {
        activityRule.getScenario().onActivity(a -> {
            if (!(move instanceof roboyard.pm.ia.ricochet.RRGameMove)) return;
            roboyard.pm.ia.ricochet.RRGameMove rrMove = (roboyard.pm.ia.ricochet.RRGameMove) move;
            int dx = 0, dy = 0;
            switch (rrMove.getDirection()) {
                case 1: dy = -1; break;
                case 2: dx =  1; break;
                case 4: dy =  1; break;
                case 8: dx = -1; break;
            }
            GameState state = gameStateManager.getCurrentState().getValue();
            if (state == null) return;
            for (roboyard.logic.core.GameElement el : state.getGameElements()) {
                if (el.getType() == roboyard.logic.core.Constants.TYPE_ROBOT && el.getColor() == rrMove.getColor()) {
                    state.setSelectedRobot(el);
                    break;
                }
            }
            gameStateManager.moveRobotInDirection(dx, dy);
            step("move", "Executed move: " + move);
        });
    }

    private void navigateToSaveLoadScreen() {
        activityRule.getScenario().onActivity(a -> {
            roboyard.ui.fragments.SaveGameFragment fragment = new roboyard.ui.fragments.SaveGameFragment();
            a.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();
        });
    }

    /**
     * Verifies that the minimap at the given RecyclerView position is:
     * 1. VISIBLE
     * 2. Has a non-null drawable/bitmap
     * 3. Is NOT a dummy blue placeholder (checks if bitmap has varied colors)
     */
    private void verifyMinimapAtPosition(int position) {
        AtomicBoolean visible = new AtomicBoolean(false);
        AtomicBoolean hasBitmap = new AtomicBoolean(false);
        AtomicBoolean isDummy = new AtomicBoolean(true);
        
        activityRule.getScenario().onActivity(a -> {
            RecyclerView rv = a.findViewById(R.id.save_slot_recycler_view);
            if (rv == null) {
                Timber.e(TAG + " history_recycler_view not found");
                return;
            }
            
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
            if (vh == null) {
                Timber.e(TAG + " ViewHolder at position %d not found", position);
                return;
            }
            
            ImageView minimap = vh.itemView.findViewById(R.id.minimap_view);
            if (minimap != null) {
                visible.set(minimap.getVisibility() == View.VISIBLE);
                
                if (minimap.getDrawable() != null) {
                    hasBitmap.set(true);
                    
                    // Check if it's a real minimap (not dummy blue placeholder)
                    try {
                        minimap.setDrawingCacheEnabled(true);
                        Bitmap bitmap = Bitmap.createBitmap(minimap.getDrawingCache());
                        minimap.setDrawingCacheEnabled(false);
                        
                        if (bitmap != null) {
                            isDummy.set(isBlueMonochrome(bitmap));
                            step("verify", "Minimap bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight()
                                    + ", isDummy=" + isDummy.get());
                        }
                    } catch (Exception e) {
                        Timber.e(e, TAG + " Error checking bitmap");
                    }
                }
                
                step("verify", "minimap visibility=" + minimap.getVisibility()
                        + " drawable=" + minimap.getDrawable()
                        + " hasBitmap=" + hasBitmap.get()
                        + " isDummy=" + isDummy.get());
            } else {
                Timber.e(TAG + " minimap_view not found in item at position %d", position);
            }
        });
        
        assertTrue(TAG + " Minimap must be VISIBLE at position " + position, visible.get());
        assertTrue(TAG + " Minimap must have a bitmap at position " + position, hasBitmap.get());
        assertFalse(TAG + " Minimap must NOT be a dummy blue placeholder at position " + position, isDummy.get());
    }

    /**
     * Checks if a bitmap is a monochrome blue placeholder (dummy).
     * Samples a few pixels and checks if they're all similar blue colors.
     */
    private boolean isBlueMonochrome(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 10 || bitmap.getHeight() < 10) {
            return false;
        }
        
        // Sample 9 pixels from different positions
        int[] pixels = new int[9];
        pixels[0] = bitmap.getPixel(5, 5);
        pixels[1] = bitmap.getPixel(bitmap.getWidth() / 2, 5);
        pixels[2] = bitmap.getPixel(bitmap.getWidth() - 5, 5);
        pixels[3] = bitmap.getPixel(5, bitmap.getHeight() / 2);
        pixels[4] = bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        pixels[5] = bitmap.getPixel(bitmap.getWidth() - 5, bitmap.getHeight() / 2);
        pixels[6] = bitmap.getPixel(5, bitmap.getHeight() - 5);
        pixels[7] = bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() - 5);
        pixels[8] = bitmap.getPixel(bitmap.getWidth() - 5, bitmap.getHeight() - 5);
        
        // Check if all pixels are similar (within threshold)
        int firstPixel = pixels[0];
        int threshold = 30; // Allow small variations
        
        for (int pixel : pixels) {
            int rDiff = Math.abs(android.graphics.Color.red(pixel) - android.graphics.Color.red(firstPixel));
            int gDiff = Math.abs(android.graphics.Color.green(pixel) - android.graphics.Color.green(firstPixel));
            int bDiff = Math.abs(android.graphics.Color.blue(pixel) - android.graphics.Color.blue(firstPixel));
            
            if (rDiff > threshold || gDiff > threshold || bDiff > threshold) {
                // Found variation - not a monochrome dummy
                return false;
            }
        }
        
        // All pixels are similar - likely a dummy
        return true;
    }

    private List<GameHistoryEntry> getHistoryEntries() {
        AtomicReference<List<GameHistoryEntry>> ref = new AtomicReference<>();
        activityRule.getScenario().onActivity(a -> ref.set(GameHistoryManager.getHistoryEntries(a)));
        return ref.get();
    }

    private void clearAllHistory() {
        AtomicReference<Activity> ref = new AtomicReference<>();
        activityRule.getScenario().onActivity(ref::set);
        Activity act = ref.get();
        if (act == null) return;
        List<GameHistoryEntry> entries = GameHistoryManager.getHistoryEntries(act);
        for (GameHistoryEntry e : entries) {
            GameHistoryManager.deleteHistoryEntry(act, e.getMapPath());
        }
        FileReadWrite.writePrivateData(act, "history_index.json", "{\"historyEntries\":[]}");
        step("setup", "History cleared (" + entries.size() + " entries removed)");
    }
}
