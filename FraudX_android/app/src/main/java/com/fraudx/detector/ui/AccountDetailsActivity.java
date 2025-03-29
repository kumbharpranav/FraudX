package com.fraudx.detector.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityAccountDetailsBinding;
import com.fraudx.detector.ui.auth.LoginActivity;
import com.fraudx.detector.utils.SessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.util.Log;
import com.fraudx.detector.utils.FirebaseHelper;

import java.util.Map;
import java.util.HashMap;

public class AccountDetailsActivity extends AppCompatActivity {
    private static final String TAG = "AccountDetailsActivity";
    private ActivityAccountDetailsBinding binding;
    private SessionManager sessionManager;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private String userId;
    private boolean processingFirebase = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "AccountDetailsActivity onCreate");
        sessionManager = new SessionManager(this);
        
        // First check if user is logged in via SessionManager
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in according to SessionManager");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Initialize Firebase only if session is valid
        try {
            Log.d(TAG, "Initializing Firebase");
            processingFirebase = true;
            firebaseAuth = FirebaseAuth.getInstance();
            currentUser = firebaseAuth.getCurrentUser();
            databaseReference = FirebaseDatabase.getInstance().getReference();
            
            if (currentUser != null) {
                userId = currentUser.getUid();
                Log.d(TAG, "Firebase user found with ID: " + userId);
            } else {
                Log.e(TAG, "Firebase user is null but SessionManager shows logged in");
                // Continue anyway, just display session data, don't redirect
                // We'll get user ID from session manager
                userId = sessionManager.getUserId();
                if (TextUtils.isEmpty(userId)) {
                    userId = sessionManager.getEmail().replace("@", "_").replace(".", "_");
                }
            }
            processingFirebase = false;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            // Continue anyway, just display session data
            userId = sessionManager.getUserId();
            if (TextUtils.isEmpty(userId)) {
                userId = sessionManager.getEmail().replace("@", "_").replace(".", "_");
            }
            processingFirebase = false;
        }

        if (userId != null) {
            // Load user data from Firebase
            loadUserData();
        }

        setupViews();
        setupClickListeners();
    }

    private void loadUserData() {
        databaseReference.child("users").child(userId).get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String firstName = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName = dataSnapshot.child("lastName").getValue(String.class);
                    String fullName = "";
                    
                    if (firstName != null) {
                        fullName = firstName;
                        if (lastName != null) {
                            fullName += " " + lastName;
                        }
                    } else {
                        // Fallback to name field if firstName/lastName not found
                        String name = dataSnapshot.child("name").getValue(String.class);
                        if (name != null) {
                            fullName = name;
                        }
                    }

                    if (!fullName.isEmpty()) {
                        binding.nameInput.setText(fullName);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user data: " + e.getMessage());
                showError("Failed to load user data: " + e.getMessage());
            });
    }

    private void setupViews() {
        try {
            // Disable editing for username and email
            binding.usernameInput.setEnabled(false);
            binding.emailInput.setEnabled(false);
            
            // Set user details from SessionManager since it's more reliable
            String email = sessionManager.getEmail();
            binding.usernameInput.setText(email != null ? email.split("@")[0] : "");
            binding.emailInput.setText(email);
            binding.nameInput.setText(sessionManager.getFirstName() + " " + sessionManager.getLastName());
        } catch (Exception e) {
            Log.e(TAG, "Error setting up views: " + e.getMessage());
            showError("Error displaying user information: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        try {
            binding.menuButton.setOnClickListener(v -> finish());
            binding.updateButton.setOnClickListener(v -> updateUserDetails());
            
            // Add delete button click listener
            binding.deleteAccountButton.setOnClickListener(v -> confirmDeleteAccount());
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage());
        }
    }
    
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called, finishing activity");
        finish();
    }
    
    private void updateUserDetails() {
        String name = binding.nameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();
        
        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            showError(getString(R.string.error_name_required));
            return;
        }
        
        // Check if passwords match if a new password is being set
        if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            showError(getString(R.string.error_passwords_dont_match));
            return;
        }
        
        // Update name in Realtime Database
        updateUserData(name, password);
    }
    
    private void updateUserData(String name, String password) {
        binding.updateButton.setEnabled(false);

        try {
            // Split name into first and last name
            String[] nameParts = name.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            // Create updates map for Firebase
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("firstName", firstName);
            updates.put("lastName", lastName);
            updates.put("updatedAt", System.currentTimeMillis());

            // Update Firebase and SessionManager
            databaseReference.child("users").child(userId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update SessionManager
                    sessionManager.createSession(
                        sessionManager.getEmail(),
                        firstName,
                        lastName,
                        sessionManager.getProfilePic()
                    );

                    Toast.makeText(AccountDetailsActivity.this, 
                        R.string.success_update, Toast.LENGTH_SHORT).show();
                    binding.updateButton.setEnabled(true);

                    // Update password if provided
                    if (!TextUtils.isEmpty(password)) {
                        updatePassword(password);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user data: " + e.getMessage());
                    showError("Failed to update profile: " + e.getMessage());
                    binding.updateButton.setEnabled(true);
                });

        } catch (Exception e) {
            Log.e(TAG, "Error in updateUserData: " + e.getMessage());
            showError("Error updating profile: " + e.getMessage());
            binding.updateButton.setEnabled(true);
        }
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            showError("User ID not found");
            return;
        }

        // Disable buttons while deleting
        binding.updateButton.setEnabled(false);
        binding.deleteAccountButton.setEnabled(false);

        FirebaseHelper.getInstance(this).deleteUserData(userId, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                sessionManager.clearSession();
                Toast.makeText(AccountDetailsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AccountDetailsActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }

            @Override
            public void onFailure(String error) {
                binding.updateButton.setEnabled(true);
                binding.deleteAccountButton.setEnabled(true);
                showError("Failed to delete account: " + error);
            }
        });
    }
    
    private void deleteUserFromAllTables() {
        // Delete user data from all tables that reference user data
        
        // Delete user's scan history
        databaseReference.child("scan_history").child(userId).removeValue();
        
        // Delete user's saved articles
        databaseReference.child("saved_articles").child(userId).removeValue();
        
        // Delete user's settings
        databaseReference.child("user_settings").child(userId).removeValue();
        
        // Delete user's reports
        databaseReference.child("user_reports").orderByChild("userId").equalTo(userId)
            .get().addOnSuccessListener(dataSnapshot -> {
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    String key = dataSnapshot.getChildren().iterator().next().getKey();
                    if (key != null) {
                        databaseReference.child("user_reports").child(key).removeValue();
                    }
                }
            });
        
        // Delete user's feedback
        databaseReference.child("user_feedback").orderByChild("userId").equalTo(userId)
            .get().addOnSuccessListener(dataSnapshot -> {
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    String key = dataSnapshot.getChildren().iterator().next().getKey();
                    if (key != null) {
                        databaseReference.child("user_feedback").child(key).removeValue();
                    }
                }
            });
    }
    
    private void showError(String message) {
        binding.errorMessage.setText(message);
        binding.errorMessage.setVisibility(View.VISIBLE);
    }
    
    private void redirectToLogin() {
        try {
            Log.d(TAG, "Redirecting to login screen");
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            // Clear back stack to prevent returning to this screen
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to login: " + e.getMessage());
        }
    }

    private void updatePassword(String newPassword) {
        if (TextUtils.isEmpty(newPassword)) return;

        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("password", newPassword);
            updates.put("passwordUpdatedAt", System.currentTimeMillis());

            databaseReference.child("users").child(userId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AccountDetailsActivity.this, 
                        "Password updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating password: " + e.getMessage());
                    showError("Failed to update password: " + e.getMessage());
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in updatePassword: " + e.getMessage());
            showError("Error updating password: " + e.getMessage());
        }
    }
}
