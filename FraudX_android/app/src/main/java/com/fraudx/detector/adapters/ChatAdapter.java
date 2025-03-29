package com.fraudx.detector.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fraudx.detector.R;
import com.fraudx.detector.models.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isBot() ? VIEW_TYPE_BOT : VIEW_TYPE_USER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_bot, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.messageText.setText(message.getText());
        
        if (message.isBot() && holder.statusText != null) {
            holder.statusText.setText(message.getStatus().toUpperCase());
            
            // Set status color based on the status
            int statusColor;
            switch (message.getStatus().toLowerCase()) {
                case "real":
                    statusColor = holder.itemView.getContext().getColor(R.color.status_real);
                    break;
                case "fake":
                    statusColor = holder.itemView.getContext().getColor(R.color.status_fake);
                    break;
                default:
                    statusColor = holder.itemView.getContext().getColor(R.color.status_unknown);
                    break;
            }
            holder.statusText.setBackgroundColor(statusColor);
        }

        // Set up report button click listener
        holder.reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://cybercrime.gov.in/Webform/Accept.aspx"));
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView statusText;
        Button reportButton;

        MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            if (viewType == VIEW_TYPE_BOT) {
                statusText = itemView.findViewById(R.id.statusText);
            }
            reportButton = itemView.findViewById(R.id.reportButton);
        }
    }
} 