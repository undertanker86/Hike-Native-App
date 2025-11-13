package com.example.hikenativeapp.data.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StormGlassResponse {

    @SerializedName("hours")
    private List<HourlyForecast> hours;

    public List<HourlyForecast> getHours() {
        return hours;
    }

    public static class HourlyForecast {
        @SerializedName("time")
        private String time;

        @SerializedName("airTemperature")
        private DataPoint airTemperature;

        @SerializedName("cloudCover")
        private DataPoint cloudCover;

        @SerializedName("precipitation")
        private DataPoint precipitation;

        // Getters
        public String getTime() {
            return time;
        }

        public DataPoint getAirTemperature() {
            return airTemperature;
        }

        public DataPoint getCloudCover() {
            return cloudCover;
        }

        public DataPoint getPrecipitation() {
            return precipitation;
        }

        // Helper methods to get data from a specific source or average
        public double getTemperature() {
            if (airTemperature == null) {
                return 0.0;
            }
            // Try to get from preferred source or use the first available
            if (airTemperature.noaa != null) {
                return airTemperature.noaa;
            } else if (airTemperature.sg != null) {
                return airTemperature.sg;
            }
            return 0.0;
        }

        public double getCloudCoverPercentage() {
            if (cloudCover == null) {
                return 0.0;
            }
            if (cloudCover.noaa != null) {
                return cloudCover.noaa;
            } else if (cloudCover.sg != null) {
                return cloudCover.sg;
            }
            return 0.0;
        }

        public double getPrecipitationAmount() {
            if (precipitation == null) {
                return 0.0;
            }
            if (precipitation.noaa != null) {
                return precipitation.noaa;
            } else if (precipitation.sg != null) {
                return precipitation.sg;
            }
            return 0.0;
        }
    }

    public static class DataPoint {
        @SerializedName("noaa")
        private Double noaa;

        @SerializedName("sg")
        private Double sg;

        // Add other possible sources as needed
        @SerializedName("icon")
        private Double icon;

        @SerializedName("meteo")
        private Double meteo;

        // Getters
        public Double getNoaa() {
            return noaa;
        }

        public Double getSg() {
            return sg;
        }

        public Double getIcon() {
            return icon;
        }

        public Double getMeteo() {
            return meteo;
        }
    }
}
