package com.fraudx.detector.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fraudx.detector.R;
import com.fraudx.detector.adapters.NewsAdapter;
import com.fraudx.detector.adapters.ScamAdapter;
import com.fraudx.detector.databinding.FragmentHomeBinding;
import com.fraudx.detector.models.News;
import com.fraudx.detector.models.Scam;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.fraudx.detector.ui.detector.AIFakeNewsDetectorActivity;
import com.fraudx.detector.ui.detector.AIScamDetectorActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private NewsAdapter newsAdapter;
    private ScamAdapter scamAdapter;
    private DatabaseReference databaseReference;
    private List<News> newsList;
    private List<Scam> scamList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance("https://fraudx-4b017-default-rtdb.firebaseio.com").getReference();

        // Initialize lists and adapters
        newsList = new ArrayList<>();
        scamList = new ArrayList<>();
        newsAdapter = new NewsAdapter(newsList, news -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(news.getUrl()));
            startActivity(intent);
        });
        scamAdapter = new ScamAdapter(scamList, scam -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(scam.getUrl()));
            startActivity(intent);
        });

        // Setup RecyclerViews
        setupRecyclerViews();

        // Setup click listeners
        binding.fakeNewsButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                startActivity(new Intent(requireContext(), AIFakeNewsDetectorActivity.class));
            });
        });

        binding.scamDetectorButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                startActivity(new Intent(requireContext(), AIScamDetectorActivity.class));
            });
        });



        // Fetch data
        fetchNewsResults();
        fetchScamResults();
    }

    private void setupRecyclerViews() {
        // Setup News RecyclerView
        binding.newsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.newsRecyclerView.setHasFixedSize(true);
        
        binding.newsRecyclerView.setAdapter(newsAdapter);

        // Setup Scams RecyclerView
        binding.scamsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.scamsRecyclerView.setHasFixedSize(true);

        binding.scamsRecyclerView.setAdapter(scamAdapter);
    }

    private void fetchNewsResults() {
        binding.progressBar.setVisibility(View.VISIBLE);
        databaseReference.child("news").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                newsList.clear();
                for (DataSnapshot newsSnapshot : snapshot.getChildren()) {
                    News news = newsSnapshot.getValue(News.class);
                    if (news != null) {
                        newsList.add(news);
                    }
                }
                newsAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load news: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchScamResults() {
        binding.progressBar.setVisibility(View.VISIBLE);
        // Fetching scams from the "scams" node for the Top Online Scams section
        databaseReference.child("scams").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scamList.clear();
                for (DataSnapshot scamSnapshot : snapshot.getChildren()) {
                    Scam scam = scamSnapshot.getValue(Scam.class);
                    if (scam != null) {
                        scamList.add(scam);
                    }
                }
                scamAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load online scams: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateButtonClick(View view, Runnable action) {
        if (!isAdded()) return;
        
        // Add a glow effect to the button before starting the action
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
                        if (isAdded()) {
                            // Add a slight delay before launching the activity for better visual feedback
                            view.postDelayed(action, 50);
                        }
                    })
                    .start();
            })
            .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}