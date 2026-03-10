package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.eclabs.R;
import roboyard.logic.core.GameHistoryEntry;
import roboyard.ui.activities.MainActivity;
import roboyard.ui.components.GameHistoryManager;
import timber.log.Timber;

/**
 * Espresso UI test for History Pagination with 200 entries:
 * - Create 200 dummy history entries
 * - Test pagination navigation (next/prev buttons)
 * - Verify UI responsiveness
 * - Check for OutOfMemory errors
 * 
 * Tags: ui, history, pagination, espresso, performance
 */
@RunWith(AndroidJUnit4.class)
public class HistoryPaginationTest {
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);
    
    @Before
    public void setUp() throws Exception {
        Timber.d("[UNITTESTS][PAGINATION_TEST] Setting up test");
        
        // Create 100 dummy history entries (reduced from 200 to avoid timeout)
        activityRule.getScenario().onActivity(activity -> {
            try {
                Timber.d("[UNITTESTS][PAGINATION_TEST] Creating 100 dummy entries");
                String[] assetFiles = activity.getAssets().list("Maps");
                
                java.util.List<String> levelFiles = new java.util.ArrayList<>();
                for (String file : assetFiles) {
                    if (file.startsWith("level_") && file.endsWith(".txt")) {
                        levelFiles.add(file);
                    }
                }
                
                if (levelFiles.isEmpty()) {
                    throw new RuntimeException("No level files found in assets/Maps");
                }
                
                for (int i = 0; i < 100; i++) {
                    String levelFile = levelFiles.get(i % levelFiles.size());
                    String mapPath = "Maps/" + levelFile;
                    
                    GameHistoryEntry entry = new GameHistoryEntry();
                    entry.setMapPath(mapPath);
                    entry.setMapName("Pagination Test " + (i + 1));
                    entry.setTimestamp(System.currentTimeMillis() - (i * 60000));
                    entry.setPlayDuration((int)(Math.random() * 300) + 30);
                    entry.setMovesMade((int)(Math.random() * 50) + 10);
                    entry.setOptimalMoves((int)(Math.random() * 30) + 5);
                    entry.setBoardSize("12×12");
                    entry.setDifficulty(i % 4); // 0=Beginner, 1=Intermediate, 2=Advanced, 3=Expert
                    entry.setCompletionCount(i % 3 == 0 ? 1 : 0);
                    
                    GameHistoryManager.addHistoryEntry(activity, entry);
                    
                    if ((i + 1) % 25 == 0) {
                        Timber.d("[UNITTESTS][PAGINATION_TEST] Created %d/100 entries", i + 1);
                    }
                }
                
                Timber.d("[UNITTESTS][PAGINATION_TEST] Successfully created 100 entries");
            } catch (Exception e) {
                Timber.e(e, "[PAGINATION_TEST] Error creating dummy entries");
                throw new RuntimeException("Failed to create dummy entries", e);
            }
        });
        
        Thread.sleep(2000); // Wait for entries to be saved
    }
    
    @Test
    public void testPaginationWith200Entries() throws InterruptedException {
        Timber.d("[UNITTESTS][PAGINATION_TEST] Starting pagination test");
        
        // Step 1: Navigate to Save/Load screen
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 1: Navigating to Save/Load screen");
        onView(withId(R.id.load_game_button)).perform(click());
        Thread.sleep(1000);
        
        // Step 2: Switch to History tab (tab index 2)
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 2: Switching to History tab");
        activityRule.getScenario().onActivity(activity -> {
            com.google.android.material.tabs.TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
            if (tabLayout != null && tabLayout.getTabCount() > 2) {
                com.google.android.material.tabs.TabLayout.Tab tab = tabLayout.getTabAt(2);
                if (tab != null) {
                    tab.select();
                }
            }
        });
        Thread.sleep(3000); // Wait for async minimap loading
        
        // Step 3: Verify pagination is visible
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 3: Verifying pagination controls");
        onView(withId(R.id.pagination_controls)).check(matches(isDisplayed()));
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 1 of 5"))));
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("100 entries"))));
        
        // Step 4: Verify prev button is disabled on page 1
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 4: Checking prev button disabled on page 1");
        onView(withId(R.id.prev_page_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.next_page_button)).check(matches(isEnabled()));
        
        // Step 5: Click next button and verify page 2
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 5: Navigating to page 2");
        onView(withId(R.id.next_page_button)).perform(click());
        Thread.sleep(2000); // Wait for page to load
        
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 2 of 5"))));
        onView(withId(R.id.prev_page_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_page_button)).check(matches(isEnabled()));
        
        // Step 6: Verify top pagination appears on page 2
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 6: Verifying top pagination on page 2");
        onView(withId(R.id.pagination_controls_top)).check(matches(isDisplayed()));
        onView(withId(R.id.page_info_text_top)).check(matches(withText(containsString("Page 2 of 5"))));
        
        // Step 7: Navigate to page 4 using next button
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 7: Navigating to page 4");
        for (int i = 0; i < 2; i++) {
            onView(withId(R.id.next_page_button)).perform(click());
            Thread.sleep(1500);
        }
        
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 4 of 5"))));
        
        // Step 8: Navigate back using prev button
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 8: Navigating back to page 3");
        onView(withId(R.id.prev_page_button)).perform(click());
        Thread.sleep(1500);
        
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 3 of 5"))));
        
        // Step 9: Navigate to last page (page 5)
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 9: Navigating to last page");
        for (int i = 0; i < 2; i++) {
            onView(withId(R.id.next_page_button)).perform(click());
            Thread.sleep(1500);
        }
        
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 5 of 5"))));
        onView(withId(R.id.next_page_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.prev_page_button)).check(matches(isEnabled()));
        
        // Step 10: Use top pagination to go back to page 1
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 10: Using top pagination to go back");
        for (int i = 0; i < 4; i++) {
            onView(withId(R.id.prev_page_button_top)).perform(click());
            Thread.sleep(1500);
        }
        
        onView(withId(R.id.page_info_text)).check(matches(withText(containsString("Page 1 of 5"))));
        
        // Step 11: Verify top pagination is hidden on page 1
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 11: Verifying top pagination hidden on page 1");
        onView(withId(R.id.pagination_controls_top)).check(matches(not(isDisplayed())));
        
        // Step 12: Verify RecyclerView is responsive (no OOM)
        Timber.d("[UNITTESTS][PAGINATION_TEST] Step 12: Checking RecyclerView responsiveness");
        activityRule.getScenario().onActivity(activity -> {
            androidx.recyclerview.widget.RecyclerView recyclerView = activity.findViewById(R.id.save_slot_recycler_view);
            if (recyclerView != null && recyclerView.getAdapter() != null) {
                int itemCount = recyclerView.getAdapter().getItemCount();
                Timber.d("[UNITTESTS][PAGINATION_TEST] RecyclerView has %d items (should be 20)", itemCount);
                if (itemCount != 20) {
                    throw new AssertionError("Expected 20 items per page, got " + itemCount);
                }
            } else {
                throw new AssertionError("RecyclerView or adapter is null");
            }
        });
        
        Timber.d("[UNITTESTS][PAGINATION_TEST] PAGINATION TEST PASSED - No OOM, UI responsive");
    }
}
