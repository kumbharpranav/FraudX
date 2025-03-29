package com.fraudx.detector.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fraudx.detector.adapters.ScamAdapter;
import com.fraudx.detector.databinding.ActivityTopScamsBinding;
import com.fraudx.detector.models.Scam;
import com.fraudx.detector.services.ScamDetectorService;
import java.util.ArrayList;
import java.util.List;

public class TopScamsActivity extends AppCompatActivity {
    private ActivityTopScamsBinding binding;
    private ScamAdapter adapter;
    private ScamDetectorService scamService;
    private List<Scam> scamList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopScamsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        loadRecentScams();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Recent Scams");
    }

    private void setupRecyclerView() {
        scamList = new ArrayList<>();
        adapter = new ScamAdapter(scamList, scam -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(scam.getUrl()));
            startActivity(intent);
        });
        binding.scamsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.scamsRecyclerView.setAdapter(adapter);
    }

    private void loadRecentScams() {
        scamService = new ScamDetectorService();
        // Load scams from Firebase or your backend
    }
}