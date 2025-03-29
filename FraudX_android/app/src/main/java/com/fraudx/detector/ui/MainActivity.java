package com.fraudx.detector.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.fraudx.detector.R;
import com.fraudx.detector.adapters.HeadlineAdapter;
import com.fraudx.detector.adapters.ScamAdapter;
import com.fraudx.detector.databinding.ActivityMainBinding;
import com.fraudx.detector.ui.auth.LoginActivity;
import com.fraudx.detector.utils.NotificationHelper;
import com.fraudx.detector.utils.NotificationWorker;
import com.fraudx.detector.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.fraudx.detector.adapters.NewsAdapter;
import com.fraudx.detector.models.News;
import com.fraudx.detector.models.Scam;
import com.fraudx.detector.services.GoogleSearchService;
import com.fraudx.detector.ui.detector.AIFakeNewsDetectorActivity;
import com.fraudx.detector.ui.detector.AIScamDetectorActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private static final int INITIAL_HEADLINES_COUNT = 5;
    private static final int INITIAL_SCAMS_COUNT = 5;
    private int currentHeadlinesCount = INITIAL_HEADLINES_COUNT;
    private int currentScamsCount = INITIAL_SCAMS_COUNT;
    private HeadlineAdapter headlineAdapter;
    private ScamAdapter scamAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private NewsAdapter newsAdapter;
    private GoogleSearchService searchService;
    private List<News> newsList = new ArrayList<>();
    private List<Scam> scamList = new ArrayList<>();
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private static final String PREF_NAME = "FraudXPrefs";
    private static final String PREF_NOTIFICATION_REQUESTED = "notification_requested";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        setupNavigationDrawer();
        setupViews();
        setupRecyclerViews();
        loadData();

        NotificationHelper.createNotificationChannel(this);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean hasRequestedNotification = prefs.getBoolean(PREF_NOTIFICATION_REQUESTED, false);

        if (!hasRequestedNotification) {
            requestNotificationPermission();
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("FraudX");
        }

        binding.navigationView.setNavigationItemSelectedListener(this);

        View headerView = binding.navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        ImageView ivProfileImage = headerView.findViewById(R.id.ivProfileImage);

        String fullName = sessionManager.getFirstName() + " " + sessionManager.getLastName();
        tvUserName.setText(fullName.trim());
        tvUserEmail.setText(sessionManager.getEmail());

        String profilePicUrl = sessionManager.getProfilePic();
        if (profilePicUrl != null && !profilePicUrl.isEmpty() && !profilePicUrl.equals("default_profile_pic_url")) {
            try {
                Glide.with(this)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivProfileImage);
            } catch (Exception e) {
            }
        }
    }

    private void setupViews() {
        binding.fakeNewsButton.setOnClickListener(v -> {
            animateCardClick(v, () -> {
                startActivity(new Intent(this, AIFakeNewsDetectorActivity.class));
            });
        });

        binding.showMoreHeadlines.setOnClickListener(v -> {
            startActivity(new Intent(this, TopHeadlinesActivity.class));
        });

        binding.showMoreScams.setOnClickListener(v -> {
            startActivity(new Intent(this, TopScamsActivity.class));
        });

        binding.scamDetectorButton.setOnClickListener(v -> {
            animateCardClick(v, () -> {
                startActivity(new Intent(this, AIScamDetectorActivity.class));
            });
        });
    }

    private void setupRecyclerViews() {
        LinearLayoutManager headlinesManager = new LinearLayoutManager(this);
        headlinesManager.setInitialPrefetchItemCount(4);
        binding.newsRecyclerView.setLayoutManager(headlinesManager);
        binding.newsRecyclerView.setHasFixedSize(true);
        binding.newsRecyclerView.setItemViewCacheSize(5);
        binding.newsRecyclerView.setNestedScrollingEnabled(false);

        RecyclerView.OnScrollListener smoothScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                } else {
                    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                }
            }
        };

        binding.newsRecyclerView.addOnScrollListener(smoothScrollListener);

        newsAdapter = new NewsAdapter(newsList, news -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(news.getUrl()));
            startActivity(intent);
        });
        binding.newsRecyclerView.setAdapter(newsAdapter);

        LinearLayoutManager scamsManager = new LinearLayoutManager(this);
        scamsManager.setInitialPrefetchItemCount(4);
        binding.scamsRecyclerView.setLayoutManager(scamsManager);
        binding.scamsRecyclerView.setHasFixedSize(true);
        binding.scamsRecyclerView.setItemViewCacheSize(5);
        binding.scamsRecyclerView.setNestedScrollingEnabled(false);
        binding.scamsRecyclerView.addOnScrollListener(smoothScrollListener);

        scamAdapter = new ScamAdapter(scamList, scam -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(scam.getUrl()));
            startActivity(intent);
        });
        binding.scamsRecyclerView.setAdapter(scamAdapter);

        setupRecyclerViewAnimations();
    }

    private void setupRecyclerViewAnimations() {
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(300);
        itemAnimator.setRemoveDuration(300);
        itemAnimator.setMoveDuration(300);
        itemAnimator.setChangeDuration(300);

        binding.newsRecyclerView.setItemAnimator(itemAnimator);
        binding.scamsRecyclerView.setItemAnimator(itemAnimator);
    }

    private void loadData() {
        searchService = new GoogleSearchService();
        loadInitialData();

        binding.showMoreHeadlines.setOnClickListener(v -> loadMoreHeadlines());
        binding.showMoreScams.setOnClickListener(v -> loadMoreScams());
    }

    private void loadInitialData() {
        searchService.searchNews("Latest breaking news", new GoogleSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<News> results) {
                runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        newsList.clear();
                        newsList.addAll(results.subList(0, Math.min(5, results.size())));
                        newsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error loading headlines: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        searchService.searchScams("recent online scams alerts", new GoogleSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<News> results) {
                runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        List<Scam> scams = new ArrayList<>();
                        for (News news : results) {
                            String description = news.getDescription();
                            if (description == null || description.isEmpty()) {
                                description = news.getSnippet();
                            }

                            String url = news.getUrl();
                            if (url == null || url.isEmpty()) {
                                url = news.getLink();
                            }

                            Scam scam = new Scam(
                                news.getTitle(),
                                description,
                                url,
                                "Online Scam",
                                news.getSource(),
                                determineRiskLevel(news.getTitle())
                            );
                            scams.add(scam);
                        }
                        scamList.clear();
                        scamList.addAll(scams.subList(0, Math.min(5, scams.size())));
                        scamAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error loading online scams: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMoreHeadlines() {
        searchService.searchNews("Latest breaking news", new GoogleSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<News> results) {
                runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        newsList.addAll(results.subList(0, Math.min(5, results.size())));
                        newsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error loading more headlines: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMoreScams() {
        searchService.searchScams("recent online scams alerts", new GoogleSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<News> results) {
                runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        List<Scam> scams = new ArrayList<>();
                        for (News news : results) {
                            String description = news.getDescription();
                            if (description == null || description.isEmpty()) {
                                description = news.getSnippet();
                            }

                            String url = news.getUrl();
                            if (url == null || url.isEmpty()) {
                                url = news.getLink();
                            }

                            Scam scam = new Scam(
                                news.getTitle(),
                                description,
                                url,
                                "Online Scam",
                                news.getSource(),
                                determineRiskLevel(news.getTitle())
                            );
                            scams.add(scam);
                        }
                        scamList.addAll(scams.subList(0, Math.min(5, scams.size())));
                        scamAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error loading online scams: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent intent = null;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_fake_news) {
            intent = new Intent(this, AIFakeNewsDetectorActivity.class);
        } else if (itemId == R.id.nav_scam_detector) {
            intent = new Intent(this, AIScamDetectorActivity.class);
        } else if (itemId == R.id.nav_account) {
            intent = new Intent(this, AccountDetailsActivity.class);
        } else if (itemId == R.id.nav_logout) {
            sessionManager.logout();
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (intent != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void animateCardClick(View view, Runnable action) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .alpha(0.9f)
            .setDuration(100)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        view.postDelayed(action, 50);
                    })
                    .start();
            })
            .start();
    }

    private String determineRiskLevel(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("scam") || lowerTitle.contains("fraud") ||
            lowerTitle.contains("hack") || lowerTitle.contains("stolen")) {
            return "High";
        } else if (lowerTitle.contains("warning") || lowerTitle.contains("alert") ||
                   lowerTitle.contains("beware") || lowerTitle.contains("risk")) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                onNotificationPermissionGranted();
            }
        } else {
            onNotificationPermissionGranted();
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_NOTIFICATION_REQUESTED, true).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onNotificationPermissionGranted();
            } else {
                Toast.makeText(this, "Notification permission denied. You won't receive important updates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onNotificationPermissionGranted() {
        NotificationWorker.scheduleNotifications(this);
        NotificationHelper.sendThankYouNotification(this);
        Toast.makeText(this, "Notifications enabled! You'll receive updates every 4 hours.", Toast.LENGTH_SHORT).show();
    }
}