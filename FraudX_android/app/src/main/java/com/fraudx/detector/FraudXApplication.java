package com.fraudx.detector;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import androidx.multidex.MultiDex;
import com.google.firebase.FirebaseApp;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;

public class FraudXApplication extends Application {
    private static final String TAG = "FraudXApplication";
    private static FraudXApplication instance;
    private static boolean isMIUI;

    @Override
    public void onCreate() {
        // Enable strict mode for debug builds
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        }

        super.onCreate();
        instance = this;
        
        // Check if running on MIUI
        isMIUI = checkIsMIUI();
        if (isMIUI) {
            setupMIUI();
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase initialized with default config");

        // Enable Firebase persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Additional initialization if needed
        initializeApp();
    }

    private boolean checkIsMIUI() {
        String manufacturer = Build.MANUFACTURER;
        return manufacturer != null && manufacturer.toLowerCase().contains("xiaomi");
    }

    private void setupMIUI() {
        try {
            // Allow MIUI optimization
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            }
            
            // Log MIUI version
            String miuiVersion = getSystemProperty("ro.miui.ui.version.name");
            Log.d(TAG, "Running on MIUI version: " + miuiVersion);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up MIUI specific configurations", e);
        }
    }

    private String getSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            return (String) systemProperties.getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            Log.e(TAG, "Error getting system property: " + key, e);
            return null;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static FraudXApplication getInstance() {
        return instance;
    }

    public static boolean isMIUI() {
        return isMIUI;
    }

    private void initializeApp() {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception", throwable);
            throwable.printStackTrace();
        });
    }
} 