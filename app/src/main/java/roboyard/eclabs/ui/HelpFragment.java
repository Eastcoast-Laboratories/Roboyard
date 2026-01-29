package roboyard.eclabs.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;

/**
 * Help screen implemented as a Fragment with native Android UI components.
 * Styled to match Laravel web app (roboyard.z11.de/impressum).
 */
public class HelpFragment extends BaseGameFragment {
    
    private LinearLayout helpContentContainer;
    private Button backButton;
    
    // Colors matching Laravel style
    private static final int COLOR_TEXT = Color.parseColor("#212529");
    private static final int COLOR_MUTED = Color.parseColor("#6c757d");
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        
        // Set up UI elements
        helpContentContainer = view.findViewById(R.id.help_content_container);
        backButton = view.findViewById(R.id.back_button);
        
        // Load help content with proper styling
        loadHelpContent();
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
        
        return view;
    }
    
    /**
     * Load help content with Laravel-style formatting (h3 headings + p text)
     */
    private void loadHelpContent() {
        // Clear any existing content
        helpContentContainer.removeAllViews();
        
        // Goal section
        addSectionHeading(getString(R.string.help_goal_section_title));
        addParagraph(getString(R.string.help_goal_description));
        
        // Movement section
        addSectionHeading(getString(R.string.help_movement_section_title));
        addParagraph(getString(R.string.help_movement_point_1) + "\n" +
                     getString(R.string.help_movement_point_2) + "\n" +
                     getString(R.string.help_movement_point_3));
        
        // Controls section
        addSectionHeading(getString(R.string.help_controls_section_title));
        addParagraph(getString(R.string.help_controls_point_1) + "\n" +
                     getString(R.string.help_controls_point_2) + "\n" +
                     getString(R.string.help_controls_point_3) + "\n" +
                     getString(R.string.help_controls_point_4));
        
        // Accessibility section
        addSectionHeading(getString(R.string.help_accessibility_section_title));
        addParagraph(getString(R.string.help_accessibility_description));
        
        // Tips section
        addSectionHeading(getString(R.string.help_tips_section_title));
        addParagraph(getString(R.string.help_tips_point_1) + "\n" +
                     getString(R.string.help_tips_point_2) + "\n" +
                     getString(R.string.help_tips_point_3));
        
        // Star system section
        addSectionHeading(getString(R.string.help_star_system_section_title));
        addParagraph(getString(R.string.help_star_system_description));
        addParagraph(getString(R.string.help_star_system_4_stars) + "\n" +
                     getString(R.string.help_star_system_3_stars) + "\n" +
                     getString(R.string.help_star_system_2_stars) + "\n" +
                     getString(R.string.help_star_system_1_star) + "\n" +
                     getString(R.string.help_star_system_0_stars));
        addParagraph(getString(R.string.help_star_system_hint_penalty));
    }
    
    /**
     * Add a section heading (h3 style - 18sp, not bold, like Laravel)
     */
    private void addSectionHeading(String text) {
        TextView heading = new TextView(requireContext());
        heading.setText(text);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heading.setTextColor(COLOR_TEXT);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(16);
        heading.setLayoutParams(params);
        
        helpContentContainer.addView(heading);
    }
    
    /**
     * Add a paragraph (p style - 14sp, normal weight, like Laravel)
     */
    private void addParagraph(String text) {
        TextView paragraph = new TextView(requireContext());
        paragraph.setText(text);
        paragraph.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        paragraph.setTextColor(COLOR_TEXT);
        paragraph.setLineSpacing(0, 1.6f);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(8);
        paragraph.setLayoutParams(params);
        
        helpContentContainer.addView(paragraph);
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.help_title);
    }
}
