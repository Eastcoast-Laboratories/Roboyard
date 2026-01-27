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
 * Helper class for showing register dialog.
 * Provides a reusable register dialog that can be used from any fragment or activity.
 */
public class RegisterDialogHelper {
    
    public interface RegisterCallback {
        void onRegisterSuccess(RoboyardApiClient.LoginResult result);
        void onRegisterError(String error);
    }
    
    // Cached registration form data (persists between dialog opens)
    private static String cachedRegisterName = "";
    private static String cachedRegisterEmail = "";
    private static String cachedRegisterPassword = "";
    
    /**
     * Show register dialog
     */
    public static void showRegisterDialog(Context context, RegisterCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.register_dialog_title);
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        
        EditText nameInput = new EditText(context);
        nameInput.setHint(R.string.register_dialog_name);
        nameInput.setText(cachedRegisterName);
        nameInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setSingleLine(true);
        layout.addView(nameInput);
        
        EditText emailInput = new EditText(context);
        emailInput.setHint(R.string.login_dialog_email);
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(cachedRegisterEmail);
        layout.addView(emailInput);
        
        EditText passwordInput = new EditText(context);
        passwordInput.setHint(R.string.login_dialog_password);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setText(cachedRegisterPassword);
        layout.addView(passwordInput);
        
        EditText confirmPasswordInput = new EditText(context);
        confirmPasswordInput.setHint(R.string.register_dialog_confirm_password);
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);
        
        // Add login link
        TextView loginLink = new TextView(context);
        loginLink.setText(R.string.settings_login);
        loginLink.setTextColor(0xFF000000);
        loginLink.setPadding(16, 16, 0, 0);
        loginLink.setClickable(true);
        layout.addView(loginLink);
        
        builder.setView(layout);
        builder.setPositiveButton(R.string.settings_register, null);
        builder.setNegativeButton(R.string.button_cancel, (dialogInterface, which) -> {
            // Save form data when canceling
            cachedRegisterName = nameInput.getText().toString().trim();
            cachedRegisterEmail = emailInput.getText().toString().trim();
            cachedRegisterPassword = passwordInput.getText().toString();
        });
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> performRegister(context, nameInput, emailInput, passwordInput, confirmPasswordInput, callback, dialog));
            
            // Set up Enter key handling for confirm password field to trigger register
            confirmPasswordInput.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    performRegister(context, nameInput, emailInput, passwordInput, confirmPasswordInput, callback, dialog);
                    return true;
                }
                return false;
            });
            
            // Login link click handler
            loginLink.setOnClickListener(v -> {
                dialog.dismiss();
                LoginDialogHelper.showLoginDialog(context, new LoginDialogHelper.LoginCallback() {
                    @Override
                    public void onLoginSuccess(RoboyardApiClient.LoginResult result) {
                        if (callback != null) {
                            callback.onRegisterSuccess(result);
                        }
                    }
                    
                    @Override
                    public void onLoginError(String error) {
                        if (callback != null) {
                            callback.onRegisterError(error);
                        }
                    }
                });
            });
        });
        
        dialog.show();
    }
    
    private static void performRegister(Context context, EditText nameInput, EditText emailInput, EditText passwordInput, EditText confirmPasswordInput, RegisterCallback callback, AlertDialog dialog) {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        
        // Save form data
        cachedRegisterName = name;
        cachedRegisterEmail = email;
        cachedRegisterPassword = password;
        
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        RoboyardApiClient.getInstance(context).register(name, email, password, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                // Clear cached data on success
                cachedRegisterName = "";
                cachedRegisterEmail = "";
                cachedRegisterPassword = "";
                
                Toast.makeText(context, R.string.settings_register_success, Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onRegisterSuccess(result);
                }
                dialog.dismiss();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, context.getString(R.string.settings_register_failed, error), Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onRegisterError(error);
                }
            }
        });
    }
}
