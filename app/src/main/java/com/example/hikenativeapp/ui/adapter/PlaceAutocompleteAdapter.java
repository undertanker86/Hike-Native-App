package com.example.hikenativeapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to display list of address suggestions from Places Autocomplete
 */
public class PlaceAutocompleteAdapter extends RecyclerView.Adapter<PlaceAutocompleteAdapter.ViewHolder> {

    private List<AutocompletePrediction> predictions;
    private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(AutocompletePrediction prediction);
    }

    public PlaceAutocompleteAdapter(OnPlaceClickListener listener) {
        this.predictions = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_autocomplete, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutocompletePrediction prediction = predictions.get(position);

        // Hiển thị tên chính của địa điểm
        holder.primaryText.setText(prediction.getPrimaryText(null));

        // Hiển thị mô tả phụ (địa chỉ)
        holder.secondaryText.setText(prediction.getSecondaryText(null));

        // Xử lý click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceClick(prediction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    /**
     * Update suggestion list
     */
    public void updatePredictions(List<AutocompletePrediction> newPredictions) {
        this.predictions.clear();
        if (newPredictions != null) {
            this.predictions.addAll(newPredictions);
        }
        notifyDataSetChanged();
    }

    /**
     * Delete all suggestions
     */
    public void clearPredictions() {
        this.predictions.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView primaryText;
        TextView secondaryText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            primaryText = itemView.findViewById(R.id.tv_primary_text);
            secondaryText = itemView.findViewById(R.id.tv_secondary_text);
        }
    }
}
