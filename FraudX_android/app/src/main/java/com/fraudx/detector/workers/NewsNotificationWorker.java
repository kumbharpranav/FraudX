package com.fraudx.detector.workers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fraudx.detector.R;
import com.fraudx.detector.ui.MainActivity;
import com.fraudx.detector.utils.NotificationHelper;

import java.util.Random;

public class NewsNotificationWorker extends Worker {
    
    public NewsNotificationWorker(
        @NonNull Context context,
        @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        // Don't send notification if they're disabled
        if (!new NotificationHelper(context).areNotificationsEnabled()) {
            return Result.success();
        }
        
        // Get sample headlines from resources - we'll add this string array resource
        String[] newsItems = new String[] {
            "Breaking: New security vulnerability discovered in popular apps",
            "Tech giants announce collaborative effort to fight online fraud",
            "Report shows 30% increase in phishing attempts last month",
            "New AI tool helps identify fake news with 95% accuracy",
            "Government introduces new cybersecurity regulations for businesses",
            "Experts warn of new sophisticated email scam targeting professionals",
            "Study reveals most common passwords still used despite warnings",
            "Major data breach affects millions of users worldwide",
            "How to protect yourself from identity theft online",
            "New cryptocurrency scam steals millions from investors"
        };
        
        // Select a random headline
        Random random = new Random();
        String selectedHeadline = newsItems[random.nextInt(newsItems.length)];
        
        // Create intent to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Send notification
        NotificationHelper.sendNewsNotification(context, "Top Headline", selectedHeadline);
        
        return Result.success();
    }
} 