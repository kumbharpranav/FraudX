package com.fraudx.detector;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.fraudx.detector.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationHelper = new NotificationHelper(this);
        notificationHelper.requestNotificationPermission();
        notificationHelper.scheduleNotifications();

        // ... rest of existing onCreate code ...
    }
} 