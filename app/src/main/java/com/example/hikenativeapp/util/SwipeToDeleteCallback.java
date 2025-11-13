package com.example.hikenativeapp.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private Drawable deleteIcon;
    private int intrinsicWidth;
    private int intrinsicHeight;
    private ColorDrawable background;
    private int backgroundColor;
    private OnSwipeListener onSwipeListener;

    public interface OnSwipeListener {
        void onSwipe(int position);
    }

    public SwipeToDeleteCallback(Context context, OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.onSwipeListener = listener;

        // Initialize delete icon and background
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white);
        if (deleteIcon != null) {
            intrinsicWidth = deleteIcon.getIntrinsicWidth();
            intrinsicHeight = deleteIcon.getIntrinsicHeight();
        }

        backgroundColor = Color.parseColor("#f44336"); // Red color
        background = new ColorDrawable();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (onSwipeListener != null) {
            onSwipeListener.onSwipe(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        // Draw red background
        background.setColor(backgroundColor);
        background.setBounds(
            itemView.getRight() + ((int) dX),
            itemView.getTop(),
            itemView.getRight(),
            itemView.getBottom()
        );
        background.draw(c);

        // Draw delete icon
        if (deleteIcon != null) {
            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
            int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int deleteIconRight = itemView.getRight() - deleteIconMargin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;

            deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
            deleteIcon.draw(c);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.7f; // Require 70% swipe to trigger delete
    }
}
