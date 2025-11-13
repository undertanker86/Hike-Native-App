package com.example.hikenativeapp.ui.hike_list;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.ui.adapter.HikeAdapter;
import com.example.hikenativeapp.ui.auth.AuthViewModel;
import com.example.hikenativeapp.ui.auth.LoginActivity;
import com.example.hikenativeapp.ui.add_hike.AddHikeActivity;
import com.example.hikenativeapp.ui.edit_hike.EditHikeActivity;
import com.example.hikenativeapp.ui.observation_list.ObservationListActivity;
import com.example.hikenativeapp.util.Constants;
import com.example.hikenativeapp.util.SwipeToDeleteCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class HikeListActivity extends AppCompatActivity implements HikeAdapter.OnHikeClickListener, SwipeToDeleteCallback.OnSwipeListener {

    private AuthViewModel authViewModel;
    private HikeListViewModel hikeListViewModel;
    private HikeRepository repository;
    private GoogleSignInClient googleSignInClient;
    private TextView tvWelcome;
    private LinearLayout llEmptyState;
    private RecyclerView recyclerView;
    private HikeAdapter hikeAdapter;
    private FloatingActionButton fabAddHike;
    private Toolbar toolbar;

    // Search UI components
    private TextInputEditText etSearchName;
    private TextInputEditText etMinLength;
    private TextInputEditText etMaxLength;
    private Button btnSearch;
    private Button btnClearSearch;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_list);

        initializeComponents();
        setupViewModels();
        setupRecyclerView();
        setupSearchListeners();
        checkAuthentication();
        loadUserInfo();
        loadHikes();
    }

    private void initializeComponents() {
        tvWelcome = findViewById(R.id.tv_welcome);
        llEmptyState = findViewById(R.id.ll_empty_state);
        recyclerView = findViewById(R.id.recycler_view_hikes);
        fabAddHike = findViewById(R.id.fab_add_hike);
        toolbar = findViewById(R.id.toolbar);

        // Search components
        etSearchName = findViewById(R.id.et_search_name);
        etMinLength = findViewById(R.id.et_min_length);
        etMaxLength = findViewById(R.id.et_max_length);
        btnSearch = findViewById(R.id.btn_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        repository = new HikeRepository(this);

        // Get current user ID
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getInt(Constants.PREF_USER_ID, -1);

        // Setup Toolbar as ActionBar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Hikes");
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup FAB click listener
        fabAddHike.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddHikeActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearchListeners() {
        // Real-time search by name as user types
        etSearchName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString().trim();
                if (searchText.isEmpty()) {
                    // If search text is empty, show all hikes
                    loadHikes();
                } else {
                    // Search by name only
                    performSimpleSearch(searchText);
                }
            }
        });

        // Search button - combines name and length search
        btnSearch.setOnClickListener(v -> performSearch());

        // Clear search button
        btnClearSearch.setOnClickListener(v -> clearSearch());
    }

    private void performSimpleSearch(String name) {
        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }
        hikeListViewModel.searchHikesByName(currentUserId, name);
    }

    private void performSearch() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etSearchName.getText() != null ? etSearchName.getText().toString().trim() : "";
        String minLengthStr = etMinLength.getText() != null ? etMinLength.getText().toString().trim() : "";
        String maxLengthStr = etMaxLength.getText() != null ? etMaxLength.getText().toString().trim() : "";

        // If only name is provided, search by name
        if (!name.isEmpty() && minLengthStr.isEmpty() && maxLengthStr.isEmpty()) {
            hikeListViewModel.searchHikesByName(currentUserId, name);
            return;
        }

        // If only length range is provided
        if (name.isEmpty() && (!minLengthStr.isEmpty() || !maxLengthStr.isEmpty())) {
            try {
                double minLength = minLengthStr.isEmpty() ? 0 : Double.parseDouble(minLengthStr);
                double maxLength = maxLengthStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxLengthStr);

                if (minLength > maxLength) {
                    Toast.makeText(this, "Min length cannot be greater than max length", Toast.LENGTH_SHORT).show();
                    return;
                }

                hikeListViewModel.searchHikesByLengthRange(currentUserId, minLength, maxLength);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for length", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // If both name and length are provided
        if (!name.isEmpty() && (!minLengthStr.isEmpty() || !maxLengthStr.isEmpty())) {
            try {
                double minLength = minLengthStr.isEmpty() ? 0 : Double.parseDouble(minLengthStr);
                double maxLength = maxLengthStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxLengthStr);

                if (minLength > maxLength) {
                    Toast.makeText(this, "Min length cannot be greater than max length", Toast.LENGTH_SHORT).show();
                    return;
                }

                hikeListViewModel.searchHikesByNameAndLength(currentUserId, name, minLength, maxLength);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for length", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // If nothing is provided, show all hikes
        loadHikes();
    }

    private void clearSearch() {
        etSearchName.setText("");
        etMinLength.setText("");
        etMaxLength.setText("");
        loadHikes();
    }

    private void setupRecyclerView() {
        hikeAdapter = new HikeAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(hikeAdapter);

        // Setup swipe to delete
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupViewModels() {
        // Initialize AuthViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        authViewModel.setRepository(repository);

        // Initialize HikeListViewModel
        hikeListViewModel = new ViewModelProvider(this).get(HikeListViewModel.class);
        hikeListViewModel.setRepository(repository);

        // Observe authentication state
        authViewModel.getIsLoggedIn().observe(this, isLoggedIn -> {
            if (isLoggedIn != null && !isLoggedIn) {
                navigateToLogin();
            }
        });

        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                tvWelcome.setText("Welcome back, " + user.getDisplayName() + "!");
            }
        });

        // Observe hikes from HikeListViewModel
        hikeListViewModel.getHikes().observe(this, hikes -> {
            if (hikes != null && !hikes.isEmpty()) {
                hikeAdapter.setHikes(hikes);
                llEmptyState.setVisibility(android.view.View.GONE);
                recyclerView.setVisibility(android.view.View.VISIBLE);
            } else {
                llEmptyState.setVisibility(android.view.View.VISIBLE);
                recyclerView.setVisibility(android.view.View.GONE);
            }
        });

        // Observe errors
        hikeListViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAuthentication() {
        if (!authViewModel.isUserSignedIn()) {
            navigateToLogin();
            return;
        }

        // Load current user data
        authViewModel.getCurrentUserFromDatabase();
    }

    private void loadUserInfo() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userName = prefs.getString(Constants.PREF_USER_NAME, "User");
        tvWelcome.setText("Welcome back, " + userName + "!");
    }

    private void loadHikes() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int userId = prefs.getInt(Constants.PREF_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load hikes using ViewModel
        hikeListViewModel.loadHikesByUserId(userId);
    }

    @Override
    public void onHikeClick(Hike hike) {
        // Navigate to ObservationListActivity to view observations for this hike
        Intent intent = new Intent(this, ObservationListActivity.class);
        intent.putExtra("HIKE_ID", hike.getId());
        intent.putExtra("HIKE_NAME", hike.getName());
        startActivity(intent);
    }

    @Override
    public void onHikeEdit(Hike hike) {
        // Navigate to EditHikeActivity
        Intent intent = new Intent(this, EditHikeActivity.class);
        intent.putExtra(EditHikeActivity.EXTRA_HIKE_ID, hike.getId());
        startActivity(intent);
    }

    @Override
    public void onHikeMap(Hike hike) {
        // Check if coordinates are valid
        if (hike.getLatitude() == 0.0 && hike.getLongitude() == 0.0) {
            Toast.makeText(this, "No location data available for this hike", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create URI for Google Maps with coordinates
        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                hike.getLatitude(),
                hike.getLongitude(),
                hike.getLatitude(),
                hike.getLongitude(),
                Uri.encode(hike.getName()));

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // If Google Maps not installed, open in browser
            String browserUri = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    hike.getLatitude(), hike.getLongitude());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
            startActivity(browserIntent);
        }
    }

    @Override
    public void onHikeDelete(Hike hike, int position) {
        // Show confirmation dialog for soft delete
        new AlertDialog.Builder(this)
                .setTitle("Delete Hike")
                .setMessage("Are you sure you want to delete \"" + hike.getName())
                .setPositiveButton("Delete", (dialog, which) -> {
                    softDeleteHike(hike, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSwipe(int position) {
        Hike hikeToDelete = hikeAdapter.getItem(position);
        if (hikeToDelete == null) return;

        // Show confirmation dialog for soft delete
        new AlertDialog.Builder(this)
                .setTitle("Delete Hike")
                .setMessage("Are you sure you want to delete \"" + hikeToDelete.getName())
                .setPositiveButton("Delete", (dialog, which) -> {
                    softDeleteHike(hikeToDelete, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Restore the item
                    hikeAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    // Restore the item if dialog is cancelled
                    hikeAdapter.notifyItemChanged(position);
                })
                .show();
    }

    private void softDeleteHike(Hike hike, int position) {
        // Remove from adapter immediately for UX
        hikeAdapter.removeItem(position);

        // Perform soft delete using ViewModel
        hikeListViewModel.softDeleteHike(hike.getId());

        Toast.makeText(this, "Hike deleted successfully", Toast.LENGTH_SHORT).show();

        // Check if list is empty and show no hikes message
        if (hikeAdapter.getItemCount() == 0) {
            llEmptyState.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        }
    }


    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload hikes when returning to this activity
        loadHikes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Don't inflate menu - remove logout option
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle only back button
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        // Clear shared preferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Sign out from Firebase
        authViewModel.signOut();

        // Sign out from Google
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                .addOnCompleteListener(this, task -> {
                    // Navigate to login
                    navigateToLogin();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModels will be cleared automatically
    }
}
