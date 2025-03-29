package com.fraudx.detector.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fraudx.detector.R;
import com.fraudx.detector.models.Scam;

import java.util.List;

public class ScamAdapter extends RecyclerView.Adapter<ScamAdapter.ScamViewHolder> {
    private List<Scam> scamList;
    private OnScamClickListener listener;

    public interface OnScamClickListener {
        void onScamClick(Scam scam);
    }

    public ScamAdapter(List<Scam> scamList, OnScamClickListener listener) {
        this.scamList = scamList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scam, parent, false);
        return new ScamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScamViewHolder holder, int position) {
        Scam scam = scamList.get(position);
        holder.scamTitleTextView.setText(scam.getTitle());
        holder.scamDescriptionTextView.setText(scam.getDescription());
        holder.scamTypeTextView.setText(scam.getRiskLevel());

        // Set status color based on risk level
        int statusColor;
        switch (scam.getRiskLevel().toLowerCase()) {
            case "high":
                statusColor = holder.itemView.getContext().getColor(R.color.risk_high);
                break;
            case "medium":
                statusColor = holder.itemView.getContext().getColor(R.color.risk_medium);
                break;
            case "low":
                statusColor = holder.itemView.getContext().getColor(R.color.risk_low);
                break;
            default:
                statusColor = holder.itemView.getContext().getColor(R.color.risk_unknown);
        }
        
        // Create a pill-shaped background with the status color
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(16);
        drawable.setColor(statusColor);
        holder.scamTypeTextView.setBackground(drawable);
        
        // Add padding and text styling
        holder.scamTypeTextView.setPadding(16, 4, 16, 4);
        holder.scamTypeTextView.setTextColor(Color.WHITE);
        holder.scamTypeTextView.setTypeface(null, Typeface.BOLD);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScamClick(scam);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scamList.size();
    }

    public void updateScams(List<Scam> newScamList) {
        this.scamList = newScamList;
        notifyDataSetChanged();
    }

    static class ScamViewHolder extends RecyclerView.ViewHolder {
        TextView scamTitleTextView;
        TextView scamDescriptionTextView;
        TextView scamTypeTextView;

        ScamViewHolder(@NonNull View itemView) {
            super(itemView);
            scamTitleTextView = itemView.findViewById(R.id.scamTitleTextView);
            scamDescriptionTextView = itemView.findViewById(R.id.scamDescriptionTextView);
            scamTypeTextView = itemView.findViewById(R.id.scamTypeTextView);
        }
    }
} 