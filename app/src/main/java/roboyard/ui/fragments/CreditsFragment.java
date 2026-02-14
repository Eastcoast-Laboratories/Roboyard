package roboyard.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import roboyard.eclabs.R;
import timber.log.Timber;

/**
 * Credits screen implemented as a Fragment with modern Android UI components.
 */
public class CreditsFragment extends BaseGameFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credits, container, false);
        
        // Set up version information
        String versionName = getVersionName(requireContext());
        int versionCode = getVersionCode(requireContext());
        
        TextView versionTextView = view.findViewById(R.id.version_text);
        versionTextView.setText(getString(R.string.version_format, versionName, versionCode));
        
        // Set up clickable links
        setupClickableLink(view, R.id.imprint_link, getString(R.string.url_imprint));
        setupClickableLink(view, R.id.opensource_link, getString(R.string.url_opensource));
        setupClickableLink(view, R.id.contact_link, getString(R.string.url_contact));
        
        // Programmatically disable accessibility for wall elements
        disableAccessibilityForDecorations(view);
        
        // Set up back button
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            Timber.d("CreditsFragment: Back button clicked");
            getParentFragmentManager().popBackStack();
        });
        
        return view;
    }
    
    /**
     * Sets up a TextView as a clickable link
     * @param view The parent view
     * @param textViewId The ID of the TextView
     * @param url The URL to open when clicked
     */
    private void setupClickableLink(View view, int textViewId, final String url) {
        TextView textView = view.findViewById(textViewId);
        String displayText = textView.getText().toString(); // Get the text from the XML layout
        SpannableString spannableString = new SpannableString(displayText);
        
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                openLink(url);
            }
            
            @Override
            public void updateDrawState(@NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(0xFF0000FF); // Set link color to blue (with full alpha)
                ds.setUnderlineText(true);
            }
        };
        
        spannableString.setSpan(clickableSpan, 0, displayText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
    
    /**
     * Opens a URL in the default browser
     * @param url The URL to open
     */
    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
    
    /**
     * Disables accessibility for decorative elements
     * @param view The parent view
     */
    private void disableAccessibilityForDecorations(View view) {
        // Get references to all wall views
        int[] wallIds = {R.id.top_wall, R.id.left_wall, R.id.right_wall, R.id.bottom_wall};
        
        for (int id : wallIds) {
            View wallView = view.findViewById(id);
            if (wallView != null) {
                // Basic approach using only core Android APIs
                wallView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                wallView.setClickable(false);
                wallView.setFocusable(false);
                wallView.setContentDescription(null);
                
                // Log for diagnostics
                Timber.d("Disabled accessibility for wall element: %s", getResources().getResourceEntryName(id));
            }
        }
    }
    
    /**
     * Get the app version code
     * @param context The context
     * @return The version code
     */
    private int getVersionCode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Error getting version code");
            return -1;
        }
    }
    
    /**
     * Get the app version name
     * @param context The context
     * @return The version name
     */
    private String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Error getting version name");
            return "";
        }
    }

    /**
     * Get the screen title for accessibility and UI
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Credits";
    }
}
