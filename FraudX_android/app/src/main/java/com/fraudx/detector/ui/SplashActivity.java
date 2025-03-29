package com.fraudx.detector.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.fraudx.detector.R;
import com.fraudx.detector.ui.auth.LoginActivity;
import com.fraudx.detector.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 2000; // 2 seconds
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        sessionManager = new SessionManager(this);
        Log.d(TAG, "onCreate: Session manager initialized");
        Log.d(TAG, "onCreate: User logged in status: " + sessionManager.isLoggedIn());

        new Handler().postDelayed(() -> {
            try {
                Intent intent;
                if (sessionManager.isLoggedIn()) {
                    Log.d(TAG, "Navigating to MainActivity - user is logged in");
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    Log.d(TAG, "Navigating to LoginActivity - user is not logged in");
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error during navigation: ", e);
                // Fallback to LoginActivity if there's an error
                Intent fallbackIntent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(fallbackIntent);
                finish();
            }
        }, SPLASH_DELAY);
    }
} 