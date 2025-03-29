package com.fraudx.detector.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Data;
import android.util.Log;

import com.fraudx.detector.data.DataManager;
import com.fraudx.detector.workers.NewsNotificationWorker;
import com.fraudx.detector.workers.ScamNotificationWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";
    private static final String WORK_NAME = "FraudXNotificationWork";
    private static final String PREF_NAME = "FraudXPrefs";
    private static final String PREF_LAST_NOTIFICATION_TYPE = "last_notification_type";
    private static final String TYPE_HEADLINE = "headline";
    private static final String TYPE_SCAM = "scam";
    
    // Input data keys
    private static final String KEY_HEADLINES = "headlines";
    private static final String KEY_SCAMS = "scams";
    
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        // Skip if notifications are disabled
        if (!new NotificationHelper(context).areNotificationsEnabled()) {
            return Result.success();
        }
        
        try {
            // Choose randomly between sending a headline or scam notification
            Random random = new Random();
            boolean sendHeadline = random.nextBoolean();
            
            if (sendHeadline) {
                // Get sample headlines
                String[] headlines = {
                    "Breaking: New security vulnerability discovered in popular apps",
                    "Tech giants announce collaborative effort to fight online fraud",
                    "Report shows 30% increase in phishing attempts last month",
                    "New AI tool helps identify fake news with 95% accuracy",
                    "Government introduces new cybersecurity regulations for businesses"
                };
                
                // Select a random headline
                String selectedHeadline = headlines[random.nextInt(headlines.length)];
                
                // Send headline notification with updated method signature
                NotificationHelper.sendNewsNotification(context, "Top Headline", selectedHeadline);
            } else {
                // Get sample scams
                String[] scams = {
                    "Beware of fake job offers asking for upfront payments",
                    "New phone scam impersonating tax authorities reported",
                    "Romance scams increased by 50% on dating apps",
                    "Cryptocurrency investment scams targeting young adults",
                    "Fake banking SMS messages asking for personal information"
                };
                
                // Select a random scam
                String selectedScam = scams[random.nextInt(scams.length)];
                
                // Send scam notification with updated method signature
                NotificationHelper.sendScamNotification(context, "Scam Alert", selectedScam);
            }
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
            return Result.failure();
        }
    }
    
    /**
     * Schedule periodic notifications for both headline news and scam alerts
     */
    public static void scheduleNotifications(Context context) {
        // We're now using dedicated worker classes from the workers package
        // Schedule news notifications every 4 hours
        PeriodicWorkRequest newsRequest = new PeriodicWorkRequest.Builder(
            NewsNotificationWorker.class, 
            4, 
            TimeUnit.HOURS
        ).build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "newsNotifications",
            ExistingPeriodicWorkPolicy.REPLACE,
            newsRequest
        );
        
        // Schedule scam notifications every 4 hours offset by 2 hours
        PeriodicWorkRequest scamRequest = new PeriodicWorkRequest.Builder(
            ScamNotificationWorker.class, 
            4, 
            TimeUnit.HOURS
        )
        .setInitialDelay(2, TimeUnit.HOURS)
        .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "scamNotifications",
            ExistingPeriodicWorkPolicy.REPLACE,
            scamRequest
        );
        
        Log.d(TAG, "Scheduled periodic notifications every 4 hours");
    }
    
    // Cancel scheduled notifications
    public static void cancelNotifications(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
} 