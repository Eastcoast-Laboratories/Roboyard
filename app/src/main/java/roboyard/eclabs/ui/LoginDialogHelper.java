package roboyard.eclabs.ui;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
        
        // Add register link
        TextView registerLink = new TextView(context);
        registerLink.setText(R.string.settings_register);
        registerLink.setTextColor(0xFF0066CC);
        registerLink.setPadding(0, 16, 0, 0);
        registerLink.setClickable(true);
        layout.addView(registerLink);
        
        builder.setView(layout);
        
        builder.setPositiveButton(R.string.settings_login, null);
        builder.setNegativeButton(R.string.button_cancel, null);
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> performLogin(context, emailInput, passwordInput, callback, dialog));
            
            // Set up Enter key handling for password field to trigger login
            passwordInput.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    performLogin(context, emailInput, passwordInput, callback, dialog);
                    return true;
                }
                return false;
            });
            
            // Register link click handler
            registerLink.setOnClickListener(v -> {
                dialog.dismiss();
                RegisterDialogHelper.showRegisterDialog(context, new RegisterDialogHelper.RegisterCallback() {
                    @Override
                    public void onRegisterSuccess(RoboyardApiClient.LoginResult result) {
                        if (callback != null) {
                            callback.onLoginSuccess(result);
                        }
                    }
                    
                    @Override
                    public void onRegisterError(String error) {
                        if (callback != null) {
                            callback.onLoginError(error);
                        }
                    }
                });
            });
        });
        
        dialog.show();
    }
    
    private static void performLogin(Context context, EditText emailInput, EditText passwordInput, LoginCallback callback, AlertDialog dialog) {
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
                dialog.dismiss();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, context.getString(R.string.settings_login_failed, error), Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onLoginError(error);
                }
            }
        });
    }
}
