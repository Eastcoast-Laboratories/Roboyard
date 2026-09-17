package roboyard.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.achievements.AchievementSyncCallback
import roboyard.logic.managers.SyncManager
import roboyard.logic.network.RoboyardApiClient
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.ui.StringProvider

/**
 * Compose port of Android's LoginDialogHelper + RegisterDialogHelper.
 * On successful login the same follow-up syncs run: achievements from server,
 * corrected state back, then full save/history sync.
 */

/**
 * Show the login dialog. Mirrors Android: email/username + password fields,
 * a register link, and post-login sync.
 */
@Composable
fun LoginDialog(
    storage: PlatformStorage,
    stringProvider: StringProvider?,
    syncManager: SyncManager?,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }

    if (showRegister) {
        RegisterDialog(storage, stringProvider, syncManager, onDismiss, onLoginSuccess)
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringProvider?.getString("login_dialog_title") ?: "Login") },
        text = {
            Column {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringProvider?.getString("login_dialog_email_or_username")
                                ?: "Email or username"
                        )
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringProvider?.getString("login_dialog_password") ?: "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                ClickableText(
                    text = AnnotatedString(stringProvider?.getString("settings_register") ?: "Register"),
                    onClick = { showRegister = true }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    if (identifier.isBlank() || password.isEmpty()) {
                        error = "Please enter email and password"
                        return@Button
                    }
                    busy = true
                    val apiClient = RoboyardApiClient.getInstance(storage)
                    apiClient.login(identifier.trim(), password, object :
                        RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult?> {
                        override fun onNeedsUpdate() {
                            busy = false
                            error = stringProvider?.getString("needs_update_toast") ?: "needs_update"
                        }

                        override fun onSuccess(result: RoboyardApiClient.LoginResult?) {
                            busy = false
                            runPostLoginSync(storage, syncManager)
                            onLoginSuccess()
                            onDismiss()
                        }

                        override fun onError(e: String?) {
                            busy = false
                            error = stringProvider?.getString("settings_login_failed", e ?: "")
                                ?: "Login failed: $e"
                        }
                    })
                }
            ) {
                Text(stringProvider?.getString("settings_login") ?: "Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringProvider?.getString("button_cancel") ?: "Cancel")
            }
        }
    )
}

/**
 * Show the register dialog. Mirrors Android: name + email + password + confirm,
 * a login link, and post-login sync on success.
 */
@Composable
fun RegisterDialog(
    storage: PlatformStorage,
    stringProvider: StringProvider?,
    syncManager: SyncManager?,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    if (showLogin) {
        LoginDialog(storage, stringProvider, syncManager, onDismiss, onLoginSuccess)
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringProvider?.getString("settings_register") ?: "Register") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringProvider?.getString("register_dialog_name") ?: "Name") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringProvider?.getString("register_dialog_email") ?: "Email") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringProvider?.getString("login_dialog_password") ?: "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringProvider?.getString("register_dialog_confirm_password")
                                ?: "Confirm password"
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                ClickableText(
                    text = AnnotatedString(stringProvider?.getString("settings_login") ?: "Login"),
                    onClick = { showLogin = true }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isEmpty()) {
                        error = "Please fill in all fields"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        error = "Passwords do not match"
                        return@Button
                    }
                    busy = true
                    val apiClient = RoboyardApiClient.getInstance(storage)
                    apiClient.register(name.trim(), email.trim(), password, object :
                        RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult?> {
                        override fun onNeedsUpdate() {
                            busy = false
                            error = stringProvider?.getString("needs_update_toast") ?: "needs_update"
                        }

                        override fun onSuccess(result: RoboyardApiClient.LoginResult?) {
                            busy = false
                            runPostLoginSync(storage, syncManager)
                            onLoginSuccess()
                            onDismiss()
                        }

                        override fun onError(e: String?) {
                            busy = false
                            error = stringProvider?.getString("settings_register_failed", e ?: "")
                                ?: "Registration failed: $e"
                        }
                    })
                }
            ) {
                Text(stringProvider?.getString("settings_register") ?: "Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringProvider?.getString("button_cancel") ?: "Cancel")
            }
        }
    )
}

/**
 * Post-login sync, mirrors Android LoginDialogHelper.performLogin:
 * fetch achievements from server, upload corrected local state, then full
 * save/history sync.
 */
private fun runPostLoginSync(storage: PlatformStorage, syncManager: SyncManager?) {
    val achievementManager = AchievementManager.getInstance(storage)
    achievementManager.syncFromServer(object : AchievementSyncCallback {
        override fun onSuccess(syncedCount: Int, newAchievements: Int, latestAppVersion: String?) {
            // Upload corrected local state back to server (e.g. streak reset after long absence)
            achievementManager.syncToServer()
        }

        override fun onError(error: String?) {}
    })
    syncManager?.fullSyncOnLogin(null)
}
