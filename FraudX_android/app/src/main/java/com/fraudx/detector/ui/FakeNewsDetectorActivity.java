package com.fraudx.detector.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityAiFakeNewsDetectorBinding;

public class FakeNewsDetectorActivity extends AppCompatActivity {
    private ActivityAiFakeNewsDetectorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiFakeNewsDetectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
