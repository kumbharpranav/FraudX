package com.fraudx.detector.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.R;
import com.fraudx.detector.adapters.SearchResultAdapter;
import com.fraudx.detector.utils.GoogleSearchUtil;
import com.google.android.material.navigation.NavigationView;
import java.util.List;
import com.fraudx.detector.ui.detector.AIScamDetectorActivity;

public abstract class BaseSearchActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener {
    
    protected RecyclerView recyclerView;
    protected ProgressBar progressBar;
    protected SearchResultAdapter adapter;
    protected GoogleSearchUtil searchUtil;
    protected DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector_unified);

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        // Setup navigation
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize search
        searchUtil = new GoogleSearchUtil();
        setupRecyclerView();
        performSearch();
    }

    private void setupRecyclerView() {
        adapter = new SearchResultAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    protected abstract String getSearchQuery();

    private void performSearch() {
        progressBar.setVisibility(View.VISIBLE);
        searchUtil.search(getSearchQuery(), new GoogleSearchUtil.SearchCallback() {
            @Override
            public void onSuccess(List<GoogleSearchUtil.SearchResult> results) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (results.isEmpty()) {
                        Toast.makeText(BaseSearchActivity.this, 
                            "No results found", Toast.LENGTH_SHORT).show();
                    } else {
                        adapter.setResults(results);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BaseSearchActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        
        if (item.getItemId() == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
        } else if (item.getItemId() == R.id.nav_fake_news) {
            intent = new Intent(this, FakeNewsActivity.class);
        } else if (item.getItemId() == R.id.nav_scam) {
            intent = new Intent(this, AIScamDetectorActivity.class);
        } else if (item.getItemId() == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
        }

        if (intent != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
} 