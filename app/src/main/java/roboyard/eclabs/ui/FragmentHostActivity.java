package roboyard.eclabs.ui;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Activity that hosts the modern UI fragments for settings, save/load, and help screens.
 * This keeps the original game logic in MainActivity completely untouched while providing
 * a modern UI for the peripheral screens.
 */
public class FragmentHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_fragment_host);
        
        // Get the navigation controller
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            
            // Check which screen to show based on intent extras
            String screen = getIntent().getStringExtra("screen");
            if (screen != null) {
                switch (screen) {
                    case "settings":
                        navController.navigate(R.id.settingsFragment);
                        break;
                    case "save":
                        boolean saveMode = getIntent().getBooleanExtra("saveMode", true);
                        Bundle args = new Bundle();
                        args.putBoolean("saveMode", saveMode);
                        navController.navigate(R.id.saveGameFragment, args);
                        break;
                    case "help":
                        navController.navigate(R.id.helpFragment);
                        break;
                    // If no valid screen is specified, we stay at the default destination
                }
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    
    @Override
    public void onBackPressed() {
        // Use the FragmentManager to handle back navigation instead of NavController
        FragmentManager fragmentManager = getSupportFragmentManager();
        
        if (fragmentManager.getBackStackEntryCount() > 0) {
            // Pop the back stack if there are fragments in it
            fragmentManager.popBackStack();
            Timber.d("Popped fragment from back stack");
        } else {
            // If no fragments in back stack, finish the activity
            Timber.d("No fragments in back stack, finishing activity");
            finish();
        }
    }
}
