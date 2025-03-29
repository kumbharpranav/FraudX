package com.fraudx.detector.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.R;
import com.fraudx.detector.adapters.MessageAdapter;
import com.fraudx.detector.models.Message;
import com.google.android.material.navigation.NavigationView;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import com.fraudx.detector.databinding.ActivityDetectorBaseBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Toast;
import com.fraudx.detector.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;
import android.util.Patterns;
import com.google.firebase.database.ServerValue;
import com.fraudx.detector.ui.detector.AIScamDetectorActivity;
import com.fraudx.detector.ui.detector.AIFakeNewsDetectorActivity;
import com.fraudx.detector.ui.auth.LoginActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.util.Log;
import android.view.WindowManager;
import android.graphics.Color;
import android.os.Build;

public abstract class BaseDetectorActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener {

    // Common variables for subclasses to use
    protected RecyclerView chatRecyclerView;
    protected EditText messageInput;
    protected List<Message> messages = new ArrayList<>();
    protected MessageAdapter messageAdapter;
    protected DrawerLayout drawerLayout;
    protected ProgressBar progressBar;
    protected DatabaseReference messagesRef;
    protected SessionManager sessionManager;
    protected NavigationView navigationView;
    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable hardware acceleration for smoother animations
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            
        // Make layout work with notch/cutout displays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        
        // Make status bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        
        // Initialize shared components
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        
        // Initialize the messages list
        messages = new ArrayList<>();
        
        // Note: chatRecyclerView and messageInput should be initialized by derived classes before calling super.onCreate
        
        // Set up recycler view, and Firebase if not null
        if (chatRecyclerView != null) {
        setupRecyclerView();
        } else {
            android.util.Log.e("BaseDetectorActivity", "ChatRecyclerView is null, cannot set up");
        }
        
        // Setup Firebase and message reference
        setupFirebase();
        
        // Load previous messages in background if views are ready
        if (chatRecyclerView != null && messageAdapter != null) {
        loadPreviousMessages();
        }
        
        android.util.Log.d("BaseDetectorActivity", "onCreate complete");
    }

    // Default implementation that can be overridden by child classes
    protected void showLoading(boolean show) {
        // Child classes should override this with their specific implementation
        android.util.Log.d("BaseDetectorActivity", "showLoading(" + show + ") - default implementation, should be overridden");
    }

    // Make abstract to force child classes to implement
    protected abstract String getActivityTitle();
    protected abstract String getFirebaseChildPath();
    protected abstract void processUserMessage(Message message);

    // Add getter and setter methods for views that child classes can use
    protected void setRecyclerView(RecyclerView recyclerView) {
        this.chatRecyclerView = recyclerView;
    }

    protected void setMessageInput(EditText messageInput) {
        this.messageInput = messageInput;
    }

    protected void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    protected void addMessage(Message message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        if (messageAdapter != null) {
        messageAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
        } else {
            android.util.Log.e("BaseDetectorActivity", "messageAdapter is null in addMessage");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
        } else if (itemId == R.id.nav_fake_news) {
            if (!(this instanceof AIFakeNewsDetectorActivity)) {
                intent = new Intent(this, AIFakeNewsDetectorActivity.class);
            }
        } else if (itemId == R.id.nav_scam_detector) {
            if (!(this instanceof AIScamDetectorActivity)) {
                intent = new Intent(this, AIScamDetectorActivity.class);
            }
        } else if (itemId == R.id.nav_account) {
            intent = new Intent(this, AccountDetailsActivity.class);
        } else if (itemId == R.id.nav_logout) {
            // Handle logout
            sessionManager.logout();
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (intent != null) {
            if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
            }
            startActivity(intent);
            return true;
        } else if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        // Add null check to prevent NullPointerException
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    protected void setupRecyclerView() {
        // Check if we successfully found the RecyclerView
        if (chatRecyclerView == null) {
            android.util.Log.e("BaseDetectorActivity", "RecyclerView not found");
            Toast.makeText(this, "Error initializing chat view", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Ensure messages list is initialized
        if (messages == null) {
            messages = new ArrayList<>();
            android.util.Log.d("BaseDetectorActivity", "Messages list was null, initializing");
        }
        
        // Setup layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        
        // Setup adapter
        messageAdapter = new MessageAdapter(messages);
        chatRecyclerView.setAdapter(messageAdapter);
        
        // Optimize recycler view
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setItemViewCacheSize(20);
        
        // Add smooth scrolling behavior 
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
        chatRecyclerView.addOnScrollListener(smoothScrollListener);
        
        // Add item animations
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(300);
        itemAnimator.setRemoveDuration(300);
        itemAnimator.setMoveDuration(300);
        itemAnimator.setChangeDuration(300);
        chatRecyclerView.setItemAnimator(itemAnimator);
        
        // Add layout animation
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
            this, R.anim.layout_animation_fall_down);
        chatRecyclerView.setLayoutAnimation(animation);
        
        android.util.Log.d("BaseDetectorActivity", "RecyclerView setup complete");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    protected void setupFirebase() {
        // First check if the user is logged in via SessionManager
        if (!sessionManager.isLoggedIn()) {
            // Handle case where user is not logged in
            Toast.makeText(this, "Please login to use this feature", Toast.LENGTH_LONG).show();
            android.util.Log.e("BaseDetectorActivity", "User not logged in, cannot set up Firebase");
            
            // Initialize messages with empty list to prevent NPE
            messagesRef = null;
            
            // Add a dummy message to explain login requirement
            Message systemMessage = new Message(
                "You need to be logged in to use this feature. Please login to your account.",
                "unknown", 
                true);
            
            // Add this message to the UI
            if (messageAdapter != null) {
                messages.add(systemMessage);
                messageAdapter.notifyDataSetChanged();
                if (chatRecyclerView != null) {
                    chatRecyclerView.smoothScrollToPosition(Math.max(0, messages.size() - 1));
                }
            }
            
            return;
        }
        
        try {
            // Get user email from session manager
            String userEmail = sessionManager.getUserEmail();
            android.util.Log.d("BaseDetectorActivity", "Setting up Firebase with user email: " + userEmail);
            
            // If we don't have an email, try to get one from Firebase Auth
            if (userEmail == null || userEmail.isEmpty()) {
                if (mAuth != null && mAuth.getCurrentUser() != null) {
                    userEmail = mAuth.getCurrentUser().getEmail();
                    // Save to session manager for future use
                    sessionManager.setUserEmail(userEmail);
                    android.util.Log.d("BaseDetectorActivity", "Got email from Firebase Auth: " + userEmail);
                } else {
                    // Final fallback - truly no user email available
                    Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
                    android.util.Log.e("BaseDetectorActivity", "No user email available from auth");
                    return;
                }
            }
            
            // Create a userId by replacing non-alphanumeric characters with underscore
            String userId = userEmail.replaceAll("[^a-zA-Z0-9]", "_");
            
            // Now we have a valid userId, set up Firebase
            String childPath = getFirebaseChildPath();
            android.util.Log.d("BaseDetectorActivity", "Setting up Firebase with path: " + childPath + "/" + userId + "/messages");
            
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            messagesRef = database.getReference()
                .child(childPath)
            .child(userId)
            .child("messages");
            
        // Keep data synced
        messagesRef.keepSynced(true);
            
            android.util.Log.d("BaseDetectorActivity", "Firebase setup complete, messagesRef: " + (messagesRef != null ? "successful" : "failed"));
        } catch (Exception e) {
            android.util.Log.e("BaseDetectorActivity", "Error setting up Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Error connecting to database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected void loadPreviousMessages() {
        android.util.Log.d("BaseDetectorActivity", "Loading previous messages");
        
        // Guard clause: Check if views are valid
        if (chatRecyclerView == null || messageAdapter == null) {
            android.util.Log.e("BaseDetectorActivity", "Cannot load messages, RecyclerView or Adapter is null");
            return;
        }
        
        // Guard clause: Check Firebase reference
        if (messagesRef == null) {
            android.util.Log.e("BaseDetectorActivity", "Cannot load messages, Firebase reference is null");
            return;
        }
        
        // Clear existing messages if any
        if (messages != null && !messages.isEmpty()) {
            int size = messages.size();
            messages.clear();
            messageAdapter.notifyItemRangeRemoved(0, size);
        }
        
        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Limit query to last 50 messages for performance
        messagesRef.orderByChild("timestamp").limitToLast(50)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    android.util.Log.d("BaseDetectorActivity", "Received message data: " + dataSnapshot.getChildrenCount() + " messages");
                    
                    // Skip if activity is destroyed
                    if (isFinishing() || isDestroyed()) {
                        android.util.Log.w("BaseDetectorActivity", "Activity destroyed, skipping message loading");
                        return;
                    }
                    
                    // Process the messages
                    try {
                        // Temporary list to hold loaded messages
                        List<Message> loadedMessages = new ArrayList<>();
                        
                        for (DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                            String text = msgSnapshot.child("text").getValue(String.class);
                            String status = msgSnapshot.child("status").getValue(String.class);
                            Boolean isBot = msgSnapshot.child("isBot").getValue(Boolean.class);
                            
                            if (text == null) {
                                android.util.Log.w("BaseDetectorActivity", "Skipping message with null text");
                                continue;
                            }
                            
                            // Fix any null values with defaults
                            status = (status != null) ? status : "user";
                            isBot = (isBot != null) ? isBot : false;
                            
                            // For bot messages, extract status from first word of message if possible
                            if (isBot && status.equals("assistant")) {
                                String firstWord = extractFirstWord(text).toLowerCase();
                                if (!firstWord.isEmpty()) {
                                    // Use first word as status (e.g., "scam", "safe", "fake", "real")
                                    status = firstWord;
                                }
                            }
                            
                            // Create the message and add to our collection
                            Message message = new Message(text, status, isBot);
                            loadedMessages.add(message);
                        }
                        
                        // Only update UI if we have messages and if the view is still valid
                        if (!loadedMessages.isEmpty() && chatRecyclerView != null && !isFinishing() && !isDestroyed()) {
                            // Replace our messages list and notify adapter
                            messages.addAll(loadedMessages);
                            messageAdapter.notifyItemRangeInserted(0, loadedMessages.size());
                            
                            // Scroll to the last message
                            chatRecyclerView.post(() -> {
                                if (chatRecyclerView != null && messages != null && !messages.isEmpty()) {
                                    chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                                }
                            });
                            
                            android.util.Log.d("BaseDetectorActivity", "Successfully loaded " + loadedMessages.size() + " messages");
                        } else {
                            android.util.Log.w("BaseDetectorActivity", "No messages found or view is no longer valid");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BaseDetectorActivity", "Error processing messages: " + e.getMessage(), e);
                    } finally {
                        // Hide loading indicator no matter what
                        if (progressBar != null && !isFinishing() && !isDestroyed()) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    android.util.Log.e("BaseDetectorActivity", "Database error: " + databaseError.getMessage());
                    
                    // Hide loading indicator
                    if (progressBar != null && !isFinishing() && !isDestroyed()) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    // Show error toast if activity is still active
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(BaseDetectorActivity.this, 
                            "Failed to load messages: " + databaseError.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    // Helper method to extract the first word from a message
    private String extractFirstWord(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Try to find first word by splitting on whitespace
        String[] words = text.trim().split("\\s+");
        if (words.length > 0) {
            // Clean up the word by removing leading/trailing punctuation
            String firstWord = words[0].replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
            
            // Check for status-indicating emojis and map them
            if (firstWord.contains("⚠️")) {
                return "scam";
            } else if (firstWord.contains("✅")) {
                return "safe";
            } else if (firstWord.contains("❓")) {
                return "uncertain";
            }
            
            return firstWord;
        }
        
        return "";
    }

    protected void sendMessage() {
        // Check if views are properly initialized first
        if (messageInput == null || chatRecyclerView == null || messageAdapter == null) {
            android.util.Log.e("BaseDetectorActivity", "Cannot send message - views not initialized");
            Toast.makeText(this, "Please wait while the app initializes...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = messageInput.getText().toString().trim();
        if (!isValidInput(text)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear input first to prevent double-sends
        messageInput.setText("");

        // Create and add user message
        Message userMessage = new Message(text, "user", false);
        messages.add(userMessage);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
        
        android.util.Log.d("BaseDetectorActivity", "Sending message: " + text);

        // Save user message to Firebase
        if (messagesRef != null) {
            try {
        DatabaseReference newMessageRef = messagesRef.push();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("text", text);
                messageData.put("status", "user");
                messageData.put("isBot", false);
                messageData.put("timestamp", ServerValue.TIMESTAMP);
                
                newMessageRef.setValue(messageData).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.e("BaseDetectorActivity", "Failed to save message to Firebase: " + task.getException());
                        Toast.makeText(this, "Failed to save message", Toast.LENGTH_SHORT).show();
                        
                        // Don't remove the message from UI, but mark it as failed
                        userMessage.setStatus("error");
                        messageAdapter.notifyDataSetChanged();
                        return;
                    }
                    
                    android.util.Log.d("BaseDetectorActivity", "Message saved to Firebase successfully");
                    // Process the message
                    processUserMessage(userMessage);
                });
            } catch (Exception e) {
                android.util.Log.e("BaseDetectorActivity", "Error sending message: " + e.getMessage(), e);
                Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Add error message to chat
                Message errorMessage = new Message("Failed to send message: " + e.getMessage(), "error", true);
                messages.add(errorMessage);
                messageAdapter.notifyDataSetChanged();
                chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        } else {
            android.util.Log.e("BaseDetectorActivity", "Cannot send message - Firebase reference is null");
            Toast.makeText(this, "Not connected to database", Toast.LENGTH_SHORT).show();
            
            // Add error message to chat
            Message errorMessage = new Message("Not connected to database. Your message was not saved.", "error", true);
            messages.add(errorMessage);
            messageAdapter.notifyDataSetChanged();
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            
            // Still process the message locally since we can't save to Firebase
        processUserMessage(userMessage);
        }
    }

    protected boolean isValidInput(String input) {
        // Basic non-empty check
        if (input.trim().isEmpty()) {
            return false;
        }
        
        // Check if input is a URL
        if (Patterns.WEB_URL.matcher(input).matches()) {
            return true;
        }
        
        // Check for minimum text requirements (similar to JS implementation)
        String[] words = input.trim().split("\\s+");
        return words.length >= 2;
    }

    @Override
    protected void onDestroy() {
        // Clean up references to prevent memory leaks and NPEs
        if (messageAdapter != null) {
            messageAdapter = null;
        }
        
        if (messages != null) {
            messages.clear();
        }
        
        if (chatRecyclerView != null) {
            chatRecyclerView.setAdapter(null);
        }
        
        super.onDestroy();
    }

    protected void setupToolbar() {
        // Initialize and setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getActivityTitle());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 