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

        String helpContent = "How to Play Roboyard\n\n" +
                "Goal\n" +
                "Move the colored robots to their matching colored targets. " +
                "The game is completed when all robots are on their matching targets.\n\n" +
                "Movement\n" +
                "- Tap on a robot to select it.\n" +
                "- Tap on an empty cell to move the selected robot in that direction.\n" +
                "- Robots will move in straight lines until they hit a wall, another robot, or the edge of the board.\n\n" +
                "Controls\n" +
                "- Hint: Shows a suggested move.\n" +
                "- Reset: Restarts the current level.\n" +
                "- Save: Saves your current map.\n" +
                "- Menu: Returns to the main menu.\n\n" +
                "Accessibility\n" +
                "This game is fully compatible with TalkBack screen reader. " +
                "Use explore by touch to hear information about the game elements and buttons. " +
                "Double-tap to activate buttons or select robots.\n\n" +
                "Tips\n" +
                "- Try to solve puzzles in the minimum number of moves.\n" +
                "- Sometimes you need to move robots to specific positions to clear paths for other robots.\n" +
                "- If you're stuck, use the Hint feature to get a suggestion.\n";
        
        // Set the text
        helpText.setText(helpContent);
    }
    
    @Override
    public String getScreenTitle() {
        return "Help";
    }
}
