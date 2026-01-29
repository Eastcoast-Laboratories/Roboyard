package roboyard.eclabs.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import roboyard.eclabs.R;
import roboyard.eclabs.achievements.Achievement;
import roboyard.eclabs.achievements.AchievementCategory;
import roboyard.eclabs.achievements.AchievementIconHelper;
import roboyard.eclabs.achievements.AchievementManager;
import timber.log.Timber;

/**
 * Fragment for displaying achievements.
 */
public class AchievementsFragment extends BaseGameFragment {
    
    private AchievementManager achievementManager;
    private LinearLayout achievementsContainer;
    private TextView progressText;
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.achievements_title);
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_achievements, container, false);
        
        // Set up back button
        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        // Get references to UI elements
        progressText = view.findViewById(R.id.progress_text);
        achievementsContainer = view.findViewById(R.id.achievements_container);
        
        return view;
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
        // Use centralized category display name from AchievementCategory enum
        header.setText(category.getDisplayName(requireContext()));
        header.setTextSize(20);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        header.setPadding(0, 32, 0, 16);
        achievementsContainer.addView(header);
    }
    
    private static final long NEW_ACHIEVEMENT_THRESHOLD_MS = 10 * 60 * 1000; // 10 minutes
    
    private boolean isNewAchievement(Achievement achievement) {
        if (!achievement.isUnlocked() || achievement.getUnlockedTimestamp() == 0) {
            return false;
        }
        long timeSinceUnlock = System.currentTimeMillis() - achievement.getUnlockedTimestamp();
        return timeSinceUnlock <= NEW_ACHIEVEMENT_THRESHOLD_MS;
    }
    
    private void addAchievementItem(Achievement achievement) {
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 16, 16, 16);
        
        boolean isNew = isNewAchievement(achievement);
        
        // Background color: golden for new, green for unlocked, gray for locked
        if (isNew) {
            itemLayout.setBackgroundColor(Color.parseColor("#FFF8E1")); // Light gold for new
        } else if (achievement.isUnlocked()) {
            itemLayout.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light green for unlocked
        } else {
            itemLayout.setBackgroundColor(Color.parseColor("#F5F5F5")); // Light gray for locked
        }
        
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        itemParams.bottomMargin = 8;
        itemLayout.setLayoutParams(itemParams);
        
        // Icon with achievement-specific color
        ImageView icon = new ImageView(requireContext());
        AchievementIconHelper.setIconWithAchievementColor(requireContext(), icon, achievement.getIconDrawableName(), achievement.getId());
        icon.setAlpha(achievement.isUnlocked() ? 1.0f : 0.3f);
        int iconSize = (int) requireContext().getResources().getDimension(R.dimen.achievement_icon_size);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.rightMargin = 16;
        icon.setLayoutParams(iconParams);
        itemLayout.addView(icon);
        
        // Text container
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        // Name with NEW badge if recently unlocked
        LinearLayout nameRow = new LinearLayout(requireContext());
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        TextView nameText = new TextView(requireContext());
        nameText.setText(getStringByName(achievement.getNameKey()));
        nameText.setTextSize(16);
        nameText.setTextColor(achievement.isUnlocked() ?
                Color.parseColor("#1B5E20") : // Dark green for unlocked
                Color.parseColor("#9E9E9E")); // Gray for locked
        nameRow.addView(nameText);
        
        // Add NEW badge if recently unlocked
        if (isNew) {
            TextView newBadge = new TextView(requireContext());
            newBadge.setText(" >NEW<");
            newBadge.setTextSize(12);
            newBadge.setTextColor(Color.parseColor("#FF6F00")); // Orange
            newBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            nameRow.addView(newBadge);
        }
        
        textContainer.addView(nameRow);
        
        // Description
        TextView descText = new TextView(requireContext());
        descText.setText(getStringByName(achievement.getDescriptionKey()));
        descText.setTextSize(12);
        descText.setTextColor(Color.parseColor("#666666"));
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
