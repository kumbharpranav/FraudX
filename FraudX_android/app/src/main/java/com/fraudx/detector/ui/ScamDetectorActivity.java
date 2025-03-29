package com.fraudx.detector.ui;
import android.graphics.PorterDuff;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fraudx.detector.R;
import com.fraudx.detector.adapters.ScamAdapter;
import com.fraudx.detector.databinding.ActivityAiScamDetectorBinding;
import com.fraudx.detector.models.Scam;
import com.fraudx.detector.services.ScamDetectorService;
import com.fraudx.detector.ui.auth.LoginActivity;
import com.fraudx.detector.utils.SessionManager;
import com.fraudx.detector.utils.VerticalSpaceItemDecoration;
import com.fraudx.detector.views.TypingIndicatorView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.WindowManager;
import android.net.Uri;
import java.net.URLEncoder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;
import com.google.firebase.database.ServerValue;

public class ScamDetectorActivity extends AppCompatActivity {
    private ActivityAiScamDetectorBinding binding;
    private ScamDetectorService scamDetectorService;
    private DatabaseReference userRef;
    private DatabaseReference messagesRef;
    private SessionManager sessionManager;
    private ScamAdapter scamAdapter;
    private List<Scam> scamList;
    private boolean isAnalyzing = false;
    private TypingIndicatorView typingIndicator;
    private String userId;
    private ValueEventListener messagesListener;
    private static final String API_URL = "https://www.fakedetector.run.place/service2/gemini";
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable hardware acceleration using the window's decorator
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        
        super.onCreate(savedInstanceState);
        binding = ActivityAiScamDetectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize session manager
        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        typingIndicator = binding.typingIndicator;
        
        setupToolbar();
        setupViews();
        setupFirebase();
    }

    private void setupFirebase() {
        try {
            // Get user email and create userId
            String userEmail = sessionManager.getUserEmail(); // Ensure getUserEmail method exists
            userId = sessionManager.getUserId();
            
            if (userId == null && userEmail != null) {
                // Create a userId by replacing non-alphanumeric characters
                userId = userEmail.replace("[^a-zA-Z0-9]", "_");
                sessionManager.setUserId(userId); // Ensure setUserId method exists
            }
            
            if (userId != null) {
                // Store data under "scam/" to separate from fake news data
                userRef = FirebaseDatabase.getInstance().getReference()
                        .child("scam")
                        .child(userId);
                
                messagesRef = userRef.child("messages");
                
                // Load previous messages
                loadPreviousMessages();
            } else {
                Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            }

            // Initialize service
            scamDetectorService = new ScamDetectorService();

            // Add realtime listener for messages
            if (messagesRef != null) {
                messagesListener = messagesRef.orderByChild("timestamp")
                    .limitToLast(50)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            List<Scam> tempList = new ArrayList<>();
                            for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                try {
                                    String key = messageSnapshot.getKey();
                                    String text = messageSnapshot.child("text").getValue(String.class);
                                    Boolean isBot = messageSnapshot.child("isBot").getValue(Boolean.class);
                                    String status = messageSnapshot.child("status").getValue(String.class);
                                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                                    
                                    if (text != null && isBot != null) {
                                        Scam item = new Scam(text, status != null ? status : "", "", "", isBot ? "Bot" : "User", "");
                                        item.setId(key);
                                        item.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                                        tempList.add(item);
                                    }
                                } catch (Exception e) {
                                    continue;
                                }
                            }
                            
                            Collections.sort(tempList, (item1, item2) -> 
                                Long.compare(item1.getTimestamp(), item2.getTimestamp()));
                            
                            scamList.clear();
                            scamList.addAll(tempList);
                            scamAdapter.notifyDataSetChanged();
                            
                            if (!scamList.isEmpty()) {
                                binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ScamDetectorActivity.this, 
                                "Error loading messages: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadPreviousMessages() {
        if (messagesRef == null) return;
        
        // Clear existing messages
        scamList.clear();
        
        // Order by timestamp and limit to 50 messages
        Query recentMessagesQuery = messagesRef.orderByChild("timestamp").limitToLast(50);
        
        recentMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Scam> tempList = new ArrayList<>();
                
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    try {
                        String key = messageSnapshot.getKey();
                        String text = messageSnapshot.child("text").getValue(String.class);
                        Boolean isBot = messageSnapshot.child("isBot").getValue(Boolean.class);
                        String status = messageSnapshot.child("status").getValue(String.class);
                        Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                        
                        if (text != null && isBot != null) {
                            Scam item = new Scam(text, status != null ? status : "", "", "", isBot ? "Bot" : "User", "");
                            item.setId(key);
                            item.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                            tempList.add(item);
                        }
                    } catch (Exception e) {
                        Toast.makeText(ScamDetectorActivity.this, "Error loading message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                
                // Sort messages by timestamp
                Collections.sort(tempList, (item1, item2) -> 
                    Long.compare(item1.getTimestamp(), item2.getTimestamp()));
                
                // Add to our display list
                scamList.addAll(tempList);
                scamAdapter.notifyDataSetChanged();
                
                // Scroll to the latest message
                if (!scamList.isEmpty()) {
                    binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ScamDetectorActivity.this, "Failed to load messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Scam Detector");
        }
    }

    private void setupViews() {
        // Setup RecyclerView with layout manager that stacks from end
        scamList = new ArrayList<>();
        scamAdapter = new ScamAdapter(scamList, scam -> {
            if (scam.getUrl() != null && !scam.getUrl().isEmpty()) {
                try {
                    Uri uri = Uri.parse(scam.getUrl());
                    if (uri.getScheme() == null) {
                        uri = Uri.parse("https://" + scam.getUrl());
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No app found to handle this URL", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Optimize RecyclerView
        binding.recyclerViewScams.setHasFixedSize(true);
        binding.recyclerViewScams.setItemViewCacheSize(20);
        binding.recyclerViewScams.setDrawingCacheEnabled(true);
        binding.recyclerViewScams.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        binding.recyclerViewScams.setNestedScrollingEnabled(false);
        binding.recyclerViewScams.setVerticalScrollBarEnabled(false);
        
        // Use hardware acceleration for better performance
        binding.recyclerViewScams.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false; // Disable predictive animations
            }
        };
        layoutManager.setStackFromEnd(true);
        binding.recyclerViewScams.setLayoutManager(layoutManager);
        
        // Reduce RecyclerView rebind operations
        binding.recyclerViewScams.setAdapter(scamAdapter);
        
        // Add vertical spacing between messages (20dp for more visible gaps)
        int spacingInPixels = (int) (20 * getResources().getDisplayMetrics().density);
        binding.recyclerViewScams.addItemDecoration(new VerticalSpaceItemDecoration(spacingInPixels));

        // Setup send button with validation
        binding.sendButton.setOnClickListener(v -> {
            String text = binding.messageInput.getText().toString().trim();
            if (validateInput(text)) {
                analyzeMessage(text);
                binding.messageInput.setText("");
            }
        });
    }

    private boolean validateInput(String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if it's a URL
        if (android.util.Patterns.WEB_URL.matcher(text).matches()) {
            return true;
        }

        // Check for minimum word count
        String[] words = text.split("\\s+");
        if (words.length < 2) {
            Toast.makeText(this, "Please enter a complete message (at least 2 words)", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void analyzeMessage(String messageText) {
        if (scamDetectorService == null) {
            Toast.makeText(this, "Service not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAnalyzing) {
            Toast.makeText(this, "Please wait for the current analysis to complete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message to the chat
        Scam userMessage = new Scam(messageText, "", "", "", "User", "");
        userMessage.setTimestamp(System.currentTimeMillis());
        scamList.add(userMessage);
        scamAdapter.notifyItemInserted(scamList.size() - 1);
        binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
        
        // Save user message to Firebase with the correct structure
        if (messagesRef != null) {
            String key = messagesRef.push().getKey();
            if (key != null) {
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("text", messageText);
                messageData.put("isBot", false);
                messageData.put("status", "");
                messageData.put("timestamp", System.currentTimeMillis());
                
                userMessage.setId(key);
                messagesRef.child(key).setValue(messageData);
            }
        }

        isAnalyzing = true;
        
        // Show typing indicator animation
        binding.typingIndicator.setVisibility(View.VISIBLE);
        binding.typingIndicator.startAnimation();

        try {
            // Build the URL with query parameter
            String encodedMessage = URLEncoder.encode(messageText, "UTF-8");
            String url = API_URL + "?messageText=" + encodedMessage;
            
            // Make the API request
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        // Hide typing indicator
                        binding.typingIndicator.stopAnimation();
                        binding.typingIndicator.setVisibility(View.GONE);
                        
                        // Add error message to the chat
                        Scam errorMessage = new Scam("Error: " + e.getMessage(), "ERROR", "", "", "System", "");
                        errorMessage.setTimestamp(System.currentTimeMillis());
                        scamList.add(errorMessage);
                        scamAdapter.notifyItemInserted(scamList.size() - 1);
                        binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                        
                        // Save error message to Firebase
                        if (messagesRef != null) {
                            String key = messagesRef.push().getKey();
                            if (key != null) {
                                Map<String, Object> messageData = new HashMap<>();
                                messageData.put("text", "Error: " + e.getMessage());
                                messageData.put("isBot", true);
                                messageData.put("status", "error");
                                messageData.put("timestamp", System.currentTimeMillis());
                                
                                errorMessage.setId(key);
                                messagesRef.child(key).setValue(messageData);
                            }
                        }
                        
                        Toast.makeText(ScamDetectorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isAnalyzing = false;
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
                        
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        
                        // Extract result and process
                        String output = jsonResponse.getString("result").trim();
                        String[] words = output.split("\\s+");
                        String firstWord = words[0].toLowerCase();
                        String remainingText = output.substring(output.indexOf(' ')+1);
                        
                        // Set status based on first word
                        String status = "unknown";
                        if (firstWord.equals("scam") || firstWord.equals("scam.")) {
                            status = "scam";
                        } else if (firstWord.equals("safe")) {
                            status = "safe";
                        }
                        
                        final String finalStatus = status;
                        final String finalText = remainingText;
                        
                        runOnUiThread(() -> {
                            // Hide typing indicator
                            binding.typingIndicator.stopAnimation();
                            binding.typingIndicator.setVisibility(View.GONE);
                            
                            // Add bot response to the chat
                            Scam botMessage = new Scam(finalText, finalStatus, "", "", "Bot", "");
                            botMessage.setTimestamp(System.currentTimeMillis());
                            scamList.add(botMessage);
                            scamAdapter.notifyItemInserted(scamList.size() - 1);
                            binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                            
                            // Save bot message to Firebase with the correct structure
                            if (messagesRef != null) {
                                String key = messagesRef.push().getKey();
                                if (key != null) {
                                    Map<String, Object> messageData = new HashMap<>();
                                    messageData.put("text", finalText);
                                    messageData.put("status", finalStatus);
                                    messageData.put("isBot", true);
                                    messageData.put("timestamp", System.currentTimeMillis());
                                    
                                    botMessage.setId(key);
                                    messagesRef.child(key).setValue(messageData);
                                }
                            }
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            // Hide typing indicator
                            binding.typingIndicator.stopAnimation();
                            binding.typingIndicator.setVisibility(View.GONE);
                            
                            // Add error message to the chat
                            Scam errorMessage = new Scam("Error: " + e.getMessage(), "ERROR", "", "", "System", "");
                            errorMessage.setTimestamp(System.currentTimeMillis());
                            scamList.add(errorMessage);
                            scamAdapter.notifyItemInserted(scamList.size() - 1);
                            binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                            
                            // Save error message to Firebase
                            if (messagesRef != null) {
                                String key = messagesRef.push().getKey();
                                if (key != null) {
                                    Map<String, Object> messageData = new HashMap<>();
                                    messageData.put("text", "Error: " + e.getMessage());
                                    messageData.put("isBot", true);
                                    messageData.put("status", "error");
                                    messageData.put("timestamp", System.currentTimeMillis());
                                    
                                    errorMessage.setId(key);
                                    messagesRef.child(key).setValue(messageData);
                                }
                            }
                            
                            Toast.makeText(ScamDetectorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            isAnalyzing = false;
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                // Hide typing indicator
                binding.typingIndicator.stopAnimation();
                binding.typingIndicator.setVisibility(View.GONE);
                
                // Add error message to the chat
                Scam errorMessage = new Scam("Error: " + e.getMessage(), "ERROR", "", "", "System", "");
                errorMessage.setTimestamp(System.currentTimeMillis());
                scamList.add(errorMessage);
                scamAdapter.notifyItemInserted(scamList.size() - 1);
                binding.recyclerViewScams.smoothScrollToPosition(scamList.size() - 1);
                
                // Save error message to Firebase
                if (messagesRef != null) {
                    String key = messagesRef.push().getKey();
                    if (key != null) {
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("text", "Error: " + e.getMessage());
                        messageData.put("isBot", true);
                        messageData.put("status", "error");
                        messageData.put("timestamp", System.currentTimeMillis());
                        
                        errorMessage.setId(key);
                        messagesRef.child(key).setValue(messageData);
                    }
                }
                
                Toast.makeText(ScamDetectorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isAnalyzing = false;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        binding = null;
    }
}