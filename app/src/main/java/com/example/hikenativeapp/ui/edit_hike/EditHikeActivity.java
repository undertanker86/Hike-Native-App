package com.example.hikenativeapp.ui.edit_hike;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.data.weather.WeatherInfo;
import com.example.hikenativeapp.ui.adapter.PlaceAutocompleteAdapter;
import com.example.hikenativeapp.util.Constants;
import com.example.hikenativeapp.util.LocationHelper;
import com.example.hikenativeapp.util.LocationSearchHelper;
import com.example.hikenativeapp.val.HikeValidator;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditHikeActivity extends AppCompatActivity implements LocationHelper.LocationListener {

    private static final String TAG = "EditHikeActivity";
    public static final String EXTRA_HIKE_ID = "hike_id";
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

    private EditHikeViewModel viewModel;
    private Hike currentHike;
    private int hikeId = -1;
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
        setContentView(R.layout.activity_edit_hike);

        // Get hike ID from intent
        hikeId = getIntent().getIntExtra(EXTRA_HIKE_ID, -1);
        if (hikeId == -1) {
            Toast.makeText(this, "Error: Invalid hike ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(EditHikeViewModel.class);
        HikeRepository repository = new HikeRepository(this);
        viewModel.setRepository(repository);

        // Setup WeatherService
        weatherService = WeatherService.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = prefs.getString(Constants.PREF_WEATHER_API_KEY, Constants.STORMGLASS_API_KEY);
        weatherService.setApiKey(apiKey);

        // Initialize location helper
        locationHelper = new LocationHelper(this);
        locationHelper.setLocationListener(this);

        initializeViews();
        setupViewModel();
        setupSpinner();
        setupDatePicker();
        setupLocationSearch();
        setupWeatherButtons();
        setupClickListeners();

        // Load hike data
        viewModel.loadHike(hikeId);
    }

    private void initializeViews() {
        tilName = findViewById(R.id.til_name);
        tilLocation = findViewById(R.id.til_location);
        tilLength = findViewById(R.id.til_length);
        tilDescription = findViewById(R.id.til_description);
        tilEstimatedDuration = findViewById(R.id.til_estimated_duration);

        etName = findViewById(R.id.et_name);
        etLocation = findViewById(R.id.et_location);
        etLength = findViewById(R.id.et_length);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        etEstimatedDuration = findViewById(R.id.et_estimated_duration);

        cbParkingAvailable = findViewById(R.id.cb_parking_available);
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        btnSave = findViewById(R.id.btn_save);
        btnSelectDate = findViewById(R.id.btn_select_date);

        tvWeatherCondition = findViewById(R.id.tv_weather_condition);
        tvWeatherTemp = findViewById(R.id.tv_weather_temp);
        tvWeatherMessage = findViewById(R.id.tv_weather_message);
        btnGetWeatherForecast = findViewById(R.id.btn_get_weather_forecast);
        btnSelectWeather = findViewById(R.id.btn_select_weather);

        fabGetCurrentLocation = findViewById(R.id.fab_get_current_location);
        btnResetLocation = findViewById(R.id.btn_reset_location);
        btnRefreshLocation = findViewById(R.id.btn_refresh_location);
        rvLocationSuggestions = findViewById(R.id.rv_location_suggestions);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Hike");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupViewModel() {
        viewModel.getHike().observe(this, hike -> {
            if (hike != null) {
                currentHike = hike;
                populateFields(hike);
            } else {
                Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getUpdateSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Hike updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (success != null) {
                Toast.makeText(this, "Failed to update hike", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            btnSave.setEnabled(!isLoading);
        });

        // Observe weather info updates
        viewModel.getWeatherInfo().observe(this, weatherInfo -> {
            if (weatherInfo != null) {
                currentWeatherInfo = weatherInfo;
                updateWeatherUI(weatherInfo);
            }
        });

        // Observe weather loading state
        viewModel.getIsLoadingWeather().observe(this, isLoading -> {
            if (isLoading != null) {
                btnGetWeatherForecast.setEnabled(!isLoading);
                if (isLoading) {
                    tvWeatherCondition.setText("Loading weather...");
                }
            }
        });
    }

    private void populateFields(Hike hike) {
        isSettingLocationProgrammatically = true;

        etName.setText(hike.getName());
        etLocation.setText(hike.getLocation());
        etDate.setText(hike.getHikeDate());
        etLength.setText(String.valueOf(hike.getLength()));
        etDescription.setText(hike.getDescription());
        cbParkingAvailable.setChecked(hike.isParkingAvailable());


        // Set estimated duration
        if (hike.getEstimatedDuration() > 0) {
            etEstimatedDuration.setText(String.valueOf(hike.getEstimatedDuration()));
        }

        // Set weather info if available
        if (hike.getWeatherCondition() != null && !hike.getWeatherCondition().isEmpty()) {
            tvWeatherCondition.setText(hike.getWeatherCondition());
            if (hike.getTemperature() > 0) {
                tvWeatherTemp.setText(String.format(Locale.US, "%.1f°C", hike.getTemperature()));
            }
            // Recreate WeatherInfo object for later use
            currentWeatherInfo = new WeatherInfo(hike.getTemperature(),
                    convertWeatherStringToCode(hike.getWeatherCondition()), hike.getHikeDate());
        }

        String[] difficulties = {
                Constants.DIFFICULTY_EASY,
                Constants.DIFFICULTY_MEDIUM,
                Constants.DIFFICULTY_HARD,
                Constants.DIFFICULTY_EXTREME
        };

        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i].equals(hike.getDifficulty())) {
                spinnerDifficulty.setSelection(i);
                break;
            }
        }

        isSettingLocationProgrammatically = false;
    }

    private int convertWeatherStringToCode(String weatherString) {
        if (weatherString == null) return WeatherInfo.WEATHER_SUNNY;
        switch (weatherString.toLowerCase()) {
            case "sunny":
            case "clear":
                return WeatherInfo.WEATHER_SUNNY;
            case "partly cloudy":
                return WeatherInfo.WEATHER_PARTLY_CLOUDY;
            case "cloudy":
                return WeatherInfo.WEATHER_CLOUDY;
            case "rainy":
            case "rain":
                return WeatherInfo.WEATHER_RAINY;
            case "stormy":
            case "thunderstorm":
                return WeatherInfo.WEATHER_STORMY;
            case "snowy":
            case "snow":
                return WeatherInfo.WEATHER_SNOWY;
            case "foggy":
            case "fog":
                return WeatherInfo.WEATHER_FOGGY;
            default:
                return WeatherInfo.WEATHER_SUNNY;
        }
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

        if (currentHike != null && !TextUtils.isEmpty(currentHike.getHikeDate())) {
            try {
                String[] dateParts = currentHike.getHikeDate().split("-");
                if (dateParts.length == 3) {
                    calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
                }
            } catch (Exception e) {
                // Use current date if parsing fails
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    etDate.setText(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void setupLocationSearch() {
        locationSearchHelper = new LocationSearchHelper(this);

        // Create adapter with only the listener parameter
        autocompleteAdapter = new PlaceAutocompleteAdapter(prediction -> {
            isSettingLocationProgrammatically = true;
            etLocation.setText(prediction.getPrimaryText(null).toString());
            rvLocationSuggestions.setVisibility(RecyclerView.GONE);

            // Fetch place details to get lat/lng
            locationSearchHelper.getPlaceDetails(prediction.getPlaceId(), new LocationSearchHelper.PlaceDetailsCallback() {
                @Override
                public void onSuccess(Place place) {
                    if (place.getLatLng() != null) {
                        double lat = place.getLatLng().latitude;
                        double lng = place.getLatLng().longitude;
                        if (currentHike != null) {
                            currentHike.setLatitude(lat);
                            currentHike.setLongitude(lng);
                        }
                    }
                    isSettingLocationProgrammatically = false;
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(EditHikeActivity.this, error, Toast.LENGTH_SHORT).show();
                    isSettingLocationProgrammatically = false;
                }
            });
        });

        rvLocationSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvLocationSuggestions.setAdapter(autocompleteAdapter);

        etLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSettingLocationProgrammatically) return;

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (s.length() > 2) {
                    searchRunnable = () -> searchLocation(s.toString());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    rvLocationSuggestions.setVisibility(RecyclerView.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnResetLocation.setOnClickListener(v -> resetLocation());
        btnRefreshLocation.setOnClickListener(v -> refreshLocation());
    }

    private void searchLocation(String query) {
        locationSearchHelper.searchPlaces(query, new LocationSearchHelper.AutocompleteCallback() {
            @Override
            public void onSuccess(List<AutocompletePrediction> predictions) {
                autocompleteAdapter.updatePredictions(predictions);
                rvLocationSuggestions.setVisibility(predictions.isEmpty() ? RecyclerView.GONE : RecyclerView.VISIBLE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditHikeActivity.this, "Search error: " + error, Toast.LENGTH_SHORT).show();
                rvLocationSuggestions.setVisibility(RecyclerView.GONE);
            }
        });
    }

    private void getCurrentLocation() {
        if (!locationHelper.hasLocationPermissions()) {
            locationHelper.requestLocationPermissions(this);
            return;
        }
        locationHelper.getCurrentLocation();
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
    }

    private void resetLocation() {
        etLocation.setText("");
        if (currentHike != null) {
            currentHike.setLatitude(0);
            currentHike.setLongitude(0);
        }
        rvLocationSuggestions.setVisibility(RecyclerView.GONE);
    }

    private void refreshLocation() {
        String currentLocation = etLocation.getText().toString().trim();
        if (!currentLocation.isEmpty()) {
            searchLocation(currentLocation);
        }
    }

    private void setupWeatherButtons() {
        btnGetWeatherForecast.setOnClickListener(v -> getWeatherForecast());
        btnSelectWeather.setOnClickListener(v -> showWeatherSelectionDialog());
    }

    private void getWeatherForecast() {
        String date = etDate.getText().toString().trim();


        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentHike == null || currentHike.getLatitude() == 0 || currentHike.getLongitude() == 0) {
            Toast.makeText(this, "Please select a valid location first", Toast.LENGTH_SHORT).show();
            return;
        }


        Toast.makeText(this, "Getting weather forecast...", Toast.LENGTH_SHORT).show();
        // Use ViewModel to fetch weather
        viewModel.fetchWeatherForecast(currentHike.getLatitude(), currentHike.getLongitude(), date);
    }

    private void showWeatherSelectionDialog() {
        String[] weatherOptions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Stormy", "Snowy", "Foggy"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Weather Condition");
        builder.setItems(weatherOptions, (dialog, which) -> {
            String selectedWeather = weatherOptions[which];
            tvWeatherCondition.setText(selectedWeather);

            // Create weather info
            int weatherCondition;
            switch (which) {
                case 0:
                    weatherCondition = WeatherInfo.WEATHER_SUNNY;
                    break;
                case 1:
                    weatherCondition = WeatherInfo.WEATHER_PARTLY_CLOUDY;
                    break;
                case 2:
                    weatherCondition = WeatherInfo.WEATHER_CLOUDY;
                    break;
                case 3:
                    weatherCondition = WeatherInfo.WEATHER_RAINY;
                    break;
                case 4:
                    weatherCondition = WeatherInfo.WEATHER_STORMY;
                    break;
                case 5:
                    weatherCondition = WeatherInfo.WEATHER_SNOWY;
                    break;
                case 6:
                    weatherCondition = WeatherInfo.WEATHER_FOGGY;
                    break;
                default:
                    weatherCondition = WeatherInfo.WEATHER_SUNNY;
                    break;
            }

            currentWeatherInfo = new WeatherInfo(20.0, weatherCondition, etDate.getText().toString());
            tvWeatherTemp.setText("20.0°C");
        });
        builder.show();
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        HikeValidator.clearErrors(tilName, tilLocation, tilLength, tilDescription, tilEstimatedDuration);

        boolean isValid = true;

        String name = etName.getText().toString().trim();
        if (!HikeValidator.validateRequiredField(tilName, name, "Name")) {
            isValid = false;
        }

        String location = etLocation.getText().toString().trim();
        if (!HikeValidator.validateRequiredField(tilLocation, location, "Location")) {
            isValid = false;
        }

        String date = etDate.getText().toString().trim();
        if (!HikeValidator.validateDate(date)) {
            Toast.makeText(this, "Please select a valid date", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String lengthStr = etLength.getText().toString().trim();
        if (!HikeValidator.validateNumericField(tilLength, lengthStr, "Length", 0)) {
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

        // Update hike object
        currentHike.setName(name);
        currentHike.setLocation(location);
        currentHike.setHikeDate(date);
        currentHike.setLength(Double.parseDouble(lengthStr));
        currentHike.setDifficulty(spinnerDifficulty.getSelectedItem().toString());
        currentHike.setParkingAvailable(cbParkingAvailable.isChecked());
        currentHike.setDescription(etDescription.getText().toString().trim());

        // Update weather and duration
        if (currentWeatherInfo != null) {
            currentHike.setWeatherCondition(currentWeatherInfo.getWeatherConditionString());
            currentHike.setTemperature(currentWeatherInfo.getTemperature());
        }

        try {
            currentHike.setEstimatedDuration(Double.parseDouble(estimatedDuration));
        } catch (NumberFormatException e) {
            currentHike.setEstimatedDuration(0.0);
        }

        viewModel.updateHike(currentHike);
    }

    private void updateWeatherUI(WeatherInfo weatherInfo) {
        tvWeatherCondition.setText(weatherInfo.getWeatherConditionString());
        tvWeatherTemp.setText(String.format(Locale.US, "%.1f°C", weatherInfo.getTemperature()));
    }

    @Override
    public void onLocationReceived(String address, double latitude, double longitude) {
        if (address != null && !address.isEmpty()) {
            isSettingLocationProgrammatically = true;
            etLocation.setText(address);
            isSettingLocationProgrammatically = false;
        }
        if (currentHike != null) {
            currentHike.setLatitude(latitude);
            currentHike.setLongitude(longitude);
        }
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationFailed(String error) {
        Toast.makeText(this, "Location failed: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
