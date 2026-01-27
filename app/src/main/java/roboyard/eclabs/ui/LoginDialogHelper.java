package roboyard.eclabs.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import roboyard.eclabs.R;
import roboyard.eclabs.RoboyardApiClient;
import timber.log.Timber;

/**
 * Helper class for showing login dialog.
 * Provides a reusable login dialog that can be used from any fragment or activity.
 */
public class LoginDialogHelper {
    
    public interface LoginCallback {
        void onLoginSuccess(RoboyardApiClient.LoginResult result);
        void onLoginError(String error);
    }
    
    /**
     * Show login dialog
     */
    public static void showLoginDialog(Context context, LoginCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.login_dialog_title);
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        
        EditText emailInput = new EditText(context);
        emailInput.setHint(R.string.login_dialog_email);
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);
        
        EditText passwordInput = new EditText(context);
        passwordInput.setHint(R.string.login_dialog_password);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton(R.string.settings_login, (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            RoboyardApiClient.getInstance(context).login(email, password, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
                @Override
                public void onSuccess(RoboyardApiClient.LoginResult result) {
                    Toast.makeText(context, R.string.settings_login_success, Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onLoginSuccess(result);
                    }
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(context, context.getString(R.string.settings_login_failed, error), Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onLoginError(error);
                    }
                }
            });
        });
        
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.show();
    }
}
