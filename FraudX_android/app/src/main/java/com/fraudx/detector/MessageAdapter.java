package com.fraudx.detector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.models.Message;
import java.util.List;

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
        return messages.size();
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

            // Set message text
            messageText.setText(text);

            // Handle alignment and background based on message type
            if (isBot) {
                containerParams.horizontalBias = 0f; // Align to left
                containerParams.setMarginStart(8);
                containerParams.setMarginEnd(48);

                // Extract and display first word if it's a status word
                String firstWordText = getFirstWord(text);
                if (isStatusWord(firstWordText)) {
                    firstWord.setVisibility(View.VISIBLE);
                    firstWord.setText(firstWordText);
                    messageText.setText(text.substring(firstWordText.length()).trim());

                    // Set background based on status
                    if (isPositiveStatus(firstWordText)) {
                        messageContainer.setBackgroundResource(R.drawable.bot_message_safe_background);
                    } else {
                        messageContainer.setBackgroundResource(R.drawable.bot_message_scam_background);
                    }
                } else {
                    firstWord.setVisibility(View.GONE);
                    messageContainer.setBackgroundResource(R.drawable.bot_message_safe_background);
                }

                // Show status if available
                if (status != null && !status.isEmpty()) {
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText(status.toUpperCase());
                } else {
                    statusText.setVisibility(View.GONE);
                }
            } else {
                // User message
                containerParams.horizontalBias = 1f; // Align to right
                containerParams.setMarginStart(48);
                containerParams.setMarginEnd(8);
                messageContainer.setBackgroundResource(R.drawable.user_message_background);
                firstWord.setVisibility(View.GONE);
                statusText.setVisibility(View.GONE);
            }

            messageContainer.setLayoutParams(containerParams);
        }

        private String getFirstWord(String text) {
            if (text == null || text.isEmpty()) return "";
            String[] words = text.split("\\s+");
            return words.length > 0 ? words[0].toUpperCase() : "";
        }

        private boolean isStatusWord(String word) {
            return word.equals("REAL") || word.equals("FAKE") || 
                   word.equals("SAFE") || word.equals("SCAM") ||
                   word.equals("UNKNOWN");
        }

        private boolean isPositiveStatus(String status) {
            return status.equals("REAL") || status.equals("SAFE");
        }
    }
} 