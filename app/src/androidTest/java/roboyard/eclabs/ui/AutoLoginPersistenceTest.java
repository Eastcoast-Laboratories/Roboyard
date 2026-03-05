package roboyard.eclabs.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import roboyard.ui.activities.MainFragmentActivity;
import roboyard.ui.components.RoboyardApiClient;
import timber.log.Timber;

import static org.junit.Assert.*;

/**
 * Test auto-login token persistence across app restarts.
 * This test verifies that a user who logs in remains logged in after the app is closed and reopened.
 */
@RunWith(AndroidJUnit4.class)
public class AutoLoginPersistenceTest {
    
    private Context context;
    private String testEmail;
    private String testPassword;
    private String testName;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Generate unique test credentials
        String randomSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        testEmail = "autotest" + randomSuffix + "@example.com";
        testPassword = "TestPass123!";
        testName = "AutoTest" + randomSuffix;
        
        // Clear any existing auth data
        clearAuthData();
        
        Timber.d("[AUTO_LOGIN_TEST] Test setup complete: email=%s", testEmail);
    }
    
    @After
    public void tearDown() {
        // Clean up test user data
        clearAuthData();
    }
    
    private void clearAuthData() {
        SharedPreferences prefs = context.getSharedPreferences("roboyard_api", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Timber.d("[AUTO_LOGIN_TEST] Cleared auth data");
    }
    
    @Test
    public void testAutoLoginPersistsAcrossAppRestarts() throws InterruptedException {
        RoboyardApiClient apiClient = RoboyardApiClient.getInstance(context);
        
        // Step 1: Verify not logged in initially
        assertFalse("[AUTO_LOGIN_TEST] Should not be logged in initially", apiClient.isLoggedIn());
        Timber.d("[AUTO_LOGIN_TEST] ✓ Step 1: Verified not logged in initially");
        
        // Step 2: Register new test user
        final boolean[] registerSuccess = {false};
        final String[] registerError = {null};
        
        apiClient.register(testName, testEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                registerSuccess[0] = true;
                Timber.d("[AUTO_LOGIN_TEST] Registration successful: token=%s", result.token.substring(0, 10) + "...");
            }
            
            @Override
            public void onError(String error) {
                registerError[0] = error;
                Timber.e("[AUTO_LOGIN_TEST] Registration failed: %s", error);
            }
        });
        
        // Wait for registration
        Thread.sleep(3000);
        
        if (registerError[0] != null) {
            Timber.e("[AUTO_LOGIN_TEST] Registration error: %s", registerError[0]);
        }
        assertTrue("[AUTO_LOGIN_TEST] Registration should succeed", registerSuccess[0]);
        Timber.d("[AUTO_LOGIN_TEST] ✓ Step 2: Registration successful");
        
        // Step 3: Verify logged in after registration
        assertTrue("[AUTO_LOGIN_TEST] Should be logged in after registration", apiClient.isLoggedIn());
        String tokenAfterRegister = apiClient.getAuthToken();
        assertNotNull("[AUTO_LOGIN_TEST] Token should exist after registration", tokenAfterRegister);
        Timber.d("[AUTO_LOGIN_TEST] ✓ Step 3: Logged in after registration, token=%s", tokenAfterRegister.substring(0, 10) + "...");
        
        // Step 4: Simulate app restart by creating new ApiClient instance
        Timber.d("[AUTO_LOGIN_TEST] ===== SIMULATING APP RESTART =====");
        
        // Force a new instance by clearing the singleton (reflection)
        try {
            java.lang.reflect.Field instanceField = RoboyardApiClient.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
            Timber.d("[AUTO_LOGIN_TEST] Cleared RoboyardApiClient singleton");
        } catch (Exception e) {
            Timber.e(e, "[AUTO_LOGIN_TEST] Failed to clear singleton");
            fail("Failed to clear singleton: " + e.getMessage());
        }
        
        // Get new instance (simulates app restart)
        RoboyardApiClient apiClientAfterRestart = RoboyardApiClient.getInstance(context);
        
        // Step 5: Verify still logged in after "restart"
        boolean isLoggedInAfterRestart = apiClientAfterRestart.isLoggedIn();
        String tokenAfterRestart = apiClientAfterRestart.getAuthToken();
        
        Timber.d("[AUTO_LOGIN_TEST] After restart: isLoggedIn=%s, token=%s", 
                isLoggedInAfterRestart, 
                tokenAfterRestart != null ? tokenAfterRestart.substring(0, 10) + "..." : "null");
        
        assertTrue("[AUTO_LOGIN_TEST] Should still be logged in after app restart", isLoggedInAfterRestart);
        assertNotNull("[AUTO_LOGIN_TEST] Token should still exist after restart", tokenAfterRestart);
        assertEquals("[AUTO_LOGIN_TEST] Token should be the same after restart", tokenAfterRegister, tokenAfterRestart);
        
        Timber.d("[AUTO_LOGIN_TEST] ✓ Step 5: Still logged in after restart");
        
        // Step 6: Verify token with server
        final boolean[] verifySuccess = {false};
        final String[] verifyError = {null};
        
        apiClientAfterRestart.verifyToken(new RoboyardApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isValid) {
                verifySuccess[0] = isValid;
                Timber.d("[AUTO_LOGIN_TEST] Token verification result: %s", isValid);
            }
            
            @Override
            public void onError(String error) {
                verifyError[0] = error;
                Timber.e("[AUTO_LOGIN_TEST] Token verification error: %s", error);
            }
        });
        
        // Wait for verification
        Thread.sleep(3000);
        
        if (verifyError[0] != null) {
            Timber.e("[AUTO_LOGIN_TEST] Verification error: %s", verifyError[0]);
        }
        assertTrue("[AUTO_LOGIN_TEST] Token should be valid on server", verifySuccess[0]);
        Timber.d("[AUTO_LOGIN_TEST] ✓ Step 6: Token verified with server");
        
        Timber.d("[AUTO_LOGIN_TEST] ===== TEST PASSED: AUTO-LOGIN PERSISTS ACROSS RESTARTS =====");
    }
}
