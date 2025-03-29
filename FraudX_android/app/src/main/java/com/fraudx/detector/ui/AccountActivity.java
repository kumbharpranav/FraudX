package com.fraudx.detector.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {
    private ActivityAccountBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_account);
        }

        // Create a userId by replacing non-alphanumeric characters
        userId = user.getEmail().replaceAll("[^a-zA-Z0-9]", "_");
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Load user data
        loadUserData();

        // Set up update button
        binding.btnUpdate.setOnClickListener(v -> updateUserDetails());
    }

    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String name = dataSnapshot.child("name").getValue(String.class);

                    binding.etUsername.setText(username);
                    binding.etEmail.setText(email);
                    binding.etName.setText(name);
                } else {
                    Toast.makeText(AccountActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AccountActivity.this, R.string.error_update, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserDetails() {
        String name = binding.etName.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_name_required));
            return;
        }

        // Validate passwords if provided
        if (!TextUtils.isEmpty(password) || !TextUtils.isEmpty(confirmPassword)) {
            if (!password.equals(confirmPassword)) {
                binding.tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
                return;
            }
        }

        // Update user data
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update password if provided
                    if (!TextUtils.isEmpty(password)) {
                        mAuth.getCurrentUser().updatePassword(password)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(AccountActivity.this, R.string.success_update, Toast.LENGTH_SHORT).show();
                                    binding.etPassword.setText("");
                                    binding.etConfirmPassword.setText("");
                                })
                                .addOnFailureListener(e -> Toast.makeText(AccountActivity.this, R.string.error_update, Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(AccountActivity.this, R.string.success_update, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AccountActivity.this, R.string.error_update, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 