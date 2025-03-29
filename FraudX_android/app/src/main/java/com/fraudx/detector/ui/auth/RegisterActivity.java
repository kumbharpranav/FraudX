package com.fraudx.detector.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityRegisterBinding;
import com.fraudx.detector.ui.MainActivity;
import com.fraudx.detector.utils.SessionManager;
import com.fraudx.detector.models.User; // Added import for User class
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private ActivityRegisterBinding binding;
    private DatabaseReference databaseReference;
    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase and SessionManager
        databaseReference = FirebaseDatabase.getInstance().getReference();
        sessionManager = new SessionManager(this);
        
        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Check if user was redirected from LoginActivity
        boolean fromLogin = getIntent().getBooleanExtra("from_login", false);
        if (fromLogin) {
            Toast.makeText(this, "Please register", Toast.LENGTH_SHORT).show();
        }
        
        // Request location permission
        requestLocationPermission();
        
        binding.registerButton.setOnClickListener(v -> registerUser());
    }
    
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        try {
                            updateLocationFields(location);
                        } catch (IOException e) {
                            Log.e(TAG, "Error getting address from location", e);
                        }
                    }
                });
    }
    
    private void updateLocationFields(Location location) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(
                location.getLatitude(), location.getLongitude(), 1);
        
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            binding.countryEditText.setText(address.getCountryName());
            binding.stateEditText.setText(address.getAdminArea());
            binding.cityEditText.setText(address.getLocality());
            binding.addressEditText.setText(address.getAddressLine(0));
        }
    }

    private void registerUser() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

        if (name.isEmpty()) {
            binding.nameEditText.setError("Name is required");
            binding.nameEditText.requestFocus();
            return;
        }

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

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords do not match");
            binding.confirmPasswordEditText.requestFocus();
            return;
        }

        // Show loading state
        binding.registerButton.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Create user with Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Firebase Authentication successful, now save additional user data
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    saveUserDataToDatabase(firebaseUser, name, password);
                } else {
                    // If Firebase Authentication fails, create user in our custom database
                    createUserInCustomDatabase(name, email, password);
                }
            });
    }
    
    private void saveUserDataToDatabase(FirebaseUser firebaseUser, String name, String password) {
        if (firebaseUser == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.registerButton.setEnabled(true);
            Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String email = firebaseUser.getEmail();
        String userId = email.replace(".", "_").replace("@", "_");
        
        // Create a User object with all necessary data
        User newUser = new User(name, email, password, "default_profile_pic_url");
        
        // Save the user data to the Realtime Database
        databaseReference.child("users").child(userId).setValue(newUser)
            .addOnCompleteListener(task -> {
                binding.progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Create session and redirect to main activity
                    sessionManager.createSession(email, name, "", "default_profile_pic_url");
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                } else {
                    binding.registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void createUserInCustomDatabase(String name, String email, String password) {
        // Create new user in the custom database
        String userId = email.replace(".", "_").replace("@", "_");
        User newUser = new User(name, email, password, "default_profile_pic_url");
        databaseReference.child("users").child(userId).setValue(newUser)
            .addOnCompleteListener(task -> {
                binding.progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    sessionManager.createSession(email, name, "", "default_profile_pic_url");
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                } else {
                    binding.registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
