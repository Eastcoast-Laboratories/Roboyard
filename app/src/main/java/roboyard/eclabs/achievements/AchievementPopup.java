package roboyard.eclabs.achievements;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import roboyard.eclabs.R;
import roboyard.eclabs.achievements.AchievementIconHelper;
import roboyard.eclabs.ui.AchievementsFragment;
import timber.log.Timber;

/**
 * Popup that shows when achievements are unlocked.
 * Fades in from top, shows for 20 seconds, then fades out.
 * If clicked, stays visible until manually closed with X button.
 * Supports multiple achievements with scrollable list.
 */
public class AchievementPopup {
    
    private static final int DISPLAY_DURATION_MS = 20000; // 20 seconds
    private static final int FADE_DURATION_MS = 500;
    private static final int BACKGROUND_COLOR_SEMI_TRANSPARENT = Color.argb(230, 255, 255, 255); // White with ~90% opacity
    
    private final Context context;
    private final ViewGroup rootView;
    private FrameLayout popupContainer;
    private final List<Achievement> pendingAchievements = new ArrayList<>();
    private boolean isShowing = false;
    private boolean isPermanent = false; // When true, popup won't auto-close
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    public AchievementPopup(Context context, ViewGroup rootView) {
        this.context = context;
        this.rootView = rootView;
    }
    
    /**
     * Show achievement unlock popup for one or more achievements.
     */
    public void show(List<Achievement> achievements) {
        if (achievements == null || achievements.isEmpty()) {
            return;
        }
        
        pendingAchievements.addAll(achievements);
        
        if (!isShowing) {
            showPopup();
        }
    }
    
    /**
     * Show achievement unlock popup for a single achievement.
     */
    public void show(Achievement achievement) {
        List<Achievement> list = new ArrayList<>();
        list.add(achievement);
        show(list);
    }
    
    private void showPopup() {
        if (pendingAchievements.isEmpty()) {
            isShowing = false;
            return;
        }
        
        isShowing = true;
        isPermanent = false;
        
        // Create popup container
        popupContainer = new FrameLayout(context);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.TOP;
        containerParams.topMargin = 50;
        containerParams.leftMargin = 32;
        containerParams.rightMargin = 32;
        popupContainer.setLayoutParams(containerParams);
        // Very high elevation to ensure it's on top of all other views
        popupContainer.setElevation(1000);
        popupContainer.setZ(1000);
        
        // Main card layout with semi-transparent white background
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(BACKGROUND_COLOR_SEMI_TRANSPARENT);
        cardLayout.setPadding(32, 32, 32, 32);
        cardLayout.setElevation(1000);
        cardLayout.setZ(1000);
        
        // Header with title and close button
        FrameLayout headerLayout = new FrameLayout(context);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Title
        TextView titleText = new TextView(context);
        titleText.setText(pendingAchievements.size() == 1 ? 
                context.getString(R.string.achievement_unlocked) : 
                context.getString(R.string.achievements_unlocked_multiple));
        titleText.setTextSize(20);
        titleText.setTextColor(Color.parseColor("#4CAF50"));
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(40, 0, 40, 16); // Extra padding for close button space
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleText.setLayoutParams(titleParams);
        headerLayout.addView(titleText);
        
        // Close button (X) - initially hidden, shown when popup becomes permanent
        TextView closeButton = new TextView(context);
        closeButton.setText("✕");
        closeButton.setTextSize(24);
        closeButton.setTextColor(Color.parseColor("#666666"));
        closeButton.setPadding(16, 0, 16, 0);
        closeButton.setVisibility(View.GONE);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.END | Gravity.TOP;
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> hidePopup());
        headerLayout.addView(closeButton);
        
        cardLayout.addView(headerLayout);
        
        // Make popup permanent when clicked (cancel auto-close, show X button)
        cardLayout.setOnClickListener(v -> {
            if (!isPermanent) {
                isPermanent = true;
                handler.removeCallbacksAndMessages(null); // Cancel auto-close
                closeButton.setVisibility(View.VISIBLE);
                Timber.d("[ACHIEVEMENT_POPUP] Popup clicked, now permanent");
            }
        });
        
        // Achievements list (scrollable if many)
        if (pendingAchievements.size() > 3) {
            ScrollView scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    400)); // Max height
            
            LinearLayout achievementsList = createAchievementsList();
            scrollView.addView(achievementsList);
            cardLayout.addView(scrollView);
        } else {
            LinearLayout achievementsList = createAchievementsList();
            cardLayout.addView(achievementsList);
        }
        
        // View Achievements button
        Button viewButton = new Button(context);
        viewButton.setText(R.string.view_achievements);
        viewButton.setTextColor(Color.WHITE);
        viewButton.setBackgroundResource(R.drawable.button_fancy_purple);
        viewButton.setPadding(32, 16, 32, 16);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.topMargin = 16;
        viewButton.setLayoutParams(buttonParams);
        viewButton.setOnClickListener(v -> {
            hidePopup();
            navigateToAchievements();
        });
        cardLayout.addView(viewButton);
        
        popupContainer.addView(cardLayout);
        
        // Start off-screen (above)
        popupContainer.setTranslationY(-500);
        popupContainer.setAlpha(0f);
        
        // Add to root view
        rootView.addView(popupContainer);
        
        // Animate in
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(popupContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(FADE_DURATION_MS);
        
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(popupContainer, "translationY", -500f, 0f);
        slideIn.setDuration(FADE_DURATION_MS);
        
        fadeIn.start();
        slideIn.start();
        
        // Schedule auto-hide
        handler.postDelayed(this::hidePopup, DISPLAY_DURATION_MS);
        
        // Clear pending achievements
        pendingAchievements.clear();
        
        Timber.d("[ACHIEVEMENT_POPUP] Showing popup");
    }
    
    private LinearLayout createAchievementsList() {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        for (Achievement achievement : pendingAchievements) {
            LinearLayout itemLayout = new LinearLayout(context);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 12, 16, 12);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            // Icon with achievement-specific color (larger, 128x128)
            ImageView icon = new ImageView(context);
            AchievementIconHelper.setIconWithAchievementColor(context, icon, achievement.getIconDrawableName(), achievement.getId());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(128, 128);
            iconParams.rightMargin = 16;
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);
            
            // Text container
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            // Name
            TextView nameText = new TextView(context);
            nameText.setText(getStringByName(achievement.getNameKey()));
            nameText.setTextSize(16);
            nameText.setTextColor(Color.parseColor("#333333"));
            textContainer.addView(nameText);
            
            // Description
            TextView descText = new TextView(context);
            descText.setText(getStringByName(achievement.getDescriptionKey()));
            descText.setTextSize(12);
            descText.setTextColor(Color.parseColor("#666666"));
            textContainer.addView(descText);
            
            itemLayout.addView(textContainer);
            list.addView(itemLayout);
        }
        
        return list;
    }
    
    private void hidePopup() {
        if (popupContainer == null) {
            isShowing = false;
            return;
        }
        
        handler.removeCallbacksAndMessages(null);
        
        // Animate out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(popupContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(FADE_DURATION_MS);
        
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(popupContainer, "translationY", 0f, -500f);
        slideOut.setDuration(FADE_DURATION_MS);
        
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (popupContainer != null && popupContainer.getParent() != null) {
                    rootView.removeView(popupContainer);
                }
                popupContainer = null;
                isShowing = false;
                
                // Show next batch if any
                if (!pendingAchievements.isEmpty()) {
                    showPopup();
                }
            }
        });
        
        fadeOut.start();
        slideOut.start();
        
        Timber.d("[ACHIEVEMENT_POPUP] Hiding popup");
    }
    
    private void navigateToAchievements() {
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            AchievementsFragment fragment = new AchievementsFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
    
    private String getStringByName(String name) {
        int resId = context.getResources().getIdentifier(name, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return name;
    }
}
