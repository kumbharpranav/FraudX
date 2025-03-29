package com.fraudx.detector.ui;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityLoginBinding;
import com.fraudx.detector.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private WebAppInterface webAppInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        webAppInterface = new WebAppInterface();

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize other UI components
        // ...rest of existing code...
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onLoginSuccess(String email, String firstName, String lastName, String profilePic) {
            sessionManager.createSession(email, firstName != null ? firstName : "", lastName != null ? lastName : "", profilePic != null ? profilePic : "");
            runOnUiThread(() -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        }

        @JavascriptInterface
        public void onLoginError(String error) {
            runOnUiThread(() -> {
                Toast.makeText(LoginActivity.this, "Login Error: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
