package com.example.hikenativeapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.entity.Hike;

import java.util.ArrayList;
import java.util.List;

public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

    private List<Hike> hikes = new ArrayList<>();
    private OnHikeClickListener onHikeClickListener;

    public interface OnHikeClickListener {
        void onHikeClick(Hike hike);
        void onHikeEdit(Hike hike);
        void onHikeDelete(Hike hike, int position);
        void onHikeMap(Hike hike);
    }

    public HikeAdapter(OnHikeClickListener listener) {
        this.onHikeClickListener = listener;
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hike, parent, false);
        return new HikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        Hike hike = hikes.get(position);
        holder.bind(hike);
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    public void setHikes(List<Hike> hikes) {
        this.hikes = hikes != null ? hikes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < hikes.size()) {
            hikes.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Hike getItem(int position) {
        if (position >= 0 && position < hikes.size()) {
            return hikes.get(position);
        }
        return null;
    }

    class HikeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvLocation, tvDate, tvLength, tvDifficulty, tvParking;
        private ImageButton btnEdit, btnMap;
        private ImageView ivParkingIcon;

        public HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_hike_name);
            tvLocation = itemView.findViewById(R.id.tv_hike_location);
            tvDate = itemView.findViewById(R.id.tv_hike_date);
            tvLength = itemView.findViewById(R.id.tv_hike_length);
            tvDifficulty = itemView.findViewById(R.id.tv_hike_difficulty);
            tvParking = itemView.findViewById(R.id.tv_hike_parking);
            btnEdit = itemView.findViewById(R.id.btn_edit_hike);
            btnMap = itemView.findViewById(R.id.btn_map);
            ivParkingIcon = itemView.findViewById(R.id.iv_parking_icon);

            itemView.setOnClickListener(v -> {
                if (onHikeClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onHikeClickListener.onHikeClick(hikes.get(position));
                    }
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (onHikeClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onHikeClickListener.onHikeEdit(hikes.get(position));
                    }
                }
            });

            btnMap.setOnClickListener(v -> {
                if (onHikeClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onHikeClickListener.onHikeMap(hikes.get(position));
                    }
                }
            });
        }

        public void bind(Hike hike) {
            tvName.setText(hike.getName());
            tvLocation.setText(hike.getLocation());
            tvDate.setText(hike.getHikeDate());
            tvLength.setText(String.format("%.1f km", hike.getLength()));
            tvDifficulty.setText(hike.getDifficulty());

            // Update parking display with icon
            if (hike.isParkingAvailable()) {
                tvParking.setText("Parking");
                ivParkingIcon.setImageResource(android.R.drawable.checkbox_on_background);
                ivParkingIcon.setColorFilter(itemView.getContext().getColor(R.color.success));
            } else {
                tvParking.setText("No Parking");
                ivParkingIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivParkingIcon.setColorFilter(itemView.getContext().getColor(R.color.text_light));
            }

            // Set difficulty background color based on level
            int difficultyColor;
            String difficulty = hike.getDifficulty().toLowerCase();
            if (difficulty.contains("easy")) {
                difficultyColor = itemView.getContext().getColor(R.color.difficulty_easy);
            } else if (difficulty.contains("moderate")) {
                difficultyColor = itemView.getContext().getColor(R.color.difficulty_moderate);
            } else if (difficulty.contains("hard")) {
                difficultyColor = itemView.getContext().getColor(R.color.difficulty_hard);
            } else {
                difficultyColor = itemView.getContext().getColor(R.color.primary_green);
            }
            tvDifficulty.setBackgroundColor(difficultyColor);
        }
    }
}
