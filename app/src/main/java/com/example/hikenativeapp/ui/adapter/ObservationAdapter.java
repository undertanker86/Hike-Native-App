package com.example.hikenativeapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.entity.Observation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder> {

    private List<Observation> observations = new ArrayList<>();
    private OnObservationClickListener onObservationClickListener;

    public interface OnObservationClickListener {
        void onObservationClick(Observation observation);
        void onObservationEdit(Observation observation);
        void onObservationDelete(Observation observation, int position);
    }

    public ObservationAdapter(OnObservationClickListener listener) {
        this.onObservationClickListener = listener;
    }

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        Observation observation = observations.get(position);
        holder.bind(observation);
    }

    @Override
    public int getItemCount() {
        return observations.size();
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations != null ? observations : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < observations.size()) {
            observations.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Observation getItem(int position) {
        if (position >= 0 && position < observations.size()) {
            return observations.get(position);
        }
        return null;
    }

    class ObservationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvObservationText, tvObservationTime, tvComments;
        private ImageView ivPhoto;
        private ImageButton btnEdit;
        private View llPhotoContainer, layoutComments;

        public ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvObservationText = itemView.findViewById(R.id.tv_observation_text);
            tvObservationTime = itemView.findViewById(R.id.tv_observation_time);
            tvComments = itemView.findViewById(R.id.tv_observation_comments);
            ivPhoto = itemView.findViewById(R.id.iv_observation_photo);
            btnEdit = itemView.findViewById(R.id.btn_edit_observation);
            llPhotoContainer = itemView.findViewById(R.id.ll_photo_container);
            layoutComments = itemView.findViewById(R.id.layout_comments);

            itemView.setOnClickListener(v -> {
                if (onObservationClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onObservationClickListener.onObservationClick(observations.get(position));
                    }
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (onObservationClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onObservationClickListener.onObservationEdit(observations.get(position));
                    }
                }
            });
        }

        public void bind(Observation observation) {
            tvObservationText.setText(observation.getObservationText() != null ?
                observation.getObservationText() : "No observation text");
            tvObservationTime.setText(observation.getObservationTime() != null ?
                observation.getObservationTime() : "");

            // Handle comments visibility
            if (observation.getComments() != null && !observation.getComments().isEmpty()) {
                layoutComments.setVisibility(View.VISIBLE);
                tvComments.setText(observation.getComments());
            } else {
                layoutComments.setVisibility(View.GONE);
            }
            String photoPath = observation.getPhotoPath();
            // Handle photo display with Glide
            if (observation.getPhotoPath() != null && !observation.getPhotoPath().isEmpty()) {

                if(photoPath != null && !photoPath.isEmpty()){
                    llPhotoContainer.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(photoPath)
                            .centerCrop()
                            .placeholder(R.drawable.ic_placeholder_image)
                            .error(R.drawable.ic_placeholder_image)
                            .into(ivPhoto);
                }
                else {
                    llPhotoContainer.setVisibility(View.GONE);
                }
            } else {
                llPhotoContainer.setVisibility(View.GONE);
            }
        }
    }
}
