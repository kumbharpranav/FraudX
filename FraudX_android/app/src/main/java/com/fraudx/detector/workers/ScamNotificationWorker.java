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

public class ScamNotificationWorker extends Worker {
    
    public ScamNotificationWorker(
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
        
        // Get sample scams from resources - we'll add this string array resource
        String[] scamItems = new String[] {
            "Beware of fake job offers asking for upfront payments",
            "New phone scam impersonating tax authorities reported",
            "Romance scams increased by 50% on dating apps",
            "Cryptocurrency investment scams targeting young adults",
            "Fake banking SMS messages asking for personal information",
            "Phishing emails claiming to be from streaming services",
            "Online shopping scams with fake discount offers",
            "Tech support scams targeting seniors on the rise",
            "Fake charity scams exploiting recent disasters",
            "Social media account hacking through fake giveaways"
        };
        
        // Select a random scam
        Random random = new Random();
        String selectedScam = scamItems[random.nextInt(scamItems.length)];
        
        // Create intent to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Send notification
        NotificationHelper.sendScamNotification(context, "Scam Alert", selectedScam);
        
        return Result.success();
    }
} 