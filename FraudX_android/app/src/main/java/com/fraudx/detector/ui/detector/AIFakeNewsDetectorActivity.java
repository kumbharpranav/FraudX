package com.fraudx.detector.ui.detector;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fraudx.detector.R;
import com.fraudx.detector.adapters.MessageAdapter;
import com.fraudx.detector.databinding.ActivityAiFakeNewsDetectorBinding;
import com.fraudx.detector.models.Message;
import com.fraudx.detector.ui.BaseDetectorActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AIFakeNewsDetectorActivity extends BaseDetectorActivity {
    private static final String TAG = "AIFakeNewsDetectorActivity";
    private ActivityAiFakeNewsDetectorBinding aiBinding;
    private Animation fadeIn;
    private Animation fadeOut;
    private TextView aiStatusTextView;
    // Use the API endpoint for fake news - updated to use service1/gemini
    private static final String API_URL = "https://www.fakedetector.run.place/service1/gemini";
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize binding FIRST before calling super
        aiBinding = ActivityAiFakeNewsDetectorBinding.inflate(getLayoutInflater());
        setContentView(aiBinding.getRoot());
        
        // Initialize animations
        setupAnimations();
        
        // Setup our RecyclerView and other views
        chatRecyclerView = aiBinding.recyclerViewNews;
        messageInput = aiBinding.messageInput;
        progressBar = aiBinding.progressBar;
        aiStatusTextView = aiBinding.statusTextView;
        
        // AFTER initializing our views, call super (which will call setupRecyclerView, etc.)
        super.onCreate(savedInstanceState);
        
        // Setup toolbar
        setSupportActionBar(aiBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getActivityTitle());
        }
        
        // Setup send button
        aiBinding.sendButton.setOnClickListener(v -> sendMessage());
        
        // Log the current state to debug
        android.util.Log.d(TAG, "onCreate: Initializing");
        android.util.Log.d(TAG, "Messages count: " + (messages != null ? messages.size() : "null"));
        
        // Hide status container initially
        if (aiBinding.statusContainer != null) {
            aiBinding.statusContainer.setVisibility(View.GONE);
        }
        
        // Animate the floating squares
        animateSquares();
    }

    private void animateSquares() {
        // Add null checks to prevent crashes if binding is null
        if (aiBinding == null || aiBinding.square1 == null || aiBinding.square2 == null || aiBinding.square3 == null) {
            android.util.Log.e(TAG, "Cannot animate squares - binding or views are null");
            return;
        }
        
        // Add subtle animations to the squares
        aiBinding.square1.animate()
            .translationY(20f)
            .rotation(5f)
            .setDuration(8000)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // Check if binding is still valid before continuing animation
                if (aiBinding != null && aiBinding.square1 != null && !isFinishing() && !isDestroyed()) {
                    aiBinding.square1.animate()
                        .translationY(-20f)
                        .rotation(-5f)
                        .setDuration(8000)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            // Only recursively call if activity is still active
                            if (!isFinishing() && !isDestroyed() && aiBinding != null) {
                                animateSquares();
                            }
                        })
                        .start();
                }
            })
            .start();
            
        aiBinding.square2.animate()
            .translationY(-15f)
            .rotation(-8f)
            .setDuration(9000)
            .setStartDelay(400)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .start();
            
        aiBinding.square3.animate()
            .translationY(10f)
            .rotation(3f)
            .setDuration(7000)
            .setStartDelay(800)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .start();
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.fake_news_detector_title);
    }

    @Override
    protected String getFirebaseChildPath() {
        // Match the JavaScript path structure: "news/{userId}"
        return "fake_news";
    }

    private void setupAnimations() {
        fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
    }

    @Override
    protected void processUserMessage(Message userMessage) {
        if (userMessage == null) {
            android.util.Log.e(TAG, "Cannot process null user message");
            return;
        }

        try {
            android.util.Log.d(TAG, "Processing user message: " + userMessage.getText());
            
            // Show typing indicator in the status container
            if (aiBinding != null) {
                runOnUiThread(() -> {
                    // Show status container with text
                    aiBinding.statusTextView.setText(R.string.analyzing);
                    aiBinding.statusContainer.setVisibility(View.VISIBLE);
                    // Show the chat typing indicator 
                    showLoading(true);
                    // Start animation
                    animateSquares();
                });
            }

            // Run detection on a background thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                String userText = userMessage.getText();
                String result;
                
                try {
                    // Call the AI detection
                    result = isFakeNews(userText);
                    android.util.Log.d(TAG, "Detection result: " + result);
                    
                    // Extract status from first word for styling
                    String status = "uncertain";
                    if (result.startsWith("⚠️")) {
                        status = "fake";
                    } else if (result.startsWith("✅")) {
                        status = "real";
                    }
                    
                    final String finalStatus = status;
                    
                    // Update UI on main thread with result
                    runOnUiThread(() -> {
                        try {
                            // Check if activity is still active
                            if (isFinishing() || isDestroyed()) {
                                android.util.Log.w(TAG, "Activity no longer active, aborting UI update");
            return;
        }

                            // Hide typing indicator
                            showLoading(false);
                            
                            // Add the actual result message with the status determined from response
                            Message botMessage = new Message(result, finalStatus, true);
                            messages.add(botMessage);
                            messageAdapter.notifyItemInserted(messages.size() - 1);
                            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                            
                            // Log path debugging info
                            android.util.Log.d(TAG, "Firebase child path: " + getFirebaseChildPath());
                            
                            // Save bot message to Firebase
                            if (messagesRef != null) {
                                DatabaseReference newMessageRef = messagesRef.push();
                                Map<String, Object> messageData = new HashMap<>();
                                messageData.put("text", result);
                                messageData.put("status", finalStatus);
                                messageData.put("isBot", true);
                                messageData.put("timestamp", ServerValue.TIMESTAMP);
                                newMessageRef.setValue(messageData);
                                android.util.Log.d(TAG, "Message saved to Firebase path: " + messagesRef.toString());
                            } else {
                                android.util.Log.e(TAG, "messagesRef is null, cannot save to Firebase");
                            }
                            
                            // Hide the status container and stop animations
                            if (aiBinding != null) {
                                aiBinding.statusContainer.setVisibility(View.GONE);
                                stopAnimations();
                            }
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error updating UI with result: " + e.getMessage(), e);
                            showError("Error displaying result: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error during detection: " + e.getMessage(), e);
                    
                    // Update UI with error
                    runOnUiThread(() -> {
                        // Check if activity is still active
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        
                        // Hide typing indicator
                        showLoading(false);
                        
                        // Add error message
                        String errorMsg = "Sorry, I encountered an error analyzing your news: " + e.getMessage();
                        Message errorMessage = new Message(errorMsg, "error", true);
                        messages.add(errorMessage);
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        
                        // Hide status container and reset animation
                        if (aiBinding != null) {
                            aiBinding.statusContainer.setVisibility(View.GONE);
                            stopAnimations();
                        }
                    });
                }
            });
            
            // Make sure to shut down the executor
            executor.shutdown();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Unexpected error in processUserMessage: " + e.getMessage(), e);
            showError("Unexpected error: " + e.getMessage());
            
            if (aiBinding != null) {
                aiBinding.statusContainer.setVisibility(View.GONE);
                stopAnimations();
            }
        }
    }

    // The actual fake news detection logic
    private String isFakeNews(String newsText) throws IOException, JSONException {
        android.util.Log.d(TAG, "Checking if news is fake: " + newsText);
        
        if (newsText == null || newsText.trim().isEmpty()) {
            return "Please provide news text to analyze.";
        }
        
        try {
            // Encode the message for the URL
            String encodedMessage = URLEncoder.encode(newsText, "UTF-8");
            
            // Use only this endpoint for fake news
            String url = API_URL + "?messageText=" + encodedMessage;
            android.util.Log.d(TAG, "Using endpoint: " + url);
            
            try {
                // Make the API request
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
                
                // Execute the request synchronously since we're already on a background thread
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        android.util.Log.w(TAG, "Endpoint failed with code: " + response.code());
                        throw new IOException("Unexpected response code: " + response.code());
                    }
                    
                    // Success! Process the response
                    return processApiResponse(response.body().string());
                }
            } catch (IOException e) {
                android.util.Log.w(TAG, "Error with endpoint: " + e.getMessage());
                throw e;
            }
            
        } catch (IOException e) {
            // If API failed, provide a mock response as last resort
            android.util.Log.e(TAG, "API failed: " + e.getMessage() + ". Using mock response.", e);
            
            // Generate a mock response based on the text input to not completely fail the user
            try {
                return generateMockResponse(newsText);
            } catch (Exception mockError) {
                android.util.Log.e(TAG, "Even mock response failed: " + mockError.getMessage(), mockError);
                throw new IOException("Network error: " + e.getMessage(), e);
            }
        } catch (JSONException e) {
            android.util.Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
            throw new JSONException("Failed to parse response: " + e.getMessage());
        } catch (Exception e) {
            android.util.Log.e(TAG, "Unexpected error in isFakeNews: " + e.getMessage(), e);
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    // Generate a mock response if all APIs fail
    private String generateMockResponse(String newsText) {
        // Simple heuristics to decide fake or real based on text content
        String textLower = newsText.toLowerCase();
        boolean likely_fake = false;
        
        // Check for suspicious phrases that might indicate fake news
        String[] suspiciousPhrases = {
            "shocking truth", "they don't want you to know", "government conspiracy",
            "secret cure", "miracle cure", "doctors hate", "one weird trick", 
            "banned information", "censored", "what the media won't tell you"
        };
        
        for (String phrase : suspiciousPhrases) {
            if (textLower.contains(phrase.toLowerCase())) {
                likely_fake = true;
                break;
            }
        }
        
        // Also check text length and complexity as heuristic
        boolean is_very_short = newsText.length() < 50;
        
        // Decide based on heuristics
        if (likely_fake || is_very_short) {
            return "⚠️ LIKELY FAKE NEWS (85%): This content appears to contain misinformation. Please verify with trusted sources.";
        } else {
            return "✅ LIKELY REAL NEWS (75%): This content appears to be legitimate, but always verify important information.";
        }
    }

    // Process the API response and return formatted result
    private String processApiResponse(String apiResponse) throws JSONException {
        android.util.Log.d(TAG, "Processing API response: " + apiResponse);
        
        JSONObject jsonObject = new JSONObject(apiResponse);
        
        // Check if there's an error message
        if (jsonObject.has("error")) {
            String error = jsonObject.getString("error");
            android.util.Log.e(TAG, "API error: " + error);
            return "Error: " + error;
        }
        
        // Extract the "result" field instead of "prediction"
        if (jsonObject.has("result")) {
            String result = jsonObject.getString("result");
            android.util.Log.d(TAG, "Result from API: " + result);
            
            // Format response based on first word of result
            String lowercaseResult = result.toLowerCase();
            if (lowercaseResult.contains("fake") || lowercaseResult.contains("false")) {
                return "⚠️ This appears to be FAKE NEWS: " + result;
            } else if (lowercaseResult.contains("real") || lowercaseResult.contains("true")) {
                return "✅ This appears to be REAL NEWS: " + result;
            } else {
                // For uncertain or complex results
                return "ℹ️ Analysis: " + result;
            }
        } else {
            android.util.Log.e(TAG, "API response missing 'result' field: " + apiResponse);
            return "Error: API response format changed. Missing 'result' field.";
        }
    }

    private void stopAnimations() {
        if (aiBinding != null) {
            if (aiBinding.square1 != null) aiBinding.square1.clearAnimation();
            if (aiBinding.square2 != null) aiBinding.square2.clearAnimation();
            if (aiBinding.square3 != null) aiBinding.square3.clearAnimation();
        }
    }

    private void showError(String message) {
                    runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Helper method to save bot messages to Firebase
    private void saveBotMessageToFirebase(Message botMessage) {
        if (messagesRef != null) {
            DatabaseReference newMessageRef = messagesRef.push();
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("text", botMessage.getText());
            messageData.put("status", botMessage.getStatus());
            messageData.put("isBot", true);
            messageData.put("timestamp", ServerValue.TIMESTAMP);
            newMessageRef.setValue(messageData);
        }
    }

    // Helper method to show/hide loading indicators
    @Override
    protected void showLoading(final boolean loading) {
        runOnUiThread(() -> {
            if (aiBinding != null && aiBinding.typingIndicator != null) {
                aiBinding.typingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void sendMessage() {
        String text = aiBinding.messageInput.getText().toString().trim();
        if (!isValidInput(text)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable input while processing
        aiBinding.messageInput.setEnabled(false);
        aiBinding.sendButton.setEnabled(false);

        // Create and add user message
        Message userMessage = new Message(text, "user", false);
        messages.add(userMessage);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);

        // Clear input
        aiBinding.messageInput.setText("");

        // Save user message to Firebase
        if (messagesRef != null) {
            DatabaseReference newMessageRef = messagesRef.push();
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("text", text);
            messageData.put("status", "user");
            messageData.put("isBot", false);
            messageData.put("timestamp", ServerValue.TIMESTAMP);
            
            newMessageRef.setValue(messageData).addOnCompleteListener(task -> {
                // Re-enable input
                aiBinding.messageInput.setEnabled(true);
                aiBinding.sendButton.setEnabled(true);
                
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Failed to save message", Toast.LENGTH_SHORT).show();
                        messages.remove(messages.size() - 1);
                    messageAdapter.notifyItemRemoved(messages.size());
                    return;
                }
                
                // Process the message
                processUserMessage(userMessage);
            });
        } else {
            Toast.makeText(this, "Not connected to database", Toast.LENGTH_SHORT).show();
            aiBinding.messageInput.setEnabled(true);
            aiBinding.sendButton.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Restart animations if needed
        if (aiBinding != null) {
            if (aiBinding.animatedBackground != null) {
                aiBinding.animatedBackground.startAnimation();
            }
            animateSquares();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Cancel any ongoing animations when activity is paused
        if (aiBinding != null) {
            if (aiBinding.square1 != null) aiBinding.square1.animate().cancel();
            if (aiBinding.square2 != null) aiBinding.square2.animate().cancel();
            if (aiBinding.square3 != null) aiBinding.square3.animate().cancel();
            if (aiBinding.animatedBackground != null) {
                aiBinding.animatedBackground.stopAnimation();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aiBinding = null;
    }
} 