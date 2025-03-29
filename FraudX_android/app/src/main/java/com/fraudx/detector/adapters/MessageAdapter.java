package com.fraudx.detector.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.R;
import com.fraudx.detector.models.Message;
import java.util.List;

/**
 * Adapter for Fake News Detector messages
 * Uses specific layouts and REAL/FAKE terminology for the Fake News Detector feature
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final CardView messageContainer;
        private final TextView firstWord;
        private final TextView messageText;
        private final TextView statusText;
        private final ConstraintLayout.LayoutParams containerParams;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            firstWord = itemView.findViewById(R.id.firstWord);
            messageText = itemView.findViewById(R.id.messageText);
            statusText = itemView.findViewById(R.id.statusText);
            containerParams = (ConstraintLayout.LayoutParams) messageContainer.getLayoutParams();
        }

        void bind(Message message) {
            String text = message.getText();
            String status = message.getStatus();
            boolean isBot = message.isBot();

            // Always display the message text with proper visibility and style
            messageText.setText(text);
            messageText.setVisibility(View.VISIBLE);
            
            // Debug log the message content
            android.util.Log.d("MessageAdapter", "Binding message: " + text + ", Status: " + status + ", isBot: " + isBot);

            // Handle alignment and background based on message type
            if (isBot) {
                // Bot message - align left
                containerParams.horizontalBias = 0f; 
                containerParams.setMarginStart(8);
                containerParams.setMarginEnd(80);
                
                // Choose background based on status
                if ("safe".equalsIgnoreCase(status) || "real".equalsIgnoreCase(status)) {
                    messageContainer.setBackgroundResource(R.drawable.bot_message_safe_background);
                    messageText.setTextColor(0xFF000000); // Black text for safe/real messages
                    messageText.setGravity(android.view.Gravity.START);
                } else if ("scam".equalsIgnoreCase(status) || "fake".equalsIgnoreCase(status)) {
                    messageContainer.setBackgroundResource(R.drawable.bot_message_scam_background);
                    messageText.setTextColor(0xFF000000); // Black text for scam/fake messages
                    messageText.setGravity(android.view.Gravity.START);
                } else {
                    messageContainer.setBackgroundResource(R.drawable.bot_message_background);
                    messageText.setTextColor(0xFFFFFFFF); // White text for neutral messages
                    messageText.setGravity(android.view.Gravity.START);
                }

                // Show status badge if available
                if (status != null && !status.isEmpty() && 
                    !status.equalsIgnoreCase("unknown") && 
                    !status.equalsIgnoreCase("processing")) {
                    firstWord.setVisibility(View.VISIBLE);
                    firstWord.setText(status.toUpperCase());
                    
                    // Set status badge color
                    if ("safe".equalsIgnoreCase(status) || "real".equalsIgnoreCase(status)) {
                        firstWord.setBackgroundResource(R.drawable.status_safe_background);
                        firstWord.setTextColor(0xFFFFFFFF); // White text for real/safe badge
                    } else if ("scam".equalsIgnoreCase(status) || "fake".equalsIgnoreCase(status)) {
                        firstWord.setBackgroundResource(R.drawable.status_scam_background);
                        firstWord.setTextColor(0xFFFFFFFF); // White text for scam/fake badge
                    } else {
                        firstWord.setBackgroundResource(R.drawable.status_unknown_background);
                    }
                } else {
                    firstWord.setVisibility(View.GONE);
                }
                
                // Hide the extra status text
                statusText.setVisibility(View.GONE);
                
                // Don't set cardBackgroundColor for bot messages - use the drawable backgrounds
                messageContainer.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            } else {
                // User message - align right
                containerParams.horizontalBias = 1f;
                containerParams.setMarginStart(80);
                containerParams.setMarginEnd(8);
                messageContainer.setBackgroundResource(R.drawable.user_message_background);
                messageText.setTextColor(0xFFFFFFFF); // White text for user messages
                messageText.setGravity(android.view.Gravity.END); // Align text to the right
                
                // User messages don't show status badges
                firstWord.setVisibility(View.GONE);
                statusText.setVisibility(View.GONE);
                
                // Set card background for user messages
                messageContainer.setCardBackgroundColor(0xFF4668F8); // Blue background
            }

            messageContainer.setCardElevation(1f); // Reduce elevation for flatter look
            
            // Apply layout params
            messageContainer.setLayoutParams(containerParams);
        }
    }
}
