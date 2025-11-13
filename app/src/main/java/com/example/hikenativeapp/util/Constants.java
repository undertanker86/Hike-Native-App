package com.example.hikenativeapp.util;
import com.example.hikenativeapp.BuildConfig;
public class Constants {

    // Database constants


    // Difficulty levels
    public static final String DIFFICULTY_EASY = "Easy";
    public static final String DIFFICULTY_MEDIUM = "Medium";
    public static final String DIFFICULTY_HARD = "Hard";
    public static final String DIFFICULTY_EXTREME = "Extreme";

    // Shared Preferences keys
    public static final String PREFS_NAME = "HikeAppPrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_GOOGLE_ID = "google_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_WEATHER_API_KEY = "weather_api_key";




    // Weather API constants
    public static final String STORMGLASS_API_KEY = BuildConfig.WEATHER_API_KEY;

    public static final int MAX_FORECAST_DAYS = 16; // StormGlass forecast limit

    // Weather conditions
    public static final int WEATHER_SUNNY = 0;
    public static final int WEATHER_PARTLY_CLOUDY = 1;
    public static final int WEATHER_CLOUDY = 2;
    public static final int WEATHER_RAINY = 3;
    public static final int WEATHER_STORMY = 4;
    public static final int WEATHER_SNOWY = 5;
    public static final int WEATHER_FOGGY = 6;

    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}
