package roboyard.eclabs.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import roboyard.eclabs.achievements.AchievementManager;
import roboyard.eclabs.achievements.StreakManager;
import timber.log.Timber;

/**
 * Debug settings fragment for testing achievements and streaks.
 * Accessible by long-pressing (3 seconds) the settings title.
 */
public class DebugSettingsFragment extends Fragment {
    
    private AchievementManager achievementManager;
    private StreakManager streakManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        achievementManager = AchievementManager.getInstance(requireContext());
        streakManager = StreakManager.getInstance(requireContext());
        
        // Create main layout
        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setPadding(16, 16, 16, 16);
        mainLayout.setBackgroundColor(0xFF1a1a1a);
        
        // Create scroll view for content
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // Title
        TextView titleView = new TextView(requireContext());
        titleView.setText("DEBUG SETTINGS");
        titleView.setTextSize(20);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);
        contentLayout.addView(titleView);
        
        // Streak Test Mode Section
        addSectionTitle(contentLayout, "STREAK TESTING");
        addStreakTestModeToggle(contentLayout);
        addStreakDaysSimulator(contentLayout);
        
        // Achievement Management Section
        addSectionTitle(contentLayout, "ACHIEVEMENT MANAGEMENT");
        addAchievementButtons(contentLayout);
        
        // Level Reset Section
        addSectionTitle(contentLayout, "GAME DATA");
        addGameDataButtons(contentLayout);
        
        // App Control Section
        addSectionTitle(contentLayout, "APP CONTROL");
        addAppControlButtons(contentLayout);
        
        scrollView.addView(contentLayout);
        mainLayout.addView(scrollView);
        
        // No back button needed - use back gesture instead
        
        return mainLayout;
    }
    
    private void addSectionTitle(LinearLayout parent, String title) {
        TextView sectionTitle = new TextView(requireContext());
        sectionTitle.setText(title);
        sectionTitle.setTextSize(14);
        sectionTitle.setTextColor(0xFF00FF00);
        sectionTitle.setPadding(0, 16, 0, 8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 8;
        sectionTitle.setLayoutParams(params);
        parent.addView(sectionTitle);
    }
    
    private void addStreakTestModeToggle(LinearLayout parent) {
        LinearLayout toggleLayout = new LinearLayout(requireContext());
        toggleLayout.setOrientation(LinearLayout.HORIZONTAL);
        toggleLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        toggleLayout.setPadding(0, 8, 0, 8);
        
        Button toggleButton = new Button(requireContext());
        toggleButton.setText(streakManager.isTestMode() ? "TEST MODE: ON" : "TEST MODE: OFF");
        toggleButton.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        toggleButton.setOnClickListener(v -> {
            boolean newState = !streakManager.isTestMode();
            streakManager.setTestMode(newState);
            toggleButton.setText(newState ? "TEST MODE: ON" : "TEST MODE: OFF");
            Toast.makeText(requireContext(), 
                    "Test mode " + (newState ? "ENABLED (1 day = 10s, streak reset)" : "DISABLED (streak reset)"), 
                    Toast.LENGTH_SHORT).show();
        });
        toggleLayout.addView(toggleButton);
        
        parent.addView(toggleLayout);
    }
    
    private void addStreakDaysSimulator(LinearLayout parent) {
        LinearLayout simulatorLayout = new LinearLayout(requireContext());
        simulatorLayout.setOrientation(LinearLayout.VERTICAL);
        simulatorLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        simulatorLayout.setPadding(0, 8, 0, 8);
        
        TextView label = new TextView(requireContext());
        label.setText("Simulate daily logins (use in test mode):");
        label.setTextColor(0xFFCCCCCC);
        label.setTextSize(12);
        simulatorLayout.addView(label);
        
        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        int[] days = {1, 3, 7, 30};
        for (int day : days) {
            Button btn = new Button(requireContext());
            btn.setText(day + "d");
            btn.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            btn.setOnClickListener(v -> simulateDailyLogins(day));
            buttonRow.addView(btn);
        }
        
        simulatorLayout.addView(buttonRow);
        
        // Current streak display
        TextView streakDisplay = new TextView(requireContext());
        streakDisplay.setText("Current streak: " + streakManager.getCurrentStreak() + " days");
        streakDisplay.setTextColor(0xFFFFFF00);
        streakDisplay.setTextSize(14);
        streakDisplay.setPadding(0, 8, 0, 0);
        simulatorLayout.addView(streakDisplay);
        
        // Stored streak days display
        TextView storedStreakDisplay = new TextView(requireContext());
        storedStreakDisplay.setText("Stored streak days: " + streakManager.getStoredStreakDays());
        storedStreakDisplay.setTextColor(0xFFCCCCCC);
        storedStreakDisplay.setTextSize(12);
        storedStreakDisplay.setPadding(0, 4, 0, 0);
        simulatorLayout.addView(storedStreakDisplay);
        
        parent.addView(simulatorLayout);
    }
    
    private void simulateDailyLogins(int days) {
        if (!streakManager.isTestMode()) {
            Toast.makeText(requireContext(), "Enable test mode first!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(requireContext(), 
                "Simulating " + days + " daily logins (no achievements)... wait " + (days * 10) + " seconds", 
                Toast.LENGTH_LONG).show();
        
        // Record first login immediately (without triggering achievements)
        streakManager.recordDailyLogin();
        
        // Schedule remaining logins
        for (int i = 1; i < days; i++) {
            final int delay = i * 10000; // 10 seconds per "day"
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                streakManager.recordDailyLogin();
                Timber.d("[DEBUG] Simulated daily login - achievements NOT unlocked");
            }, delay);
        }
        
        Timber.d("[DEBUG] Simulated %d daily logins - achievements will only unlock on next game start", days);
    }
    
    private void addAchievementButtons(LinearLayout parent) {
        LinearLayout buttonLayout = new LinearLayout(requireContext());
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // Unlock all button
        Button unlockAllBtn = new Button(requireContext());
        unlockAllBtn.setText("Unlock All Achievements");
        unlockAllBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        unlockAllBtn.setOnClickListener(v -> {
            achievementManager.unlockAll();
            Toast.makeText(requireContext(), "All achievements unlocked", Toast.LENGTH_SHORT).show();
        });
        buttonLayout.addView(unlockAllBtn);
        
        // Reset all button
        Button resetAllBtn = new Button(requireContext());
        resetAllBtn.setText("Reset All Achievements");
        resetAllBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        resetAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Achievements")
                    .setMessage("Are you sure you want to reset all achievements?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        achievementManager.resetAll();
                        Toast.makeText(requireContext(), "All achievements reset", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        buttonLayout.addView(resetAllBtn);
        
        // Toggle individual achievement button
        Button toggleIndividualBtn = new Button(requireContext());
        toggleIndividualBtn.setText("Toggle Individual Achievement");
        toggleIndividualBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        toggleIndividualBtn.setOnClickListener(v -> showAchievementSelector());
        buttonLayout.addView(toggleIndividualBtn);
        
        parent.addView(buttonLayout);
    }
    
    private void showAchievementSelector() {
        String[] achievements = {
                "first_game", "level_1_complete", "level_10_complete",
                "3_star_hard_level", "3_star_10_levels",
                "speedrun_under_30s", "speedrun_random_5_games_under_30s",
                "daily_login_7", "daily_login_30", "comeback_player",
                "perfect_random_games_10", "no_hints_streak_random_10",
                "play_10_move_games_all_resolutions",
                "play_12_move_games_all_resolutions",
                "play_15_move_games_all_resolutions"
        };
        
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout listLayout = new LinearLayout(requireContext());
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        for (String achievementId : achievements) {
            Button btn = new Button(requireContext());
            boolean isUnlocked = achievementManager.isUnlocked(achievementId);
            btn.setText((isUnlocked ? "✓ " : "○ ") + achievementId);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            btn.setOnClickListener(v -> {
                boolean currentlyUnlocked = achievementManager.isUnlocked(achievementId);
                if (currentlyUnlocked) {
                    achievementManager.lock(achievementId);
                    btn.setText("○ " + achievementId);
                    Toast.makeText(requireContext(), achievementId + " LOCKED", Toast.LENGTH_SHORT).show();
                } else {
                    achievementManager.unlock(achievementId);
                    btn.setText("✓ " + achievementId);
                    Toast.makeText(requireContext(), achievementId + " UNLOCKED", Toast.LENGTH_SHORT).show();
                }
            });
            listLayout.addView(btn);
        }
        
        scrollView.addView(listLayout);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Toggle Achievements")
                .setView(scrollView)
                .setNegativeButton("Close", null)
                .show();
    }
    
    private void addGameDataButtons(LinearLayout parent) {
        LinearLayout buttonLayout = new LinearLayout(requireContext());
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        Button resetLevelsBtn = new Button(requireContext());
        resetLevelsBtn.setText("Reset All Levels");
        resetLevelsBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        resetLevelsBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Levels")
                    .setMessage("Are you sure you want to reset all level progress?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        // Reset level progress
                        android.content.SharedPreferences prefs = requireContext()
                                .getSharedPreferences("roboyard_levels", android.content.Context.MODE_PRIVATE);
                        prefs.edit().clear().apply();
                        Toast.makeText(requireContext(), "All levels reset", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        buttonLayout.addView(resetLevelsBtn);
        
        parent.addView(buttonLayout);
    }
    
    private void addAppControlButtons(LinearLayout parent) {
        LinearLayout buttonLayout = new LinearLayout(requireContext());
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        Button restartBtn = new Button(requireContext());
        restartBtn.setText("Restart App");
        restartBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        restartBtn.setOnClickListener(v -> {
            Timber.d("[DEBUG] Restarting app");
            Toast.makeText(requireContext(), "Restarting app...", Toast.LENGTH_SHORT).show();
            
            // Get the activity and restart it
            android.app.Activity activity = getActivity();
            if (activity != null) {
                android.content.Intent intent = activity.getIntent();
                activity.finish();
                startActivity(intent);
            }
        });
        buttonLayout.addView(restartBtn);
        
        parent.addView(buttonLayout);
    }
    
}
