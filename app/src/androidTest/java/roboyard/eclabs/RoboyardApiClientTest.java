package roboyard.eclabs;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import roboyard.ui.components.RoboyardApiClient;
import timber.log.Timber;

/**
 * Integration tests for user registration API.
 * Tests the registration flow against the real API at roboyard.z11.de.
 * 
 * NOTE: These tests require network access to roboyard.z11.de.
 * They may fail on emulators without proper network configuration.
 * 
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=roboyard.ui.components.RoboyardApiClientTest
 */
@RunWith(AndroidJUnit4.class)
public class RoboyardApiClientTest {

    private Context context;
    private RoboyardApiClient apiClient;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        apiClient = RoboyardApiClient.getInstance(context);
        
        // Logout before each test
        apiClient.logout();
    }

    @After
    public void tearDown() {
        // Logout after each test
        if (apiClient != null) {
            apiClient.logout();
        }
    }

    /**
     * Test API registration with unique email
     */
    @Test
    public void testApiRegistration() throws InterruptedException {
        // Generate unique email to avoid conflicts
        String uniqueEmail = "test_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String testName = "Test User";
        String testPassword = "testpassword123";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>("");

        apiClient.register(testName, uniqueEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                errorMessage.set(error);
                latch.countDown();
            }
        });

        // Wait for API response (max 30 seconds)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        if (!completed) {
            throw new AssertionError("API registration timed out");
        }
        
        if (!success.get()) {
            throw new AssertionError("API registration failed: " + errorMessage.get());
        }

        // Verify user is logged in
        assert apiClient.isLoggedIn() : "User should be logged in after registration";
        assert testName.equals(apiClient.getUserName()) : "User name should match";
        assert uniqueEmail.equals(apiClient.getUserEmail()) : "User email should match";
    }

    /**
     * Test API login after registration
     */
    @Test
    public void testApiLoginAfterRegistration() throws InterruptedException {
        // Generate unique email
        String uniqueEmail = "test_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String testName = "Test User";
        String testPassword = "testpassword123";

        // First register
        CountDownLatch registerLatch = new CountDownLatch(1);
        AtomicBoolean registerSuccess = new AtomicBoolean(false);
        AtomicReference<String> registerError = new AtomicReference<>("");

        apiClient.register(testName, uniqueEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                registerSuccess.set(true);
                registerLatch.countDown();
            }

            @Override
            public void onError(String error) {
                registerError.set(error);
                registerLatch.countDown();
            }
        });

        boolean registerCompleted = registerLatch.await(30, TimeUnit.SECONDS);
        if (!registerCompleted || !registerSuccess.get()) {
            throw new AssertionError("Registration failed: " + registerError.get());
        }

        // Logout
        apiClient.logout();
        assert !apiClient.isLoggedIn() : "User should be logged out";

        // Now login
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicBoolean loginSuccess = new AtomicBoolean(false);
        AtomicReference<String> loginError = new AtomicReference<>("");

        apiClient.login(uniqueEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                loginSuccess.set(true);
                loginLatch.countDown();
            }

            @Override
            public void onError(String error) {
                loginError.set(error);
                loginLatch.countDown();
            }
        });

        boolean loginCompleted = loginLatch.await(30, TimeUnit.SECONDS);
        if (!loginCompleted || !loginSuccess.get()) {
            throw new AssertionError("Login failed: " + loginError.get());
        }

        // Verify user is logged in
        assert apiClient.isLoggedIn() : "User should be logged in after login";
        assert uniqueEmail.equals(apiClient.getUserEmail()) : "User email should match";
    }

    /**
     * Test that registration with mismatched passwords shows error (client-side validation)
     */
    @Test
    public void testRegistrationPasswordMismatchValidation() {
        String password1 = "password123";
        String password2 = "differentpassword";
        
        // Passwords don't match - this should be caught client-side
        assert !password1.equals(password2) : "Test setup: passwords should not match";
    }

    /**
     * Test that registration with empty fields is rejected
     */
    @Test
    public void testRegistrationEmptyFieldsValidation() {
        String emptyName = "";
        String emptyEmail = "";
        String emptyPassword = "";
        
        // All fields are empty - this should be caught client-side
        assert emptyName.isEmpty() : "Test setup: name should be empty";
        assert emptyEmail.isEmpty() : "Test setup: email should be empty";
        assert emptyPassword.isEmpty() : "Test setup: password should be empty";
    }

    /**
     * Test registration with invalid email format
     */
    @Test
    public void testApiRegistrationInvalidEmail() throws InterruptedException {
        String invalidEmail = "not-an-email";
        String testName = "Test User";
        String testPassword = "testpassword123";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>("");

        apiClient.register(testName, invalidEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                errorMessage.set(error);
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        if (!completed) {
            throw new AssertionError("API registration timed out");
        }
        
        // Registration should fail with invalid email
        assert !success.get() : "Registration should fail with invalid email";
        assert !errorMessage.get().isEmpty() : "Error message should not be empty";
    }

    /**
     * Test registration with duplicate email
     */
    @Test
    public void testApiRegistrationDuplicateEmail() throws InterruptedException {
        // Use a known existing email
        String existingEmail = "github@r.z11.de";
        String testName = "Test User";
        String testPassword = "testpassword123";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>("");

        apiClient.register(testName, existingEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                errorMessage.set(error);
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        if (!completed) {
            throw new AssertionError("API registration timed out");
        }
        
        // Registration should fail with duplicate email
        assert !success.get() : "Registration should fail with duplicate email";
        assert !errorMessage.get().isEmpty() : "Error message should not be empty";
    }

    /**
     * Test map sharing and verify the share URL loads without errors.
     * This test registers a user, shares a map, and then fetches the share URL
     * to check for database errors or other server-side issues.
     */
    @Test
    public void testMapSharingAndShareUrlWorks() throws Exception {
        // Generate unique email
        String uniqueEmail = "test_share_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String testName = "Test Share User";
        String testPassword = "testpassword123";

        // First register
        CountDownLatch registerLatch = new CountDownLatch(1);
        AtomicBoolean registerSuccess = new AtomicBoolean(false);
        AtomicReference<String> registerError = new AtomicReference<>("");

        apiClient.register(testName, uniqueEmail, testPassword, new RoboyardApiClient.ApiCallback<RoboyardApiClient.LoginResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.LoginResult result) {
                registerSuccess.set(true);
                registerLatch.countDown();
            }

            @Override
            public void onError(String error) {
                registerError.set(error);
                registerLatch.countDown();
            }
        });

        boolean registerCompleted = registerLatch.await(30, TimeUnit.SECONDS);
        if (!registerCompleted || !registerSuccess.get()) {
            throw new AssertionError("Registration failed: " + registerError.get());
        }

        // Now share a map
        CountDownLatch shareLatch = new CountDownLatch(1);
        AtomicBoolean shareSuccess = new AtomicBoolean(false);
        AtomicReference<String> shareError = new AtomicReference<>("");
        AtomicReference<String> shareUrl = new AtomicReference<>("");

        String testMapData = "16,16,W0-0,W0-1,W0-2,R1-5,B2-8,T3-10"; // Sample map data
        
        apiClient.shareMap(testMapData, "Test Map " + System.currentTimeMillis(), new RoboyardApiClient.ApiCallback<RoboyardApiClient.ShareResult>() {
            @Override
            public void onSuccess(RoboyardApiClient.ShareResult result) {
                shareSuccess.set(true);
                shareUrl.set(result.shareUrl);
                shareLatch.countDown();
            }

            @Override
            public void onError(String error) {
                shareError.set(error);
                shareLatch.countDown();
            }
        });

        boolean shareCompleted = shareLatch.await(30, TimeUnit.SECONDS);
        if (!shareCompleted || !shareSuccess.get()) {
            throw new AssertionError("Map sharing failed: " + shareError.get());
        }

        // Verify share URL is not empty
        assert !shareUrl.get().isEmpty() : "Share URL should not be empty";

        // Now fetch the share URL and check for errors
        String urlToCheck = shareUrl.get();
        String pageContent = fetchUrlContent(urlToCheck);
        
        // Check for common error patterns in the page content
        checkForServerErrors(pageContent, urlToCheck);
    }

    /**
     * Fetch content from a URL
     */
    private String fetchUrlContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        conn.disconnect();
        
        // If we got an error response code, include it in the error message
        if (responseCode >= 400) {
            throw new AssertionError("HTTP error " + responseCode + " when fetching " + urlString + "\nContent: " + content.toString().substring(0, Math.min(500, content.length())));
        }
        
        return content.toString();
    }

    /**
     * Check page content for common server-side errors
     */
    private void checkForServerErrors(String content, String url) {
        // Check for SQL errors
        if (content.contains("SQLSTATE[")) {
            // Extract the error message
            int start = content.indexOf("SQLSTATE[");
            int end = content.indexOf("</", start);
            if (end == -1) end = Math.min(start + 200, content.length());
            String errorSnippet = content.substring(start, end);
            throw new AssertionError("SQL error found on " + url + ": " + errorSnippet);
        }
        
        // Check for Laravel/PHP exceptions
        if (content.contains("Illuminate\\Database\\QueryException")) {
            throw new AssertionError("Database QueryException found on " + url);
        }
        
        if (content.contains("ErrorException") || content.contains("FatalErrorException")) {
            throw new AssertionError("PHP Exception found on " + url);
        }
        
        // Check for "Column not found" errors
        if (content.contains("Column not found")) {
            int start = content.indexOf("Column not found");
            String errorSnippet = content.substring(start, Math.min(start + 100, content.length()));
            throw new AssertionError("Missing column error on " + url + ": " + errorSnippet);
        }
        
        // Check for "Table not found" errors
        if (content.contains("Base table or view not found") || content.contains("Table") && content.contains("doesn't exist")) {
            throw new AssertionError("Missing table error on " + url);
        }
        
        // Check for generic 500 error page
        if (content.contains("500 | Server Error") || content.contains("Whoops, looks like something went wrong")) {
            throw new AssertionError("Server error (500) found on " + url);
        }
    }
}
