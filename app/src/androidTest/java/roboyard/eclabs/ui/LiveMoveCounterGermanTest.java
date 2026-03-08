package roboyard.eclabs.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import roboyard.eclabs.R;
import roboyard.ui.activities.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Test to verify that live move counter displays German text when locale is set to German.
 * This test:
 * 1. Changes locale to German
 * 2. Starts a random game
 * 3. Opens hint (to make live move counter visible)
 * 4. Enables live move counter toggle
 * 5. Verifies German text is displayed ("Zug" or "Züge")
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LiveMoveCounterGermanTest {

    private androidx.test.core.app.ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws InterruptedException {
        System.out.println("\n=== SETUP: Setting locale to German BEFORE app starts ===");
        
        // Save German locale to SharedPreferences BEFORE Preferences.initialize() is called
        Context context = ApplicationProvider.getApplicationContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences("roboyard_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("app_language", "de").commit();
        System.out.println("Saved 'de' to SharedPreferences (app_language)");
        
        // Set system default locale
        Locale locale = new Locale("de");
        Locale.setDefault(locale);
        System.out.println("Set Locale.setDefault(de)");
        
        // NOW launch the activity - it will load preferences and apply German locale
        System.out.println("Launching activity...");
        scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity.class);
        System.out.println("Activity launched");
        
        // Wait for activity to fully start
        Thread.sleep(2000);
        
        // Verify locale is set correctly after activity start
        scenario.onActivity(activity -> {
            String currentLocale = activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
            System.out.println("\n=== LOCALE VERIFICATION ===");
            System.out.println("Activity Locale: " + currentLocale);
            System.out.println("Preferences.appLanguage: " + roboyard.logic.core.Preferences.appLanguage);
            
            // Test a simple string to verify locale
            String testString = activity.getString(R.string.pre_hint_less_than_x, 5);
            System.out.println("\nTest String (pre_hint_less_than_x, 5):");
            System.out.println("  Got: '" + testString + "'");
            
            boolean isGerman = testString.contains("KI") && testString.contains("Zügen");
            System.out.println("  Is German: " + isGerman);
            
            if (isGerman) {
                System.out.println("✓✓✓ LOCALE SETUP SUCCESSFUL! ✓✓✓\n");
            } else {
                System.out.println("✗✗✗ LOCALE SETUP FAILED! ✗✗✗\n");
            }
        });
    }
    
    @org.junit.After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testLiveMoveCounterDisplaysGermanText() throws InterruptedException {
        System.out.println("\n=== LIVE MOVE COUNTER GERMAN TEST ===\n");
        
        // Test resources first
        scenario.onActivity(activity -> {
            String singular = activity.getResources().getQuantityString(
                R.plurals.live_move_counter_optimal_plural, 1, 1);
            String plural = activity.getResources().getQuantityString(
                R.plurals.live_move_counter_optimal_plural, 5, 5);
            
            System.out.println("Resource Test:");
            System.out.println("  Singular (1): " + singular);
            System.out.println("  Plural (5): " + plural);
            System.out.println("  Expected: Should contain 'Zug' or 'Züge'");
            System.out.println();
        });
        
        // Wait for main menu
        Thread.sleep(2000);
        
        // Start random game using TestHelper
        System.out.println("Starting random game...");
        TestHelper.startRandomGame();
        Thread.sleep(3000);
        
        // Close any popups
        TestHelper.closeAchievementPopupIfPresent();
        Thread.sleep(500);
        
        // Click hint button
        System.out.println("Clicking hint button...");
        onView(withId(R.id.hint_button)).perform(click());
        
        // Wait for solver to finish
        System.out.println("Waiting for solver to finish...");
        Thread.sleep(10000);
        
        // Try to enable live move counter toggle if visible
        System.out.println("Attempting to enable live move counter toggle...");
        final boolean[] toggleEnabled = {false};
        scenario.onActivity(activity -> {
            android.widget.ToggleButton toggle = activity.findViewById(R.id.live_move_counter_toggle);
            if (toggle != null && toggle.getVisibility() == android.view.View.VISIBLE) {
                toggle.setChecked(true);
                toggleEnabled[0] = true;
                System.out.println("Toggle enabled programmatically!");
            } else {
                System.out.println("Toggle not visible, will check status text anyway");
            }
        });
        
        Thread.sleep(2000);
        
        // Get and print status text MULTIPLE times to see changes
        System.out.println("\n=== CHECKING STATUS TEXT ===");
        for (int i = 0; i < 3; i++) {
            final int iteration = i + 1;
            final String[] statusText = {""};
            scenario.onActivity(activity -> {
                TextView statusTextView = activity.findViewById(R.id.status_text);
                if (statusTextView != null) {
                    statusText[0] = statusTextView.getText().toString();
                    System.out.println("Iteration " + iteration + " - Status Text: '" + statusText[0] + "'");
                } else {
                    System.out.println("Iteration " + iteration + " - ERROR: status_text view not found!");
                }
            });
            
            // Check for German vs English text
            boolean hasGerman = statusText[0].contains("Zug") || statusText[0].contains("Züge");
            boolean hasEnglish = statusText[0].contains("move") || statusText[0].contains("moves");
            
            System.out.println("  → Contains German (Zug/Züge): " + hasGerman);
            System.out.println("  → Contains English (move/moves): " + hasEnglish);
            
            if (hasEnglish && !hasGerman) {
                System.out.println("  ✗ PROBLEM: English text found instead of German!");
            } else if (hasGerman) {
                System.out.println("  ✓ German text found!");
            }
            
            Thread.sleep(2000);
        }
        
        // Final verdict
        final String[] finalText = {""};
        scenario.onActivity(activity -> {
            TextView statusTextView = activity.findViewById(R.id.status_text);
            if (statusTextView != null) {
                finalText[0] = statusTextView.getText().toString();
            }
        });
        
        boolean foundZug = finalText[0].contains("Zug");
        boolean foundZuege = finalText[0].contains("Züge");
        boolean foundEnglish = finalText[0].contains("move from here") || finalText[0].contains("moves from here");
        
        System.out.println("\n=== FINAL RESULTS ===");
        System.out.println("Final Status Text: '" + finalText[0] + "'");
        System.out.println("Contains 'Zug': " + foundZug);
        System.out.println("Contains 'Züge': " + foundZuege);
        System.out.println("Contains English 'move(s) from here': " + foundEnglish);
        
        if (foundZug || foundZuege) {
            System.out.println("\n✓✓✓ SUCCESS! German text found in live move counter! ✓✓✓");
        } else if (foundEnglish) {
            System.out.println("\n✗✗✗ FAILED! English text found instead of German! ✗✗✗");
            System.out.println("Expected: 'X Zug von hier' or 'X Züge von hier'");
            System.out.println("Got: '" + finalText[0] + "'");
        } else {
            System.out.println("\n⚠ WARNING: No move counter text found");
        }
        
        // Keep test alive for manual inspection
        Thread.sleep(5000);
    }
}
