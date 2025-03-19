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
        helpContent.append("How to Play Roboyard\n\n");
        helpContent.append("Goal\n");
        helpContent.append("Move the colored robots to their matching colored targets. ");
        helpContent.append("Once a robot reaches a target, it's locked in place. ");
        helpContent.append("The game is completed when all robots are on their matching targets.\n\n");
        
        helpContent.append("Movement\n");
        helpContent.append("- Tap on a robot to select it.\n");
        helpContent.append("- Tap on an empty cell to move the selected robot in that direction.\n");
        helpContent.append("- Robots will move in straight lines until they hit a wall, another robot, or the edge of the board.\n\n");
        
        helpContent.append("Controls\n");
        helpContent.append("- Hint: Shows a suggested move.\n");
        helpContent.append("- Reset: Restarts the current level.\n");
        helpContent.append("- Save: Saves your current game progress.\n");
        helpContent.append("- Menu: Returns to the main menu.\n\n");
        
        helpContent.append("Accessibility\n");
        helpContent.append("This game is fully compatible with TalkBack screen reader. ");
        helpContent.append("Use explore by touch to hear information about the game elements and buttons. ");
        helpContent.append("Double-tap to activate buttons or select robots.\n\n");
        
        helpContent.append("Tips\n");
        helpContent.append("- Try to solve puzzles in the minimum number of moves.\n");
        helpContent.append("- Sometimes you need to move robots to specific positions to clear paths for other robots.\n");
        helpContent.append("- If you're stuck, use the Hint feature to get a suggestion.\n");
        
        // Set the text
        helpText.setText(helpContent.toString());
    }
    
    @Override
    public String getScreenTitle() {
        return "Help";
    }
}
