package com.fraudx.detector.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.R;
import com.fraudx.detector.models.Headline;
import java.util.List;

public class HeadlineAdapter extends RecyclerView.Adapter<HeadlineAdapter.ViewHolder> {
    private final Context context;
    private List<Headline> headlines;

    public HeadlineAdapter(Context context, List<Headline> headlines) {
        this.context = context;
        this.headlines = headlines;
    }

    public void setHeadlines(List<Headline> headlines) {
        this.headlines = headlines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_headline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Headline headline = headlines.get(position);
        // TODO: Bind headline data to views
    }

    @Override
    public int getItemCount() {
        return headlines != null ? headlines.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
            // TODO: Initialize views
        }
    }
}
