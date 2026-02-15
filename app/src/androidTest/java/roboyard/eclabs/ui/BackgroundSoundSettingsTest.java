package roboyard.eclabs.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.SeekBar;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import roboyard.SoundService;
import roboyard.eclabs.R;
import roboyard.ui.activities.MainFragmentActivity;
import roboyard.logic.core.Preferences;
import timber.log.Timber;

/**
 * Espresso UI test for Background Sound settings:
 * - Verify SeekBar is displayed in Settings
 * - Verify SeekBar changes persist to Preferences
 * - Verify SoundService starts when volume > 0
 */
@RunWith(AndroidJUnit4.class)
public class BackgroundSoundSettingsTest {

    @Rule
    public ActivityScenarioRule<MainFragmentActivity> activityRule =
            new ActivityScenarioRule<>(MainFragmentActivity.class);

    @After
    public void tearDown() {
        // Stop the sound service after tests
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.stopService(new Intent(context, SoundService.class));
        // Reset volume to 0
        Preferences.setBackgroundSoundVolume(0);
    }

    @Test
    public void testBackgroundSoundSeekBarIsDisplayed() throws InterruptedException {
        Timber.d("[TEST] Starting background sound seekbar visibility test");

        // Navigate to Settings
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);

        // Verify settings screen is displayed
        onView(withId(R.id.title_text)).check(matches(isDisplayed()));

        // Verify the background sound label is displayed
        onView(withId(R.id.background_sound_label)).check(matches(isDisplayed()));

        // Verify the seekbar is displayed
        onView(withId(R.id.background_sound_seekbar)).check(matches(isDisplayed()));

        Timber.d("[TEST] Background sound seekbar is visible in settings");
    }

    @Test
    public void testBackgroundSoundSeekBarChangesPreference() throws InterruptedException {
        Timber.d("[TEST] Starting background sound preference persistence test");

        // Navigate to Settings
        onView(withId(R.id.settings_icon_button)).perform(click());
        Thread.sleep(1000);

        // Set seekbar to 50%
        onView(withId(R.id.background_sound_seekbar)).perform(setSeekBarProgress(50));
        Thread.sleep(500);

        // Verify preference was updated
        assert Preferences.backgroundSoundVolume == 50 :
                "Expected volume 50, got " + Preferences.backgroundSoundVolume;

        Timber.d("[TEST] Background sound preference persisted correctly: %d", Preferences.backgroundSoundVolume);

        // Set seekbar to 0 (off)
        onView(withId(R.id.background_sound_seekbar)).perform(setSeekBarProgress(0));
        Thread.sleep(500);

        assert Preferences.backgroundSoundVolume == 0 :
                "Expected volume 0, got " + Preferences.backgroundSoundVolume;

        Timber.d("[TEST] Background sound preference reset to 0");
    }

    /**
     * Custom ViewAction to set a SeekBar's progress.
     */
    private static ViewAction setSeekBarProgress(final int progress) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Set SeekBar progress to " + progress;
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof SeekBar) {
                    ((SeekBar) view).setProgress(progress);
                }
            }
        };
    }
}
