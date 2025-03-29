package com.fraudx.detector.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fraudx.detector.adapters.HeadlineAdapter;
import com.fraudx.detector.databinding.ActivityTopHeadlinesBinding;
import com.fraudx.detector.services.HeadlinesService;
import com.fraudx.detector.models.Headline;
import java.util.ArrayList;
import java.util.List;

public class TopHeadlinesActivity extends AppCompatActivity {
    private ActivityTopHeadlinesBinding binding;
    private HeadlineAdapter adapter;
    private HeadlinesService headlinesService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopHeadlinesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        loadHeadlines();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Top Headlines");
    }

    private void setupRecyclerView() {
        adapter = new HeadlineAdapter(this, new ArrayList<>());
        binding.headlinesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.headlinesRecyclerView.setAdapter(adapter);
    }

    private void loadHeadlines() {
        headlinesService = new HeadlinesService();
        headlinesService.getTopHeadlines(new HeadlinesService.HeadlinesCallback() {
            @Override
            public void onSuccess(List<Headline> headlines) {
                runOnUiThread(() -> adapter.setHeadlines(headlines));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> showError(error));
            }
        });
    }

    private void showError(String error) {
        // Show error message
    }
}
