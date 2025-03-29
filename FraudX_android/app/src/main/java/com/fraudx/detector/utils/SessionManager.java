package com.fraudx.detector.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "FraudXPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PROFILE_PIC = "profilePic";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    /**
     * Create login session
     */
    public void createLoginSession(String userId, String firstName, String lastName, String email, String profilePic) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PROFILE_PIC, profilePic);
        editor.apply();
    }
    
    /**
     * Backward compatibility method
     */
    public void createSession(String email, String firstName, String lastName, String profilePic) {
        String userId = email.replace("@", "_").replace(".", "_");
        createLoginSession(userId, firstName, lastName, email, profilePic);
    }
    
    /**
     * Get stored session data
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * Clear session details
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
    
    /**
     * Backward compatibility method
     */
    public void clearSession() {
        logout();
    }
    
    // Getters for user data
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }
    
    public String getFirstName() {
        return pref.getString(KEY_FIRST_NAME, "");
    }
    
    public String getLastName() {
        return pref.getString(KEY_LAST_NAME, "");
    }
    
    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }
    
    /**
     * Backward compatibility method
     */
    public String getUserEmail() {
        return getEmail();
    }
    
    public String getProfilePic() {
        return pref.getString(KEY_PROFILE_PIC, "default_profile_pic_url");
    }
    
    /**
     * Backward compatibility methods for setting user data
     */
    public void setUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }
    
    public void setUserEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
} 