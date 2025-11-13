package com.example.hikenativeapp.util;

import android.content.Context;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.local.entity.User;

import java.util.Arrays;
import java.util.List;

public class DatabaseHelper {

    // Difficulty levels validation
    public static final List<String> VALID_DIFFICULTIES = Arrays.asList(
            Constants.DIFFICULTY_EASY,
            Constants.DIFFICULTY_MEDIUM,
            Constants.DIFFICULTY_HARD,
            Constants.DIFFICULTY_EXTREME
    );

    // Validation methods
    public static String getDifficultyColor(String difficulty) {
        switch (difficulty) {
            case Constants.DIFFICULTY_EASY:
                return "#4CAF50"; // Green
            case Constants.DIFFICULTY_MEDIUM:
                return "#FF9800"; // Orange
            case Constants.DIFFICULTY_HARD:
                return "#F44336"; // Red
            case Constants.DIFFICULTY_EXTREME:
                return "#9C27B0"; // Purple
            default:
                return "#757575"; // Grey
        }
    }



}
