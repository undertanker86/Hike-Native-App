package com.example.hikenativeapp.ui.add_hike;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.api.WeatherService;
import com.example.hikenativeapp.data.weather.WeatherInfo;
import com.example.hikenativeapp.ui.adapter.PlaceAutocompleteAdapter;
import com.example.hikenativeapp.ui.confirm_hike.ConfirmHikeActivity;
import com.example.hikenativeapp.util.Constants;
import com.example.hikenativeapp.util.LocationHelper;
import com.example.hikenativeapp.util.LocationSearchHelper;
import com.example.hikenativeapp.val.HikeValidator;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public class AddHikeActivity extends AppCompatActivity implements LocationHelper.LocationListener {

    private static final String TAG = "AddHikeActivity";
    private static final int SEARCH_DELAY_MS = 300;

    private TextInputLayout tilName, tilLocation, tilLength, tilDescription, tilEstimatedDuration;
    private EditText etName, etLocation, etLength, etDescription, etEstimatedDuration;
    private EditText etDate;
    private CheckBox cbParkingAvailable;
    private Spinner spinnerDifficulty;
    private Button btnSave, btnSelectDate;
    private Button btnGetWeatherForecast, btnSelectWeather;
    private TextView tvWeatherCondition, tvWeatherTemp, tvWeatherMessage;
    private FloatingActionButton fabGetCurrentLocation;
    private ImageButton btnResetLocation, btnRefreshLocation;
    private RecyclerView rvLocationSuggestions;

    private AddHikeViewModel viewModel;
    private LocationHelper locationHelper;
    private LocationSearchHelper locationSearchHelper;
    private PlaceAutocompleteAdapter autocompleteAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isSettingLocationProgrammatically = false;
    private WeatherService weatherService;
    private WeatherInfo currentWeatherInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_hike);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AddHikeViewModel.class);

        // Setup WeatherService and API key
        weatherService = WeatherService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = prefs.getString(Constants.PREF_WEATHER_API_KEY, Constants.STORMGLASS_API_KEY);
        viewModel.setWeatherApiKey(apiKey);
        weatherService.setApiKey(apiKey);

        // Initialize location helper
        locationHelper = new LocationHelper(this);
        locationHelper.setLocationListener(this);

        initializeViews();
        setupViewModel();
        setupLocationSearch();
        setupSpinner();
        setupDatePicker();
        setupClickListeners();
    }

    private void setupViewModel() {
        // Observe weather info
        viewModel.getWeatherInfo().observe(this, weatherInfo -> {
            if (weatherInfo != null) {
                currentWeatherInfo = weatherInfo;
                updateWeatherUI(weatherInfo);
                tvWeatherMessage.setVisibility(View.GONE);
                Log.d(TAG, "Weather forecast received: " +
                        weatherInfo.getWeatherConditionString() + ", " +
                        weatherInfo.getTemperature() + "°C");
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                tvWeatherMessage.setVisibility(View.VISIBLE);
                tvWeatherMessage.setText(getString(R.string.weather_error_message, error));
                Toast.makeText(this, "Failed to get weather forecast: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Weather forecast error: " + error);
            }
        });

        // Observe loading state
        viewModel.getIsLoadingWeather().observe(this, isLoading -> {
            if (isLoading != null) {
                btnGetWeatherForecast.setEnabled(!isLoading);
                if (isLoading) {
                    Toast.makeText(this, "Fetching weather forecast...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe selected date
        viewModel.getSelectedDate().observe(this, date -> {
            if (date != null && !date.isEmpty()) {
                etDate.setText(date);
            }
        });

        // Observe location
        viewModel.getCurrentLatitude().observe(this, latitude -> {
            // Location updated in ViewModel
        });

        viewModel.getCurrentLongitude().observe(this, longitude -> {
            // Location updated in ViewModel
        });
    }

    private void initializeViews() {
        // TextInputLayouts for error handling
        tilName = findViewById(R.id.til_name);
        tilLocation = findViewById(R.id.til_location);
        tilLength = findViewById(R.id.til_length);
        tilDescription = findViewById(R.id.til_description);
        tilEstimatedDuration = findViewById(R.id.til_estimated_duration);

        // EditTexts
        etName = findViewById(R.id.et_name);
        etLocation = findViewById(R.id.et_location);
        etLength = findViewById(R.id.et_length);
        etDescription = findViewById(R.id.et_description);
        etEstimatedDuration = findViewById(R.id.et_estimated_duration);
        etDate = findViewById(R.id.et_date);

        // Weather views
        tvWeatherCondition = findViewById(R.id.tv_weather_condition);
        tvWeatherTemp = findViewById(R.id.tv_weather_temp);
        tvWeatherMessage = findViewById(R.id.tv_weather_message);
        btnGetWeatherForecast = findViewById(R.id.btn_get_weather_forecast);
        btnSelectWeather = findViewById(R.id.btn_select_weather);

        // Other views
        cbParkingAvailable = findViewById(R.id.cb_parking_available);
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        btnSave = findViewById(R.id.btn_save);
        btnSelectDate = findViewById(R.id.btn_select_date);
        fabGetCurrentLocation = findViewById(R.id.fab_get_current_location);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Location search views
        rvLocationSuggestions = findViewById(R.id.rv_location_suggestions);
        btnResetLocation = findViewById(R.id.btn_reset_location);
        btnRefreshLocation = findViewById(R.id.btn_refresh_location);

        // Setup Toolbar as ActionBar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add New Hike");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup RecyclerView
        autocompleteAdapter = new PlaceAutocompleteAdapter(this::onPlaceSelected);
        rvLocationSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvLocationSuggestions.setAdapter(autocompleteAdapter);

        // Initially hide buttons and suggestions
        btnResetLocation.setVisibility(View.GONE);
        btnRefreshLocation.setVisibility(View.GONE);
    }

    /**
     * Lazy initialization for LocationSearchHelper
     * Only initialize when actually needed to save resources
     */
    private LocationSearchHelper getLocationSearchHelper() {
        if (locationSearchHelper == null) {
            Log.d(TAG, "Initializing LocationSearchHelper (lazy initialization)");
            locationSearchHelper = new LocationSearchHelper(this);
        }
        return locationSearchHelper;
    }

    /**
     * Setup location search functionality
     */
    private void setupLocationSearch() {
        etLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Skip if location is being set programmatically (GPS or place selection)
                if (isSettingLocationProgrammatically) {
                    Log.d(TAG, "Skipping search - location is being set programmatically");
                    return;
                }

                // Cancel previous search request
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Create new search request with delay
                searchRunnable = () -> performLocationSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Perform location search
     */
    private void performLocationSearch(String query) {
        if (TextUtils.isEmpty(query) || query.length() < 3) {
            // Hide suggestions if query is too short
            rvLocationSuggestions.setVisibility(View.GONE);
            autocompleteAdapter.clearPredictions();
            return;
        }

        // Lazy init LocationSearchHelper only when user actually types in location field
        Log.d(TAG, "User is searching for location, initializing Places API...");

        // Perform search
        getLocationSearchHelper().searchPlaces(query, new LocationSearchHelper.AutocompleteCallback() {
            @Override
            public void onSuccess(java.util.List<AutocompletePrediction> predictions) {
                runOnUiThread(() -> {
                    if (predictions != null && !predictions.isEmpty()) {
                        autocompleteAdapter.updatePredictions(predictions);
                        rvLocationSuggestions.setVisibility(View.VISIBLE);
                    } else {
                        rvLocationSuggestions.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Location search error: " + error);
                    rvLocationSuggestions.setVisibility(View.GONE);
                    Toast.makeText(AddHikeActivity.this,
                            "Failed to search location: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Handle when user selects a place from suggestions
     */
    private void onPlaceSelected(AutocompletePrediction prediction) {
        // Hide suggestions list
        rvLocationSuggestions.setVisibility(View.GONE);

        // Set flag to avoid triggering search
        isSettingLocationProgrammatically = true;
        
        // Display place name in EditText
        etLocation.setText(prediction.getPrimaryText(null));
        
        isSettingLocationProgrammatically = false;

        // Get place details to obtain coordinates
        getLocationSearchHelper().getPlaceDetails(prediction.getPlaceId(), new LocationSearchHelper.PlaceDetailsCallback() {
            @Override
            public void onSuccess(Place place) {
                runOnUiThread(() -> {
                    if (place.getLatLng() != null) {
                        viewModel.setCurrentLocation(place.getLatLng().latitude, place.getLatLng().longitude);

                        // Show control buttons
                        btnResetLocation.setVisibility(View.VISIBLE);
                        btnRefreshLocation.setVisibility(View.VISIBLE);

                        // Update full address if available
                        if (place.getAddress() != null) {
                            isSettingLocationProgrammatically = true;
                            etLocation.setText(place.getAddress());
                            isSettingLocationProgrammatically = false;
                        }

                        Toast.makeText(AddHikeActivity.this,
                                "Location selected: " + place.getName(),
                                Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "Place selected: " + place.getName() +
                                " at " + place.getLatLng().latitude + ", " + place.getLatLng().longitude);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddHikeActivity.this,
                            "Error getting place details: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Place details error: " + error);
                });
            }
        });
    }

    private void setupSpinner() {
        String[] difficulties = {
                Constants.DIFFICULTY_EASY,
                Constants.DIFFICULTY_MEDIUM,
                Constants.DIFFICULTY_HARD,
                Constants.DIFFICULTY_EXTREME
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                difficulties
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        etDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    viewModel.setSelectedDate(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
        fabGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnResetLocation.setOnClickListener(v -> resetLocationData());
        btnRefreshLocation.setOnClickListener(v -> refreshLocationData());
        btnGetWeatherForecast.setOnClickListener(v -> getWeatherForecast());
        btnSelectWeather.setOnClickListener(v -> showWeatherSelectionDialog());
    }

    private void getCurrentLocation() {
        // Only using GPS for CurrentLocation
        Log.d(TAG, "Getting current GPS location (no Places API used)");

        if (!locationHelper.hasLocationPermissions()) {
            locationHelper.requestLocationPermissions(this);
        } else {
            locationHelper.checkLocationSettings(this);
        }
    }

    private void resetLocationData() {
        etLocation.setText("");
        viewModel.setCurrentLocation(0, 0);
        btnResetLocation.setVisibility(View.GONE);
        btnRefreshLocation.setVisibility(View.GONE);
        rvLocationSuggestions.setVisibility(View.GONE);
        autocompleteAdapter.clearPredictions();
        Toast.makeText(this, "Location reset successfully", Toast.LENGTH_SHORT).show();
    }

    private void refreshLocationData() {
        Double latitude = viewModel.getCurrentLatitude().getValue();
        Double longitude = viewModel.getCurrentLongitude().getValue();

        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            Toast.makeText(this, "Refreshing location data...", Toast.LENGTH_SHORT).show();

            // Use Android Geocoder instead of Places API for reverse geocoding
            // Geocoder is free and efficient for this task
            getLocationSearchHelper().reverseGeocode(latitude, longitude,
                    new LocationSearchHelper.ReverseGeocodeCallback() {
                        @Override
                        public void onSuccess(String address, double latitude, double longitude) {
                            runOnUiThread(() -> {
                                // Set flag to avoid triggering search
                                isSettingLocationProgrammatically = true;
                                etLocation.setText(address);
                                isSettingLocationProgrammatically = false;
                                
                                Toast.makeText(AddHikeActivity.this,
                                        "Location refreshed successfully",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(AddHikeActivity.this,
                                        "Error refreshing location: " + error,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } else {
            Toast.makeText(this, "No location data to refresh", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get weather forecast for selected location and date
     */
    private void getWeatherForecast() {
        Double latitude = viewModel.getCurrentLatitude().getValue();
        Double longitude = viewModel.getCurrentLongitude().getValue();
        String selectedDate = viewModel.getSelectedDate().getValue();

        if (latitude == null || longitude == null || latitude == 0 && longitude == 0) {
            Toast.makeText(this, "Please set a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedDate)) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!weatherService.isDateWithinForecastRange(selectedDate)) {
            tvWeatherMessage.setVisibility(View.VISIBLE);
            tvWeatherMessage.setText(R.string.date_beyond_forecast_range);
            return;
        }

        // Use ViewModel to fetch weather
        viewModel.fetchWeatherForecast(latitude, longitude, selectedDate);
    }

    /**
     * Update UI with weather information
     */
    private void updateWeatherUI(WeatherInfo weatherInfo) {
        tvWeatherCondition.setText(weatherInfo.getWeatherConditionString());
        tvWeatherTemp.setText(String.format(Locale.US, "%.1f°C", weatherInfo.getTemperature()));
    }

    /**
     * Show dialog for user to manually select weather conditions
     */
    private void showWeatherSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_weather, null);
        builder.setView(dialogView);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_weather);
        EditText etTemperature = dialogView.findViewById(R.id.et_dialog_temperature);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_ok);

        // Set default temperature if available
        if (currentWeatherInfo != null) {
            etTemperature.setText(String.format(Locale.US, "%.1f", currentWeatherInfo.getTemperature()));

            // Select radio button corresponding to current weather condition
            int weatherCondition = currentWeatherInfo.getWeatherCondition();
            RadioButton radioButton;
            switch (weatherCondition) {
                case WeatherInfo.WEATHER_SUNNY:
                    radioButton = dialogView.findViewById(R.id.radio_sunny);
                    break;
                case WeatherInfo.WEATHER_PARTLY_CLOUDY:
                    radioButton = dialogView.findViewById(R.id.radio_partly_cloudy);
                    break;
                case WeatherInfo.WEATHER_CLOUDY:
                    radioButton = dialogView.findViewById(R.id.radio_cloudy);
                    break;
                case WeatherInfo.WEATHER_RAINY:
                    radioButton = dialogView.findViewById(R.id.radio_rainy);
                    break;
                case WeatherInfo.WEATHER_STORMY:
                    radioButton = dialogView.findViewById(R.id.radio_stormy);
                    break;
                case WeatherInfo.WEATHER_SNOWY:
                    radioButton = dialogView.findViewById(R.id.radio_snowy);
                    break;
                case WeatherInfo.WEATHER_FOGGY:
                    radioButton = dialogView.findViewById(R.id.radio_foggy);
                    break;
                default:
                    radioButton = dialogView.findViewById(R.id.radio_sunny);
                    break;
            }
            radioButton.setChecked(true);
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            // Get temperature from EditText
            String tempString = etTemperature.getText().toString();
            double temperature = 0;
            try {
                if (!TextUtils.isEmpty(tempString)) {
                    temperature = Double.parseDouble(tempString);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(AddHikeActivity.this, "Please enter a valid temperature", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected weather condition
            int weatherCondition;
            int selectedRadioId = radioGroup.getCheckedRadioButtonId();
            if (selectedRadioId == R.id.radio_sunny) {
                weatherCondition = WeatherInfo.WEATHER_SUNNY;
            } else if (selectedRadioId == R.id.radio_partly_cloudy) {
                weatherCondition = WeatherInfo.WEATHER_PARTLY_CLOUDY;
            } else if (selectedRadioId == R.id.radio_cloudy) {
                weatherCondition = WeatherInfo.WEATHER_CLOUDY;
            } else if (selectedRadioId == R.id.radio_rainy) {
                weatherCondition = WeatherInfo.WEATHER_RAINY;
            } else if (selectedRadioId == R.id.radio_stormy) {
                weatherCondition = WeatherInfo.WEATHER_STORMY;
            } else if (selectedRadioId == R.id.radio_snowy) {
                weatherCondition = WeatherInfo.WEATHER_SNOWY;
            } else if (selectedRadioId == R.id.radio_foggy) {
                weatherCondition = WeatherInfo.WEATHER_FOGGY;
            } else {
                weatherCondition = WeatherInfo.WEATHER_SUNNY;
            }

            // Update current weather information
            String currentSelectedDate = viewModel.getSelectedDate().getValue();
            if (TextUtils.isEmpty(currentSelectedDate)) {
                // If no date selected, use current date
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                currentSelectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
                viewModel.setSelectedDate(currentSelectedDate);
            }

            currentWeatherInfo = new WeatherInfo(temperature, weatherCondition, currentSelectedDate);
            updateWeatherUI(currentWeatherInfo);
            tvWeatherMessage.setVisibility(View.GONE);

            dialog.dismiss();

            // Log selected weather data
            Log.d(TAG, "Weather manually selected: " +
                    currentWeatherInfo.getWeatherConditionString() + ", " +
                    currentWeatherInfo.getTemperature() + "°C");
        });

        dialog.show();
    }

    @Override
    public void onLocationReceived(String address, double latitude, double longitude) {
        viewModel.setCurrentLocation(latitude, longitude);

        isSettingLocationProgrammatically = true;
        etLocation.setText(address);
        isSettingLocationProgrammatically = false;

        btnResetLocation.setVisibility(View.VISIBLE);
        btnRefreshLocation.setVisibility(View.VISIBLE);
        rvLocationSuggestions.setVisibility(View.GONE);

        Toast.makeText(this, "Current location updated successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "GPS location received: " + address + " at " + latitude + ", " + longitude);
    }

    @Override
    public void onLocationFailed(String error) {
        Toast.makeText(this, "Error getting current location: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                locationHelper.checkLocationSettings(this);
            } else {
                Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LocationHelper.REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User enabled location settings, get location
                locationHelper.getCurrentLocation();
            } else {
                Toast.makeText(this, "Location settings must be enabled to use this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void validateAndSave() {
        // Clear previous errors
        HikeValidator.clearErrors(tilName, tilLocation, tilLength,
                tilDescription, tilEstimatedDuration);

        boolean isValid = true;

        // Validate required fields using the HikeValidator
        String name = etName.getText().toString().trim();
        if (!HikeValidator.validateRequiredField(tilName, name, "Name")) {
            isValid = false;
        }

        String location = etLocation.getText().toString().trim();
        if (!HikeValidator.validateRequiredField(tilLocation, location, "Location")) {
            isValid = false;
        }

        String selectedDate = viewModel.getSelectedDate().getValue();
        if (!HikeValidator.validateDate(selectedDate)) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String lengthStr = etLength.getText().toString().trim();
        if (!HikeValidator.validateNumericField(tilLength, lengthStr, "Length", 0)) {
            isValid = false;
        }

        // Check weather information
        if (currentWeatherInfo == null) {
            Toast.makeText(this, "Please set weather information", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String estimatedDuration = etEstimatedDuration.getText().toString().trim();
        if (!HikeValidator.validateRequiredField(tilEstimatedDuration, estimatedDuration, "Estimated duration")) {
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_LONG).show();
            return;
        }

        // Create hike object and proceed to confirmation
        proceedToConfirmation();
    }

    private void proceedToConfirmation() {
        Intent intent = new Intent(this, ConfirmHikeActivity.class);

        // Pass all form data to confirmation activity
        intent.putExtra("name", etName.getText().toString().trim());
        intent.putExtra("location", etLocation.getText().toString().trim());
        intent.putExtra("date", viewModel.getSelectedDate().getValue());
        intent.putExtra("length", Double.parseDouble(etLength.getText().toString().trim()));
        intent.putExtra("difficulty", spinnerDifficulty.getSelectedItem().toString());
        intent.putExtra("parking_available", cbParkingAvailable.isChecked());
        intent.putExtra("description", etDescription.getText().toString().trim());
        intent.putExtra("estimated_duration", etEstimatedDuration.getText().toString().trim());

        if (currentWeatherInfo != null) {
            intent.putExtra("weather_condition", currentWeatherInfo.getWeatherConditionString());
            intent.putExtra("temperature", currentWeatherInfo.getTemperature());
        }

        Double latitude = viewModel.getCurrentLatitude().getValue();
        Double longitude = viewModel.getCurrentLongitude().getValue();
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
        }

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates when activity is destroyed
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
