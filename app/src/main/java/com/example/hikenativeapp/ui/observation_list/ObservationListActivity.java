package com.example.hikenativeapp.ui.observation_list;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.repository.ObservationRepository;
import com.example.hikenativeapp.ui.adapter.ObservationAdapter;
import com.example.hikenativeapp.ui.add_observation.AddObservationActivity;
import com.example.hikenativeapp.ui.edit_observation.EditObservationActivity;
import com.example.hikenativeapp.util.SwipeToDeleteCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ObservationListActivity extends AppCompatActivity
        implements ObservationAdapter.OnObservationClickListener, SwipeToDeleteCallback.OnSwipeListener {

    private ObservationListViewModel viewModel;
    private ObservationRepository repository;
    private RecyclerView recyclerView;
    private ObservationAdapter observationAdapter;
    private FloatingActionButton fabAddObservation;
    private Toolbar toolbar;
    private TextView tvEmptyState;
    private LinearLayout llEmptyState;

    private int hikeId;
    private String hikeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation_list);

        // Get hike ID from intent
        hikeId = getIntent().getIntExtra("HIKE_ID", -1);
        hikeName = getIntent().getStringExtra("HIKE_NAME");

        if (hikeId == -1) {
            Toast.makeText(this, "Invalid hike", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeComponents();
        setupViewModels();
        setupRecyclerView();
        loadObservations();
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_observations);
        fabAddObservation = findViewById(R.id.fab_add_observation);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        llEmptyState = findViewById(R.id.ll_empty_state);

        repository = new ObservationRepository(this);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(hikeName != null ? hikeName : "Observations");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup FAB click listener
        fabAddObservation.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddObservationActivity.class);
            intent.putExtra("HIKE_ID", hikeId);
            intent.putExtra("HIKE_NAME", hikeName);
            startActivity(intent);
        });
    }

    private void setupViewModels() {
        viewModel = new ViewModelProvider(this).get(ObservationListViewModel.class);
        viewModel.setRepository(repository);
    }

    private void setupRecyclerView() {
        observationAdapter = new ObservationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(observationAdapter);

        // Setup swipe to delete
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void loadObservations() {
        new Thread(() -> {
            List<Observation> observations = viewModel.getObservationsByHikeId(hikeId);
            runOnUiThread(() -> {
                if (observations != null && !observations.isEmpty()) {
                    observationAdapter.setObservations(observations);
                    recyclerView.setVisibility(android.view.View.VISIBLE);
                    llEmptyState.setVisibility(android.view.View.GONE);
                } else {
                    recyclerView.setVisibility(android.view.View.GONE);
                    llEmptyState.setVisibility(android.view.View.VISIBLE);
                }
            });
        }).start();
    }

    @Override
    public void onObservationClick(Observation observation) {
        // View observation details - can be implemented later
        Intent intent = new Intent(this, EditObservationActivity.class);
        intent.putExtra("OBSERVATION_ID", observation.getId());
        intent.putExtra("HIKE_ID", hikeId);
        intent.putExtra("HIKE_NAME", hikeName);
        intent.putExtra("VIEW_MODE", true);
        startActivity(intent);
    }

    @Override
    public void onObservationEdit(Observation observation) {
        Intent intent = new Intent(this, EditObservationActivity.class);
        intent.putExtra("OBSERVATION_ID", observation.getId());
        intent.putExtra("HIKE_ID", hikeId);
        intent.putExtra("HIKE_NAME", hikeName);
        startActivity(intent);
    }

    @Override
    public void onObservationDelete(Observation observation, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Observation")
                .setMessage("Are you sure you want to delete this observation?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteObservation(observation, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteObservation(Observation observation, int position) {
        new Thread(() -> {
            viewModel.deleteObservation(observation.getId());
            runOnUiThread(() -> {
                observationAdapter.removeItem(position);
                Toast.makeText(this, "Observation deleted", Toast.LENGTH_SHORT).show();

                // Check if list is empty
                if (observationAdapter.getItemCount() == 0) {
                    recyclerView.setVisibility(android.view.View.GONE);
                    llEmptyState.setVisibility(android.view.View.VISIBLE);
                }
            });
        }).start();
    }

    @Override
    public void onSwipe(int position) {
        Observation observation = observationAdapter.getItem(position);
        if (observation != null) {
            onObservationDelete(observation, position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadObservations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
    }
}
