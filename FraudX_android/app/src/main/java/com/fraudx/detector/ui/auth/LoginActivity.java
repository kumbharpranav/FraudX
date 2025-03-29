package com.fraudx.detector.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityLoginBinding;
import com.fraudx.detector.ui.MainActivity;
import com.fraudx.detector.utils.SessionManager;
import com.fraudx.detector.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Initialize Firebase Database and Session Manager
            databaseReference = FirebaseDatabase.getInstance("https://fraudx-4b017-default-rtdb.firebaseio.com").getReference();
            sessionManager = new SessionManager(this);

            // Check if user is already logged in
            if (sessionManager.isLoggedIn()) {
                Log.d(TAG, "User already logged in, redirecting to MainActivity");
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }

            // Setup click listeners
            setupClickListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showToast("Failed to initialize login. Please restart the app.");
        }
    }
    
    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("from_login", true);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailEditText.setError("Email is required");
            binding.emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.passwordEditText.setError("Password is required");
            binding.passwordEditText.requestFocus();
            return;
        }

        // Show loading state
        binding.loginButton.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.errorMessage.setVisibility(View.GONE);

        // Check user in database
        String userId = email.replace(".", "_").replace("@", "_");
        databaseReference.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    // Check password
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    if (storedPassword != null && storedPassword.equals(password)) {
                        // Create session and redirect
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        String profilePic = snapshot.child("profilePic").getValue(String.class);
                        
                        sessionManager.createSession(email, firstName != null ? firstName : "", 
                                lastName != null ? lastName : "", 
                                profilePic != null ? profilePic : "");
                        
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        binding.loginButton.setEnabled(true);
                        binding.errorMessage.setText("Invalid password");
                        binding.errorMessage.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.loginButton.setEnabled(true);
                    binding.errorMessage.setText("User not registered. Please sign up.");
                    binding.errorMessage.setVisibility(View.VISIBLE);
                    
                    // Redirect to register activity with toast message
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    intent.putExtra("from_login", true);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.loginButton.setEnabled(true);
                binding.errorMessage.setText("Login failed: " + error.getMessage());
                binding.errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showToast(String message) {
        Log.d(TAG, "Showing toast: " + message);
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
