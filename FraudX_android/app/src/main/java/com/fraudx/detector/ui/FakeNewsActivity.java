package com.fraudx.detector.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fraudx.detector.R;
import com.fraudx.detector.databinding.ActivityFakeNewsBinding;
import com.fraudx.detector.models.Message;
import com.fraudx.detector.models.News;
import com.fraudx.detector.adapters.MessageAdapter;
import com.fraudx.detector.utils.FirebaseHelper;
import com.fraudx.detector.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.fraudx.detector.services.GoogleSearchService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import com.fraudx.detector.ui.detector.AIScamDetectorActivity;

public class FakeNewsActivity extends BaseDetectorActivity {
    private ActivityFakeNewsBinding binding;
    private DrawerLayout drawerLayout;
    private FirebaseHelper firebaseHelper;
    private DatabaseReference userRef;
    private boolean isDarkMode = false;
    private SharedPreferences prefs;
    private static final String API_URL = "YOUR_API_ENDPOINT"; // Replace with actual API endpoint
    private final OkHttpClient client = new OkHttpClient();
    private GoogleSearchService googleSearchService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFakeNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupUI();
        setupListeners();
        googleSearchService = new GoogleSearchService();
    }

    @Override
    protected String getActivityTitle() {
        return "Fake News Detector";
    }

    @Override
    protected String getFirebaseChildPath() {
        return "fake_news";
    }

    @Override
    protected void setupFirebase() {
        firebaseHelper = FirebaseHelper.getInstance(this);
        userRef = firebaseHelper.getFakeNewsReference();
    }

    private void setupUI() {
        drawerLayout = findViewById(R.id.drawerLayout);
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("darkMode", false);
        applyTheme();

        binding.menuButton.setOnClickListener(v -> drawerLayout.open());
        binding.darkModeToggle.setOnClickListener(v -> toggleDarkMode());
    }

    private void setupListeners() {
        binding.messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.sendButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.sendButton.setOnClickListener(v -> sendMessage());

        binding.navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            switch (item.getItemId()) {
                case R.id.nav_home:
                    startActivity(new Intent(this, MainActivity.class));
                    break;
                case R.id.nav_scam_detector:
                    startActivity(new Intent(this, AIScamDetectorActivity.class));
                    break;
                case R.id.nav_profile:
                    startActivity(new Intent(this, ProfileActivity.class));
                    break;
                case R.id.nav_logout:
                    logout();
                    break;
            }
            return true;
        });
    }

    @Override
    protected void sendMessage() {
        String messageText = binding.messageInput.getText().toString().trim();
        if (messageText.isEmpty()) return;

        // Add user message
        Message userMessage = new Message(messageText, "unknown", false);
        addMessage(userMessage);

        // Clear input
        binding.messageInput.setText("");
        binding.sendButton.setVisibility(View.GONE);

        // Show loading
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        // Call API (replace with your actual API endpoint)
        processUserMessage(userMessage);
    }

    @Override
    protected void processUserMessage(Message message) {
        // Create bot response message
        Message botMessage = new Message("Analyzing your message...", "unknown", true);
        addMessage(botMessage);

        // First, search Google for related news
        googleSearchService.searchNews(message.getText(), new GoogleSearchService.SearchCallback() {
            @Override
            public void onSuccess(List<News> searchResults) {
                // Create JSON request body with user message and search results
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("text", message.getText());
                    jsonBody.put("searchResults", searchResults);
                    
                    RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), 
                        jsonBody.toString()
                    );

                    Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> {
                                botMessage.setText("Sorry, there was an error processing your message.");
                                botMessage.setStatus("unknown");
                                messageAdapter.notifyDataSetChanged();
                                showLoading(false);
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                String responseData = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseData);
                                
                                String prediction = jsonResponse.getString("prediction");
                                String explanation = jsonResponse.getString("explanation");
                                
                                runOnUiThread(() -> {
                                    botMessage.setText(explanation);
                                    botMessage.setStatus(prediction.toLowerCase());
                                    messageAdapter.notifyDataSetChanged();
                                    showLoading(false);
                                });

                                // Save bot message to Firebase
                                DatabaseReference newMessageRef = messagesRef.push();
                                newMessageRef.setValue(botMessage);
                                
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    botMessage.setText("Sorry, I couldn't understand the response.");
                                    botMessage.setStatus("unknown");
                                    messageAdapter.notifyDataSetChanged();
                                    showLoading(false);
                                });
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    botMessage.setText("Sorry, there was an error processing your message.");
                    botMessage.setStatus("unknown");
                    messageAdapter.notifyDataSetChanged();
                    showLoading(false);
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    botMessage.setText("Sorry, there was an error searching for related news.");
                    botMessage.setStatus("unknown");
                    messageAdapter.notifyDataSetChanged();
                    showLoading(false);
                });
            }
        });
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        prefs.edit().putBoolean("darkMode", isDarkMode).apply();
        applyTheme();
    }

    private void applyTheme() {
        if (isDarkMode) {
            binding.getRoot().setBackgroundResource(R.drawable.gradient_background_dark);
            // Apply other dark mode styles
        } else {
            binding.getRoot().setBackgroundResource(R.drawable.gradient_background);
            // Apply other light mode styles
        }
    }

    private void logout() {
        if (firebaseHelper != null) {
            firebaseHelper.signOut(new SessionManager(this));
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 