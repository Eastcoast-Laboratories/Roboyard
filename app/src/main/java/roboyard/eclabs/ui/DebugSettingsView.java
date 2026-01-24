package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import roboyard.eclabs.achievements.AchievementManager;
import roboyard.eclabs.achievements.StreakManager;
import roboyard.logic.core.Constants;
import roboyard.logic.core.Preferences;
import timber.log.Timber;

/**
 * Hidden debug view for testing achievements and streaks.
 * Accessible by long-pressing (3 seconds) the settings title.
 */
public class DebugSettingsView extends LinearLayout {
    
    private Context context;
    private AchievementManager achievementManager;
    private StreakManager streakManager;
    
    public DebugSettingsView(Context context) {
        super(context);
        this.context = context;
        init();
    }
    
    public DebugSettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }
    
    private void init() {
        achievementManager = AchievementManager.getInstance(context);
        streakManager = StreakManager.getInstance(context);
        
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setPadding(16, 16, 16, 16);
        setBackgroundColor(0xFF1a1a1a);
        
        // Create scroll view for content
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(VERTICAL);
        contentLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        // Title
        TextView titleView = new TextView(context);
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
        
        // Robot Count Section
        addSectionTitle(contentLayout, "FUN CHALLENGES");
        addRobotCountInput(contentLayout);
        
        scrollView.addView(contentLayout);
        addView(scrollView);
        
        // Close button
        Button closeButton = new Button(context);
        closeButton.setText("CLOSE DEBUG VIEW");
        closeButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        closeButton.setOnClickListener(v -> {
            // Find and dismiss the parent dialog
            android.view.View parent = (android.view.View) getParent();
            while (parent != null) {
                if (parent instanceof android.widget.FrameLayout) {
                    // This is likely the dialog's content container
                    break;
                }
                parent = (android.view.View) parent.getParent();
            }
            // Post dismiss to ensure it happens after click is processed
            post(() -> {
                try {
                    if (getContext() instanceof android.app.Activity) {
                        ((android.app.Activity) getContext()).onBackPressed();
                    }
                } catch (Exception e) {
                    Timber.e(e, "[DEBUG] Error closing debug view");
                }
            });
        });
        addView(closeButton);
    }
    
    private void addSectionTitle(LinearLayout parent, String title) {
        TextView sectionTitle = new TextView(context);
        sectionTitle.setText(title);
        sectionTitle.setTextSize(14);
        sectionTitle.setTextColor(0xFF00FF00);
        sectionTitle.setPadding(0, 16, 0, 8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = 8;
        sectionTitle.setLayoutParams(params);
        parent.addView(sectionTitle);
    }
    
    private void addStreakTestModeToggle(LinearLayout parent) {
        LinearLayout toggleLayout = new LinearLayout(context);
        toggleLayout.setOrientation(HORIZONTAL);
        toggleLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        toggleLayout.setPadding(0, 8, 0, 8);
        
        Button toggleButton = new Button(context);
        toggleButton.setText(streakManager.isTestMode() ? "TEST MODE: ON" : "TEST MODE: OFF");
        toggleButton.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
        toggleButton.setOnClickListener(v -> {
            boolean newState = !streakManager.isTestMode();
            streakManager.setTestMode(newState);
            toggleButton.setText(newState ? "TEST MODE: ON" : "TEST MODE: OFF");
            Toast.makeText(context, "Test mode " + (newState ? "ENABLED (1 day = 10s)" : "DISABLED"), Toast.LENGTH_SHORT).show();
        });
        toggleLayout.addView(toggleButton);
        
        parent.addView(toggleLayout);
    }
    
    private void addStreakDaysSimulator(LinearLayout parent) {
        LinearLayout simulatorLayout = new LinearLayout(context);
        simulatorLayout.setOrientation(VERTICAL);
        simulatorLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        simulatorLayout.setPadding(0, 8, 0, 8);
        
        TextView label = new TextView(context);
        label.setText("Simulate Daily Logins:");
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(12);
        simulatorLayout.addView(label);
        
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(HORIZONTAL);
        buttonLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        for (int days = 1; days <= 7; days++) {
            final int numDays = days;
            Button btn = new Button(context);
            btn.setText(numDays + "d");
            btn.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
            btn.setOnClickListener(v -> simulateDailyLogins(numDays));
            buttonLayout.addView(btn);
        }
        
        simulatorLayout.addView(buttonLayout);
        parent.addView(simulatorLayout);
    }
    
    private void simulateDailyLogins(int days) {
        new AlertDialog.Builder(context)
                .setTitle("Simulate " + days + " Daily Logins?")
                .setMessage("This will simulate " + days + " consecutive daily logins.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    streakManager.resetStreak();
                    
                    for (int i = 0; i < days; i++) {
                        streakManager.recordDailyLogin();
                        Timber.d("[DEBUG] Simulated daily login %d/%d", i + 1, days);
                    }
                    
                    Toast.makeText(context, "Simulated " + days + " daily logins", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void addAchievementButtons(LinearLayout parent) {
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(VERTICAL);
        buttonLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        // Unlock All button
        Button unlockAllBtn = new Button(context);
        unlockAllBtn.setText("Unlock All Achievements");
        unlockAllBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        unlockAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Unlock All Achievements?")
                    .setMessage("This will unlock all achievements.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        achievementManager.unlockAll();
                        Toast.makeText(context, "All achievements unlocked", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        buttonLayout.addView(unlockAllBtn);
        
        // Reset All button
        Button resetAllBtn = new Button(context);
        resetAllBtn.setText("Reset All Achievements");
        resetAllBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        resetAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Reset All Achievements?")
                    .setMessage("This will reset all achievements to locked state.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        achievementManager.resetAll();
                        Toast.makeText(context, "All achievements reset", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        buttonLayout.addView(resetAllBtn);
        
        // Individual achievement selector
        Button selectAchievementBtn = new Button(context);
        selectAchievementBtn.setText("Toggle Individual Achievement");
        selectAchievementBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        selectAchievementBtn.setOnClickListener(v -> showAchievementSelector());
        buttonLayout.addView(selectAchievementBtn);
        
        parent.addView(buttonLayout);
    }
    
    private void showAchievementSelector() {
        // Get all achievement IDs
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
        
        // Create a scrollable list view for achievements
        ScrollView scrollView = new ScrollView(context);
        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        for (String achievementId : achievements) {
            Button btn = new Button(context);
            boolean isUnlocked = achievementManager.isUnlocked(achievementId);
            btn.setText((isUnlocked ? "✓ " : "○ ") + achievementId);
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            btn.setOnClickListener(v -> {
                if (isUnlocked) {
                    achievementManager.lock(achievementId);
                    btn.setText("○ " + achievementId);
                    Toast.makeText(context, achievementId + " LOCKED", Toast.LENGTH_SHORT).show();
                } else {
                    achievementManager.unlock(achievementId);
                    btn.setText("✓ " + achievementId);
                    Toast.makeText(context, achievementId + " UNLOCKED", Toast.LENGTH_SHORT).show();
                }
            });
            listLayout.addView(btn);
        }
        
        scrollView.addView(listLayout);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Toggle Achievements")
                .setView(scrollView)
                .setNegativeButton("Close", null)
                .create();
        dialog.show();
    }
    
    private void addGameDataButtons(LinearLayout parent) {
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(VERTICAL);
        buttonLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        // Reset all levels
        Button resetLevelsBtn = new Button(context);
        resetLevelsBtn.setText("Reset All Levels");
        resetLevelsBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        resetLevelsBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Reset All Levels?")
                    .setMessage("This will mark all levels as unplayed.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        SharedPreferences prefs = context.getSharedPreferences("roboyard_levels", Context.MODE_PRIVATE);
                        prefs.edit().clear().apply();
                        Toast.makeText(context, "All levels reset", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        buttonLayout.addView(resetLevelsBtn);
        
        parent.addView(buttonLayout);
    }
    
    private void addRobotCountInput(LinearLayout parent) {
        LinearLayout inputLayout = new LinearLayout(context);
        inputLayout.setOrientation(HORIZONTAL);
        inputLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        inputLayout.setPadding(0, 8, 0, 8);
        
        // Minus button
        Button minusBtn = new Button(context);
        minusBtn.setText("-");
        minusBtn.setLayoutParams(new LayoutParams(60, LayoutParams.WRAP_CONTENT));
        minusBtn.setOnClickListener(v -> {
            int current = Preferences.robotCount;
            if (current > 1) {
                int newCount = current - 1;
                Preferences.setRobotCount(newCount);
                updateRobotCountDisplay(inputLayout, newCount);
                Timber.d("[DEBUG] Robot count decreased to: %d", newCount);
            }
        });
        inputLayout.addView(minusBtn);
        
        // TextField
        EditText textField = new EditText(context);
        textField.setText(String.valueOf(Preferences.robotCount));
        textField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        textField.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
        textField.setGravity(Gravity.CENTER);
        textField.setTextColor(0xFFFFFFFF);
        textField.setHintTextColor(0xFF888888);
        inputLayout.addView(textField);
        
        // Plus button
        Button plusBtn = new Button(context);
        plusBtn.setText("+");
        plusBtn.setLayoutParams(new LayoutParams(60, LayoutParams.WRAP_CONTENT));
        plusBtn.setOnClickListener(v -> {
            int current = Preferences.robotCount;
            if (current < Constants.MAX_NUM_ROBOTS) {
                int newCount = current + 1;
                Preferences.setRobotCount(newCount);
                textField.setText(String.valueOf(newCount));
                Timber.d("[DEBUG] Robot count increased to: %d", newCount);
            }
        });
        inputLayout.addView(plusBtn);
        
        // Apply button
        Button applyBtn = new Button(context);
        applyBtn.setText("Apply");
        applyBtn.setLayoutParams(new LayoutParams(80, LayoutParams.WRAP_CONTENT));
        applyBtn.setOnClickListener(v -> {
            try {
                int newCount = Integer.parseInt(textField.getText().toString());
                if (newCount >= 1 && newCount <= Constants.MAX_NUM_ROBOTS) {
                    Preferences.setRobotCount(newCount);
                    Toast.makeText(context, "Robot count set to: " + newCount, Toast.LENGTH_SHORT).show();
                    Timber.d("[DEBUG] Robot count set to: %d", newCount);
                } else {
                    Toast.makeText(context, "Value must be between 1 and " + Constants.MAX_NUM_ROBOTS, Toast.LENGTH_SHORT).show();
                    textField.setText(String.valueOf(Preferences.robotCount));
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                textField.setText(String.valueOf(Preferences.robotCount));
            }
        });
        inputLayout.addView(applyBtn);
        
        parent.addView(inputLayout);
    }
    
    private void updateRobotCountDisplay(LinearLayout layout, int newCount) {
        EditText textField = null;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof EditText) {
                textField = (EditText) child;
                break;
            }
        }
        if (textField != null) {
            textField.setText(String.valueOf(newCount));
        }
    }
}
