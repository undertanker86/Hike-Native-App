package com.example.hikenativeapp.ui.confirm_hike;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.ui.hike_list.HikeListActivity;
import com.example.hikenativeapp.util.Constants;

import java.util.Locale;

public class ConfirmHikeActivity extends AppCompatActivity {

    private TextView tvName, tvLocation, tvDate, tvParking, tvLength, tvDifficulty;
    private TextView tvDescription, tvWeatherCondition, tvEstimatedDuration;
    private Button btnConfirm, btnEdit;

    private ConfirmHikeViewModel viewModel;
    private String name, location, date, difficulty, description, weatherCondition, estimatedDuration;
    private boolean parkingAvailable;
    private double length;
    private double temperature = 0.0;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_hike);

        initializeViews();
        setupViewModel();
        loadDataFromIntent();
        displayHikeDetails();
        setupClickListeners();
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tv_name);
        tvLocation = findViewById(R.id.tv_location);
        tvDate = findViewById(R.id.tv_date);
        tvParking = findViewById(R.id.tv_parking);
        tvLength = findViewById(R.id.tv_length);
        tvDifficulty = findViewById(R.id.tv_difficulty);
        tvDescription = findViewById(R.id.tv_description);
        tvWeatherCondition = findViewById(R.id.tv_weather_condition);
        tvEstimatedDuration = findViewById(R.id.tv_estimated_duration);

        btnConfirm = findViewById(R.id.btn_confirm);
        btnEdit = findViewById(R.id.btn_edit);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Setup Toolbar as ActionBar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Confirm Hike Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ConfirmHikeViewModel.class);
        HikeRepository repository = new HikeRepository(this);
        viewModel.setRepository(repository);

        // Observe hike save result
        viewModel.getHikeId().observe(this, hikeId -> {
            if (hikeId != null && hikeId > 0) {
                Toast.makeText(this, "Hike saved successfully!", Toast.LENGTH_SHORT).show();
                navigateToHikeList();
            } else if (hikeId != null) {
                Toast.makeText(this, "Failed to save hike", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe saving state
        viewModel.getIsSaving().observe(this, isSaving -> {
            btnConfirm.setEnabled(!isSaving);
            btnEdit.setEnabled(!isSaving);
        });
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        location = intent.getStringExtra("location");
        date = intent.getStringExtra("date");
        parkingAvailable = intent.getBooleanExtra("parking_available", false);
        length = intent.getDoubleExtra("length", 0.0);
        difficulty = intent.getStringExtra("difficulty");
        description = intent.getStringExtra("description");
        weatherCondition = intent.getStringExtra("weather_condition");
        estimatedDuration = intent.getStringExtra("estimated_duration");
        temperature = intent.getDoubleExtra("temperature", 0.0);

        // Load latitude and longitude
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
    }

    private void displayHikeDetails() {
        tvName.setText(name);
        tvLocation.setText(location);
        tvDate.setText(date);
        tvParking.setText(parkingAvailable ? "Yes" : "No");
        tvLength.setText(String.format(Locale.US, "%.1f km", length));
        tvDifficulty.setText(difficulty);
        tvDescription.setText(description.isEmpty() ? "No description provided" : description);
        tvWeatherCondition.setText(weatherCondition);
        tvEstimatedDuration.setText(String.format(Locale.US, "%s hours", estimatedDuration));
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> saveHike());
        btnEdit.setOnClickListener(v -> goBackToEdit());
    }

    private void saveHike() {
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int userId = prefs.getInt(Constants.PREF_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create hike object
        Hike hike = new Hike();
        hike.setUserId(userId);
        hike.setName(name);
        hike.setLocation(location);
        hike.setHikeDate(date);
        hike.setParkingAvailable(parkingAvailable);
        hike.setLength(length);
        hike.setDifficulty(difficulty);
        hike.setDescription(description);

        // Set latitude and longitude
        hike.setLatitude(latitude);
        hike.setLongitude(longitude);

        // Set weather condition and temperature
        if (weatherCondition != null && !weatherCondition.isEmpty()) {
            hike.setWeatherCondition(weatherCondition);
        }

        // Set temperature from weather data
        hike.setTemperature(temperature);

        // Set estimated duration
        if (estimatedDuration != null && !estimatedDuration.isEmpty()) {
            try {
                hike.setEstimatedDuration(Double.parseDouble(estimatedDuration));
            } catch (NumberFormatException e) {
                hike.setEstimatedDuration(0.0);
            }
        }

        // Save using ViewModel
        viewModel.saveHike(hike);
    }

    private void goBackToEdit() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void navigateToHikeList() {
        Intent intent = new Intent(this, HikeListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
