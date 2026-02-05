package roboyard.eclabs.achievements;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    
    public static final String STREAK_POPUP_ID = "daily_streak_popup";
    
    private final Context context;
    private final ViewGroup rootView;
    private FrameLayout popupContainer;
    private final List<Achievement> pendingAchievements = new ArrayList<>();
    private boolean isShowing = false;
    private boolean isPermanent = false; // When true, popup won't auto-close
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PopupVisibilityListener popupVisibilityListener;
    private boolean isCurrentStreakPopup = false;

    public interface PopupVisibilityListener {
        void onPopupVisibilityChanged(boolean isVisible, boolean isStreakPopup);
    }
    
    public AchievementPopup(Context context, ViewGroup rootView) {
        this.context = context;
        this.rootView = rootView;
    }
    
    public void setPopupVisibilityListener(PopupVisibilityListener listener) {
        this.popupVisibilityListener = listener;
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
        isCurrentStreakPopup = pendingAchievements.size() == 1 &&
                STREAK_POPUP_ID.equals(pendingAchievements.get(0).getId());
        
        // Screen metrics
        float density = context.getResources().getDisplayMetrics().density;
        int screenWidthDp = (int) (context.getResources().getDisplayMetrics().widthPixels / density);
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        // Popup height constant (tunable)
        final int POPUP_HEIGHT_DP = 220;
        int popupHeightPx = (int) (POPUP_HEIGHT_DP * density);
        
        // Button height constant (tunable)
        final int BUTTON_HEIGHT_DP = 50;
        int buttonHeightPx = (int) (BUTTON_HEIGHT_DP * density);
        
        // Horizontal padding (tunable) - more in landscape, less in portrait
        final int HORIZONTAL_MARGIN_PORTRAIT_DP = 32;
        final int HORIZONTAL_MARGIN_LANDSCAPE_DP = 48;
        int horizontalMarginPx = isLandscape ? 
                (int) (HORIZONTAL_MARGIN_LANDSCAPE_DP * density) : 
                (int) (HORIZONTAL_MARGIN_PORTRAIT_DP * density);
        
        // ========== POPUP CONTAINER ==========
        popupContainer = new FrameLayout(context);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeightPx);
        containerParams.gravity = Gravity.TOP;
        containerParams.topMargin = (int) (40 * density);
        containerParams.leftMargin = horizontalMarginPx;
        containerParams.rightMargin = horizontalMarginPx;
        popupContainer.setLayoutParams(containerParams);
        popupContainer.setElevation(1000);
        popupContainer.setZ(1000);
        popupContainer.setClipChildren(false);
        popupContainer.setClipToPadding(false);
        
        // ========== MAIN BOX WITH BACKGROUND ==========
        FrameLayout mainBox = new FrameLayout(context);
        mainBox.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        int popupBgResId = isLandscape ? R.drawable.achievement_popup_bg_landscape : R.drawable.achievement_popup_bg;
        mainBox.setBackgroundResource(popupBgResId);
        mainBox.setElevation(1000);
        mainBox.setZ(1000);
        Timber.d("[ACHIEVEMENT_POPUP_BG] isLandscape=%b bg=%s", isLandscape, context.getResources().getResourceEntryName(popupBgResId));
        
        // ========== VIEW ACHIEVEMENTS BUTTON (BOTTOM ALIGNED, NO MARGINS) ==========
        FrameLayout buttonContainer = new FrameLayout(context);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                buttonHeightPx);
        buttonParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        buttonContainer.setLayoutParams(buttonParams);
        buttonContainer.setOnClickListener(v -> {
            hidePopup();
            navigateToAchievements();
        });
        
        // Button image
        ImageView buttonImage = new ImageView(context);
        buttonImage.setImageResource(R.drawable.achievement_button);
        buttonImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        buttonImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        buttonContainer.addView(buttonImage);
        
        // Button text overlay
        TextView buttonText = new TextView(context);
        buttonText.setText(R.string.view_achievements);
        buttonText.setTextColor(Color.WHITE);
        buttonText.setTextSize(14);
        buttonText.setGravity(Gravity.CENTER);
        buttonText.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        buttonContainer.addView(buttonText);
        
        mainBox.addView(buttonContainer);
        
        // ========== SCROLLABLE CONTENT AREA (ABOVE BUTTON) ==========
        ScrollView scrollView = new ScrollView(context);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeightPx - buttonHeightPx);
        scrollParams.gravity = Gravity.TOP;
        scrollView.setLayoutParams(scrollParams);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setFillViewport(true);
        scrollView.setClickable(true);
        scrollView.setFocusable(true);
        
        // Content inside scroll: title + achievements list
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        contentLayout.setPadding(horizontalMarginPx, (int)(12 * density), horizontalMarginPx, 0);
        contentLayout.setClickable(true);
        contentLayout.setFocusable(true);
        
        // Close button (X) - top right, initially hidden
        final TextView closeButton = new TextView(context);
        closeButton.setText("✕");
        closeButton.setTextSize(32);
        closeButton.setTextColor(Color.parseColor("#0f5a11"));
        closeButton.setPadding((int)(16 * density), (int)(12 * density), (int)(16 * density), (int)(12 * density));
        closeButton.setBackground(context.getResources().getDrawable(R.drawable.close_button_bg));
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setVisibility(View.GONE);
        closeButton.setOnClickListener(v -> hidePopup());
        closeButton.setClickable(true);
        closeButton.setFocusable(true);
        
        // Title (only for streak popup or single achievement)
        boolean showTitle = isCurrentStreakPopup || pendingAchievements.size() == 1;
        if (showTitle) {
            TextView titleText = new TextView(context);
            if (isCurrentStreakPopup) {
                titleText.setText(getStringByName("streak_popup_title", null));
                // Streak popup: 4sp larger (18 + 4 = 22)
                titleText.setTextSize(22);
            } else {
                titleText.setText(context.getString(R.string.achievement_unlocked));
                // Single achievement: keep original size
                titleText.setTextSize(18);
            }
            titleText.setTextColor(Color.parseColor("#0f5a11"));
            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
            titleText.setGravity(Gravity.CENTER);
            titleText.setPadding(0, 0, 0, (int)(8 * density));
            contentLayout.addView(titleText);
        }
        
        // Achievements list
        LinearLayout achievementsList = createAchievementsList(isCurrentStreakPopup, pendingAchievements.size(), isLandscape);
        contentLayout.addView(achievementsList);
        
        scrollView.addView(contentLayout);
        
        // Set OnClickListener on contentLayout - this works because contentLayout is clickable
        contentLayout.setOnClickListener(v -> {
            if (!isPermanent) {
                isPermanent = true;
                handler.removeCallbacksAndMessages(null);
                closeButton.setVisibility(View.VISIBLE);
                Timber.d("[ACHIEVEMENT_POPUP] Content area clicked, now permanent - timer paused, close button shown. closeButton visibility=%d", closeButton.getVisibility());
            }
        });
        
        mainBox.addView(scrollView);
        popupContainer.addView(mainBox);
        
        // Add close button to popupContainer (not mainBox) so it can be positioned at screen top
        // Position at top right with upper edge outside viewport
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.END | Gravity.TOP;
        
        // Right margin: more in landscape (10dp further left), less in portrait
        int rightMarginPx = isLandscape ? 
                (int)(58 * density) : 
                (int)(32 * density);
        closeParams.rightMargin = rightMarginPx;
        
        // Top margin: negative so upper edge goes outside viewport, positioned deeper
        closeParams.topMargin = (int)(-20 * density);
        
        closeButton.setLayoutParams(closeParams);
        closeButton.setElevation(2000);
        closeButton.setZ(2000);
        popupContainer.addView(closeButton);
        
        // Start off-screen (above)
        popupContainer.setTranslationY(-500);
        popupContainer.setAlpha(0f);
        
        // Add to root view
        rootView.addView(popupContainer);
        
        if (popupVisibilityListener != null) {
            popupVisibilityListener.onPopupVisibilityChanged(true, isCurrentStreakPopup);
        }
        
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
    
    private LinearLayout createAchievementsList(boolean isStreakPopup, int achievementCount, boolean isLandscape) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        
        float density = context.getResources().getDisplayMetrics().density;
        
        // Determine text sizes and icon size: larger for single achievement or streak popup
        boolean enlargeContent = isStreakPopup || achievementCount == 1;
        int nameTextSize = enlargeContent ? 20 : 16;
        int descTextSize = enlargeContent ? 18 : 14;
        
        for (Achievement achievement : pendingAchievements) {
            LinearLayout itemLayout = new LinearLayout(context);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 12, 16, 12);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            // Icon with achievement-specific color
            ImageView icon = new ImageView(context);
            AchievementIconHelper.setIconWithAchievementColor(context, icon, achievement.getIconDrawableName(), achievement.getId());
            int iconSize = (int) context.getResources().getDimension(R.dimen.achievement_icon_size);
            // Increase icon size by 50% for single achievement or streak popup
            if (enlargeContent) {
                iconSize = (int) (iconSize * 1.5);
            }
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            // Increase right margin in landscape mode
            iconParams.rightMargin = isLandscape ? (int)(32 * density) : 16;
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);
            
            // Text container
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            // Name
            TextView nameText = new TextView(context);
            nameText.setText(getStringByName(achievement.getNameKey(), achievement.getNameFormatArgs()));
            nameText.setTextSize(nameTextSize);
            nameText.setTextColor(Color.parseColor("#333333"));
            nameText.setTypeface(null, android.graphics.Typeface.BOLD);
            textContainer.addView(nameText);
            
            // Description
            TextView descText = new TextView(context);
            descText.setText(getStringByName(achievement.getDescriptionKey(), achievement.getDescriptionFormatArgs()));
            descText.setTextSize(descTextSize);
            descText.setTextColor(Color.parseColor("#0f5a11"));
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

                if (popupVisibilityListener != null) {
                    popupVisibilityListener.onPopupVisibilityChanged(false, isCurrentStreakPopup);
                }
                isCurrentStreakPopup = false;
                
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
    
    private String getStringByName(String name, Object[] formatArgs) {
        if (name == null) {
            return "";
        }
        int resId = context.getResources().getIdentifier(name, "string", context.getPackageName());
        if (resId != 0) {
            if (formatArgs != null && formatArgs.length > 0) {
                return context.getString(resId, formatArgs);
            }
            return context.getString(resId);
        }
        if (formatArgs != null && formatArgs.length > 0) {
            return String.format(Locale.getDefault(), name, formatArgs);
        }
        return name;
    }
}
