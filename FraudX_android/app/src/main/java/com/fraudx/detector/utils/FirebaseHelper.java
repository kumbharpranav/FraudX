package com.fraudx.detector.utils;

import android.content.Context;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Map;
import java.util.HashMap;

public class FirebaseHelper {
    private static final String FIREBASE_URL = "https://fraudx-4b017-default-rtdb.firebaseio.com";
    private static final String TAG = "FirebaseHelper";
    
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final DatabaseReference dbRef;
    
    private static FirebaseHelper instance;
    private static boolean persistenceEnabled = false;
    
    private FirebaseHelper(Context context) {
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        
        auth = FirebaseAuth.getInstance();
        
        // Configure Firebase Database
        // Remove or comment out the following line:
        // FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        database = FirebaseDatabase.getInstance(FIREBASE_URL);
        database.getReference().keepSynced(true);
        dbRef = database.getReference();
    }
    
    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context);
        }
        return instance;
    }
    
    public FirebaseAuth getAuth() {
        return auth;
    }
    
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    public DatabaseReference getUserReference() {
        if (getCurrentUser() == null) return null;
        return database.getReference("users").child(getCurrentUser().getUid());
    }
    
    public DatabaseReference getMessagesReference() {
        if (getCurrentUser() == null) return null;
        return database.getReference("messages").child(getCurrentUser().getUid());
    }
    
    public DatabaseReference getScamDetectionReference() {
        if (getCurrentUser() == null) return null;
        return database.getReference("scam_detection_android").child(getCurrentUser().getUid());
    }
    
    public DatabaseReference getFakeNewsReference() {
        if (getCurrentUser() == null) return null;
        return database.getReference("fake_news_android").child(getCurrentUser().getUid());
    }
    
    public void signOut(SessionManager sessionManager) {
        auth.signOut();
        if (sessionManager != null) {
            sessionManager.clearSession();
        }
    }
    
    public void storeUserData(String userId, String email, String displayName) {
        try {
            DatabaseReference userRef = database.getReference("users").child(userId);
            userRef.child("email").setValue(email);
            userRef.child("displayName").setValue(displayName);
            userRef.child("createdAt").setValue(System.currentTimeMillis());
        } catch (Exception e) {
            Log.e("FirebaseHelper", "Error: ", e);
        }
    }
    
    public void storeMessage(String messageId, String text, String status) {
        try {
            if (getCurrentUser() == null) return;
            
            DatabaseReference messageRef = getMessagesReference().child(messageId);
            messageRef.child("text").setValue(text);
            messageRef.child("status").setValue(status);
            messageRef.child("timestamp").setValue(System.currentTimeMillis());
            messageRef.child("userId").setValue(getCurrentUser().getUid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteUserData(String userId, OnCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure("User ID is empty");
            return;
        }

        Map<String, Object> deleteUpdates = new HashMap<>();
        deleteUpdates.put("/users/" + userId, null);
        deleteUpdates.put("/fake_news/" + userId, null);
        deleteUpdates.put("/scam/" + userId, null);
        deleteUpdates.put("/scan_history/" + userId, null);
        deleteUpdates.put("/saved_articles/" + userId, null);
        deleteUpdates.put("/user_settings/" + userId, null);
        deleteUpdates.put("/user_reports/" + userId, null);
        deleteUpdates.put("/user_feedback/" + userId, null);

        dbRef.updateChildren(deleteUpdates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Successfully deleted all user data for: " + userId);
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting user data: " + e.getMessage());
                listener.onFailure(e.getMessage());
            });
    }

    public void updateUserProfile(String userId, Map<String, Object> updates, OnCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure("User ID is empty");
            return;
        }

        dbRef.child("users").child(userId)
            .updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Successfully updated profile for: " + userId);
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating profile: " + e.getMessage());
                listener.onFailure(e.getMessage());
            });
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}