package com.fraudx.detector.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.fraudx.detector.R;
import com.fraudx.detector.ui.MainActivity;
import com.fraudx.detector.workers.NewsNotificationWorker;
import com.fraudx.detector.workers.ScamNotificationWorker;

import java.util.concurrent.TimeUnit;

public class NotificationHelper {
    private static final String CHANNEL_ID = "FraudXChannel";
    private static final String CHANNEL_NAME = "FraudX Notifications";
    private static final String CHANNEL_DESC = "Notifications for FraudX app";
    private static final int THANK_YOU_NOTIFICATION_ID = 1001;
    private static final int NEWS_NOTIFICATION_ID = 1002;
    private static final int SCAM_NOTIFICATION_ID = 1003;
    
    private Context context;
    
    public NotificationHelper(Context context) {
        this.context = context;
    }
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    public void requestNotificationPermission() {
        // This is handled in MainActivity directly since it requires Activity context
    }
    
    public void scheduleNotifications() {
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
    }
    
    public static void sendThankYouNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Thank You!")
            .setContentText("Thanks for enabling notifications. You'll receive important updates about scams and news.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
            
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(THANK_YOU_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Handle permission not granted
        }
    }
    
    public static void sendNewsNotification(Context context, String title, String content) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
            
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NEWS_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Handle permission not granted
        }
    }
    
    public static void sendScamNotification(Context context, String title, String content) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
            
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(SCAM_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Handle permission not granted
        }
    }
    
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
} 