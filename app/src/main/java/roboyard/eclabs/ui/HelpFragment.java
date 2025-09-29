package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;

/**
 * Help screen implemented as a Fragment with native Android UI components.
 * Replaces the canvas-based HelpScreen.
 */
public class HelpFragment extends BaseGameFragment {
    
    private TextView helpText;
    private Button backButton;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        
        // Set up UI elements
        helpText = view.findViewById(R.id.help_text);
        backButton = view.findViewById(R.id.back_button);
        
        // Load help content
        loadHelpContent();
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            // Navigate back
            requireActivity().onBackPressed();
        });
        
        return view;
    }
    
    /**
     * Load help content into the text view
     */
    private void loadHelpContent() {
        // Set help text with formatting
        StringBuilder helpContent = new StringBuilder();
        
        // Title
        helpContent.append(getString(R.string.help_screen_content_title)).append("\n\n");
        
        // Goal section
        helpContent.append(getString(R.string.help_goal_section_title)).append("\n");
        helpContent.append(getString(R.string.help_goal_description)).append("\n\n");
        
        // Movement section
        helpContent.append(getString(R.string.help_movement_section_title)).append("\n");
        helpContent.append(getString(R.string.help_movement_point_1)).append("\n");
        helpContent.append(getString(R.string.help_movement_point_2)).append("\n");
        helpContent.append(getString(R.string.help_movement_point_3)).append("\n\n");
        
        // Controls section
        helpContent.append(getString(R.string.help_controls_section_title)).append("\n");
        helpContent.append(getString(R.string.help_controls_point_1)).append("\n");
        helpContent.append(getString(R.string.help_controls_point_2)).append("\n");
        helpContent.append(getString(R.string.help_controls_point_3)).append("\n");
        helpContent.append(getString(R.string.help_controls_point_4)).append("\n\n");
        
        // Accessibility section
        helpContent.append(getString(R.string.help_accessibility_section_title)).append("\n");
        helpContent.append(getString(R.string.help_accessibility_description)).append("\n\n");
        
        // Tips section
        helpContent.append(getString(R.string.help_tips_section_title)).append("\n");
        helpContent.append(getString(R.string.help_tips_point_1)).append("\n");
        helpContent.append(getString(R.string.help_tips_point_2)).append("\n");
        helpContent.append(getString(R.string.help_tips_point_3)).append("\n\n");
        
        // Star system section
        helpContent.append(getString(R.string.help_star_system_section_title)).append("\n");
        helpContent.append(getString(R.string.help_star_system_description)).append("\n\n");
        helpContent.append(getString(R.string.help_star_system_4_stars)).append("\n");
        helpContent.append(getString(R.string.help_star_system_3_stars)).append("\n");
        helpContent.append(getString(R.string.help_star_system_2_stars)).append("\n");
        helpContent.append(getString(R.string.help_star_system_1_star)).append("\n");
        helpContent.append(getString(R.string.help_star_system_0_stars)).append("\n\n");
        helpContent.append(getString(R.string.help_star_system_hint_penalty));
        
        // Set the help text
        helpText.setText(helpContent.toString());
    }
    
    @Override
    public String getScreenTitle() {
        return getString(R.string.help_title);
    }
}
