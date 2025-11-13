package com.example.hikenativeapp.ui.add_observation;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.repository.ObservationRepository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddObservationActivity extends AppCompatActivity {

    private EditText etObservationText, etObservationTime, etComments;
    private Button btnSave, btnCancel, btnSelectTime, btnTakePhoto, btnRemovePhoto, btnSelectPhoto;
    private ImageView ivObservationPhoto;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private AddObservationViewModel viewModel;
    private ObservationRepository repository;
    private int hikeId;
    private String hikeName;
    private Calendar selectedDateTime;
    private String currentPhotoPath;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<String> selectPictureLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_observation);

        // Get hike ID from intent
        hikeId = getIntent().getIntExtra("HIKE_ID", -1);
        hikeName = getIntent().getStringExtra("HIKE_NAME");

        if (hikeId == -1) {
            Toast.makeText(this, "Invalid hike", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeActivityResultLaunchers();
        initializeComponents();
        setupViewModel();
        setupListeners();
        observeViewModel();

    }



    private void initializeActivityResultLaunchers() {
        // Camera launcher
        takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Photo saved to currentPhotoPath
                    displayPhoto();
                }
            }
        );

        // Permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Camera and storage permissions are required", Toast.LENGTH_SHORT).show();
                }
            }
        );

        selectPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    // Callback when user finishes selecting an image
                    if (uri != null) {
                        this.currentPhotoPath = uri.toString();
                        displayPhoto();
                    }
                }
        );
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        etObservationText = findViewById(R.id.et_observation_text);
        etObservationTime = findViewById(R.id.et_observation_time);
        etComments = findViewById(R.id.et_comments);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnRemovePhoto = findViewById(R.id.btn_remove_photo);
        ivObservationPhoto = findViewById(R.id.iv_observation_photo);
        progressBar = findViewById(R.id.progress_bar);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        repository = new ObservationRepository(this);
        selectedDateTime = Calendar.getInstance();

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Observation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set current time as default
        updateTimeDisplay();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AddObservationViewModel.class);
        viewModel.setRepository(repository);
    }

    private void observeViewModel() {
        // Observe save success
        viewModel.saveSuccess.observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Observation added successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Observe error messages
        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe loading state
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    progressBar.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                }
            }
        });
    }

    private void setupListeners() {
        btnSelectTime.setOnClickListener(v -> showDateTimePicker());

        btnSave.setOnClickListener(v -> saveObservation());

        btnCancel.setOnClickListener(v -> finish());

        toolbar.setNavigationOnClickListener(v -> finish());

        btnTakePhoto.setOnClickListener(v -> checkCameraAndStoragePermissions());

        btnSelectPhoto.setOnClickListener(v -> selectPictureLauncher.launch("image/*"));

        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }

    private void showDateTimePicker() {
        // Show date picker first
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                // Then show time picker
                showTimePicker();
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateTimeDisplay();
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }

    private void updateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        etObservationTime.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void checkCameraAndStoragePermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            dispatchTakePictureIntent();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "OBSERVATION_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void displayPhoto() {
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            // Glide can handle both File paths and Content URIs
            Glide.with(this)
                    .load(currentPhotoPath)
                    .centerCrop()
                    .into(ivObservationPhoto);

            ivObservationPhoto.setVisibility(View.VISIBLE);
            btnRemovePhoto.setVisibility(View.VISIBLE);
        }
    }

    private void removePhoto() {
        // Only delete file if it's a file path (not a content URI)
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty() && !currentPhotoPath.startsWith("content://")) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) {
                photoFile.delete();
            }
        }

        currentPhotoPath = null;
        ivObservationPhoto.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.GONE);

        // Clear the ImageView
        Glide.with(this).clear(ivObservationPhoto);
    }

    private void saveObservation() {
        String observationText = etObservationText.getText().toString().trim();
        String observationTime = etObservationTime.getText().toString().trim();
        String comments = etComments.getText().toString().trim();

        // Validation through ViewModel
        if (!viewModel.validateObservation(observationText, observationTime)) {
            if (observationText.isEmpty()) {
                etObservationText.setError("Observation text is required");
                etObservationText.requestFocus();
            } else if (observationTime.isEmpty()) {
                etObservationTime.setError("Observation time is required");
                etObservationTime.requestFocus();
            }
            return;
        }

        // Create observation object
        Observation observation = new Observation();
        observation.setHikeId(hikeId);
        observation.setObservationText(observationText);
        observation.setObservationTime(observationTime);
        observation.setComments(comments);
        observation.setPhotoPath(currentPhotoPath); // Set photo path

        // Save through ViewModel
        viewModel.saveObservation(observation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
    }
}
