package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

import roboyard.eclabs.R;
import roboyard.eclabs.achievements.Achievement;
import roboyard.eclabs.achievements.AchievementCategory;
import roboyard.eclabs.achievements.AchievementManager;
import timber.log.Timber;

/**
 * Fragment for displaying achievements.
 */
public class AchievementsFragment extends BaseGameFragment {
    
    private AchievementManager achievementManager;
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.achievements_title);
    }
    private LinearLayout achievementsContainer;
    private TextView progressText;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Create the layout programmatically
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_dark));
        scrollView.setPadding(32, 32, 32, 32);
        
        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Title
        TextView titleText = new TextView(requireContext());
        titleText.setText(R.string.achievements_title);
        titleText.setTextSize(28);
        titleText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        titleText.setPadding(0, 0, 0, 16);
        mainLayout.addView(titleText);
        
        // Progress
        progressText = new TextView(requireContext());
        progressText.setTextSize(18);
        progressText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        progressText.setPadding(0, 0, 0, 32);
        mainLayout.addView(progressText);
        
        // Achievements container
        achievementsContainer = new LinearLayout(requireContext());
        achievementsContainer.setOrientation(LinearLayout.VERTICAL);
        achievementsContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mainLayout.addView(achievementsContainer);
        
        // Back button
        TextView backButton = new TextView(requireContext());
        backButton.setText(R.string.back_button);
        backButton.setTextSize(20);
        backButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        backButton.setBackgroundResource(R.drawable.button_fancy_blue);
        backButton.setPadding(48, 24, 48, 24);
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        backParams.topMargin = 32;
        backButton.setLayoutParams(backParams);
        mainLayout.addView(backButton);
        
        scrollView.addView(mainLayout);
        return scrollView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        achievementManager = AchievementManager.getInstance(requireContext());
        loadAchievements();
    }
    
    private void loadAchievements() {
        int unlocked = achievementManager.getUnlockedCount();
        int total = achievementManager.getTotalCount();
        progressText.setText(getString(R.string.achievements_progress, unlocked, total));
        
        achievementsContainer.removeAllViews();
        
        // Group achievements by category
        AchievementCategory currentCategory = null;
        
        for (Achievement achievement : achievementManager.getAllAchievements()) {
            // Add category header if changed
            if (achievement.getCategory() != currentCategory) {
                currentCategory = achievement.getCategory();
                addCategoryHeader(currentCategory);
            }
            
            addAchievementItem(achievement);
        }
        
        Timber.d("[ACHIEVEMENTS] Loaded %d/%d achievements", unlocked, total);
    }
    
    private void addCategoryHeader(AchievementCategory category) {
        TextView header = new TextView(requireContext());
        header.setText(getCategoryName(category));
        header.setTextSize(20);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        header.setPadding(0, 32, 0, 16);
        achievementsContainer.addView(header);
    }
    
    private String getCategoryName(AchievementCategory category) {
        switch (category) {
            case PROGRESSION: return "Progression";
            case PERFORMANCE: return "Performance";
            case CHALLENGE: return "Challenge";
            case MASTERY: return "Mastery";
            case SPECIAL: return "Special";
            case RANDOM_DIFFICULTY: return "Random - Difficulty";
            case RANDOM_SOLUTION: return "Random - Solution Length";
            case RANDOM_RESOLUTION: return "Random - Screen Resolutions";
            case RANDOM_TARGETS: return "Random - Multiple Targets";
            case RANDOM_ROBOTS: return "Random - Robot Count";
            case RANDOM_COVERAGE: return "Random - Square Coverage";
            case RANDOM_STREAKS: return "Random - Streaks";
            case RANDOM_SPEED: return "Random - Speed";
            default: return category.name();
        }
    }
    
    private void addAchievementItem(Achievement achievement) {
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 16, 16, 16);
        itemLayout.setBackgroundColor(achievement.isUnlocked() ? 
                ContextCompat.getColor(requireContext(), R.color.achievement_unlocked) :
                ContextCompat.getColor(requireContext(), R.color.achievement_locked));
        
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        itemParams.bottomMargin = 8;
        itemLayout.setLayoutParams(itemParams);
        
        // Icon
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(achievement.getIconResId());
        icon.setAlpha(achievement.isUnlocked() ? 1.0f : 0.3f);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.rightMargin = 16;
        icon.setLayoutParams(iconParams);
        itemLayout.addView(icon);
        
        // Text container
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        // Name
        TextView nameText = new TextView(requireContext());
        nameText.setText(getStringByName(achievement.getNameKey()));
        nameText.setTextSize(16);
        nameText.setTextColor(achievement.isUnlocked() ?
                ContextCompat.getColor(requireContext(), android.R.color.white) :
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textContainer.addView(nameText);
        
        // Description
        TextView descText = new TextView(requireContext());
        descText.setText(getStringByName(achievement.getDescriptionKey()));
        descText.setTextSize(12);
        descText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        textContainer.addView(descText);
        
        itemLayout.addView(textContainer);
        
        // Unlocked indicator
        if (achievement.isUnlocked()) {
            TextView unlockedText = new TextView(requireContext());
            unlockedText.setText("✓");
            unlockedText.setTextSize(24);
            unlockedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
            itemLayout.addView(unlockedText);
        }
        
        achievementsContainer.addView(itemLayout);
    }
    
    private String getStringByName(String name) {
        int resId = getResources().getIdentifier(name, "string", requireContext().getPackageName());
        if (resId != 0) {
            return getString(resId);
        }
        return name;
    }
}
