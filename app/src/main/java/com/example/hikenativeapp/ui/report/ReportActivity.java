package com.example.hikenativeapp.ui.report;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.report.ReportStatistics;
import com.example.hikenativeapp.util.Constants;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity hiển thị báo cáo thống kê hiking
 */
public class ReportActivity extends AppCompatActivity {

    private ReportViewModel viewModel;
    private Toolbar toolbar;

    // UI Components
    private ProgressBar progressLoading;
    private View layoutStatistics;
    private TextView textTotalHikes;
    private TextView textTotalDistance;
    private TextView textTotalDuration;
    private TextView textAvgTemperature;
    private TextView textTopLocation;
    private PieChart chartDifficulty;
    private PieChart chartParking;

    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup ActionBar with back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Hiking Report");
        }

        // Initialize UI components
        initViews();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        // Get current user ID from SharedPreferences (similar to HikeListActivity)
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getInt(Constants.PREF_USER_ID, -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load statistics
        viewModel.loadStatistics(currentUserId);

        // Observe LiveData
        observeViewModel();
    }

    private void initViews() {
        progressLoading = findViewById(R.id.progress_loading);
        layoutStatistics = findViewById(R.id.layout_statistics);
        textTotalHikes = findViewById(R.id.text_total_hikes);
        textTotalDistance = findViewById(R.id.text_total_distance);
        textTotalDuration = findViewById(R.id.text_total_duration);
        textAvgTemperature = findViewById(R.id.text_avg_temperature);
        textTopLocation = findViewById(R.id.text_top_location);
        chartDifficulty = findViewById(R.id.chart_difficulty);
        chartParking = findViewById(R.id.chart_parking);
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressLoading.setVisibility(View.VISIBLE);
                layoutStatistics.setVisibility(View.GONE);
            } else {
                progressLoading.setVisibility(View.GONE);
                layoutStatistics.setVisibility(View.VISIBLE);
            }
        });

        // Observe statistics data
        viewModel.getStatistics().observe(this, this::displayStatistics);

        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayStatistics(ReportStatistics stats) {
        // Display basic statistics
        textTotalHikes.setText(String.valueOf(stats.getTotalHikes()));
        textTotalDistance.setText(String.format(Locale.getDefault(), "%.2f km", stats.getTotalDistance()));
        textTotalDuration.setText(String.format(Locale.getDefault(), "%.1f hrs", stats.getTotalDuration()));
        textAvgTemperature.setText(String.format(Locale.getDefault(), "%.1f°C", stats.getAverageTemperature()));

        // Display top location
        if (stats.getTopLocation() != null) {
            textTopLocation.setText(stats.getTopLocation() + " (" + stats.getTopLocationCount() + " trips)");
        } else {
            textTopLocation.setText("N/A");
        }

        // Setup charts
        setupDifficultyChart(stats);
        setupParkingChart(stats);
    }

    private void setupDifficultyChart(ReportStatistics stats) {
        List<PieEntry> entries = new ArrayList<>();

        if (stats.getEasyHikes() > 0) {
            entries.add(new PieEntry(stats.getEasyHikes(), "Easy"));
        }
        if (stats.getModerateHikes() > 0) {
            entries.add(new PieEntry(stats.getModerateHikes(), "Moderate"));
        }
        if (stats.getHardHikes() > 0) {
            entries.add(new PieEntry(stats.getHardHikes(), "Hard"));
        }

        if (entries.isEmpty()) {
            chartDifficulty.setNoDataText("No difficulty data available");
            chartDifficulty.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Custom colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(46, 204, 113));  // Green for Easy
        colors.add(Color.rgb(241, 196, 15));  // Yellow for Moderate
        colors.add(Color.rgb(231, 76, 60));   // Red for Hard
        dataSet.setColors(colors);

        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        chartDifficulty.setData(data);

        // Chart styling
        chartDifficulty.getDescription().setEnabled(false);
        chartDifficulty.setDrawHoleEnabled(true);
        chartDifficulty.setHoleColor(Color.WHITE);
        chartDifficulty.setTransparentCircleRadius(55f);
        chartDifficulty.setEntryLabelColor(Color.BLACK);
        chartDifficulty.setEntryLabelTextSize(12f);
        chartDifficulty.getLegend().setEnabled(true);
        chartDifficulty.animateY(1000);
        chartDifficulty.invalidate();
    }

    private void setupParkingChart(ReportStatistics stats) {
        List<PieEntry> entries = new ArrayList<>();

        if (stats.getHikesWithParking() > 0) {
            entries.add(new PieEntry(stats.getHikesWithParking(), "With Parking"));
        }
        if (stats.getHikesWithoutParking() > 0) {
            entries.add(new PieEntry(stats.getHikesWithoutParking(), "No Parking"));
        }

        if (entries.isEmpty()) {
            chartParking.setNoDataText("No parking data available");
            chartParking.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Custom colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(52, 152, 219));  // Blue for With Parking
        colors.add(Color.rgb(149, 165, 166)); // Gray for No Parking
        dataSet.setColors(colors);

        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        chartParking.setData(data);

        // Chart styling
        chartParking.getDescription().setEnabled(false);
        chartParking.setDrawHoleEnabled(true);
        chartParking.setHoleColor(Color.WHITE);
        chartParking.setTransparentCircleRadius(55f);
        chartParking.setEntryLabelColor(Color.BLACK);
        chartParking.setEntryLabelTextSize(12f);
        chartParking.getLegend().setEnabled(true);
        chartParking.animateY(1000);
        chartParking.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
