package com.example.hikenativeapp.data.weather;

import java.io.Serializable;
import com.example.hikenativeapp.util.Constants;

public class WeatherInfo implements Serializable {

    public static final int WEATHER_SUNNY = Constants.WEATHER_SUNNY;
    public static final int WEATHER_PARTLY_CLOUDY = Constants.WEATHER_PARTLY_CLOUDY;
    public static final int WEATHER_CLOUDY = Constants.WEATHER_CLOUDY;
    public static final int WEATHER_RAINY = Constants.WEATHER_RAINY;
    public static final int WEATHER_STORMY = Constants.WEATHER_STORMY;
    public static final int WEATHER_SNOWY = Constants.WEATHER_SNOWY;
    public static final int WEATHER_FOGGY = Constants.WEATHER_FOGGY;

    private double temperature; // in Celsius
    private int weatherCondition;
    private String date; // ISO format: YYYY-MM-DD

    public WeatherInfo(double temperature, int weatherCondition, String date) {
        this.temperature = temperature;
        this.weatherCondition = weatherCondition;
        this.date = date;
    }

    // Getters and Setters
    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(int weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Convert weather conditions to string descriptions
     */
    public String getWeatherConditionString() {
        switch (weatherCondition) {
            case WEATHER_SUNNY:
                return "Sunny";
            case WEATHER_PARTLY_CLOUDY:
                return "Partly Cloudy";
            case WEATHER_CLOUDY:
                return "Cloudy";
            case WEATHER_RAINY:
                return "Rainy";
            case WEATHER_STORMY:
                return "Stormy";
            case WEATHER_SNOWY:
                return "Snowy";
            case WEATHER_FOGGY:
                return "Foggy";
            default:
                return "Unknown";
        }
    }

    /**
     * Determine weather conditions based on data from API
     */
    public static int determineWeatherCondition(double cloudCover, double precipitation) {
        if (precipitation > 5.0) {
            return WEATHER_STORMY;
        } else if (precipitation > 0.5) {
            return WEATHER_RAINY;
        } else if (cloudCover > 80) {
            return WEATHER_CLOUDY;
        } else if (cloudCover > 30) {
            return WEATHER_PARTLY_CLOUDY;
        } else {
            return WEATHER_SUNNY;
        }
    }
}
