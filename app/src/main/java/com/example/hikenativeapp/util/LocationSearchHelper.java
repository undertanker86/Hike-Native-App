package com.example.hikenativeapp.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class to manage location search and autocomplete using Google Places SDK
 */
public class LocationSearchHelper {
    private static final String TAG = "LocationSearchHelper";

    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private Context context;

    // Interface callbacks
    public interface AutocompleteCallback {
        void onSuccess(List<AutocompletePrediction> predictions);
        void onError(String error);
    }

    public interface PlaceDetailsCallback {
        void onSuccess(Place place);
        void onError(String error);
    }

    public LocationSearchHelper(Context context) {
        this.context = context;
        initializePlaces();
    }

    /**
     * Initialize Places SDK
     */
    private void initializePlaces() {
        try {
            String apiKey = getApiKeyFromMetadata();

            if (apiKey == null || apiKey.trim().isEmpty()) {
                Log.e(TAG, "API Key is empty or null. Please check your AndroidManifest.xml and local.properties");
                throw new IllegalStateException("Google Maps API Key not found. Please add MAPS_API_KEY to local.properties");
            }

            Log.d(TAG, "Initializing Places SDK with API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

            if (!Places.isInitialized()) {
                // Initialize Places SDK with API key from metadata
                Places.initialize(context, apiKey);
                Log.d(TAG, "Places SDK initialized successfully");
            } else {
                Log.d(TAG, "Places SDK already initialized");
            }

            placesClient = Places.createClient(context);
            sessionToken = AutocompleteSessionToken.newInstance();
            Log.d(TAG, "PlacesClient created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Places SDK: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Places SDK: " + e.getMessage(), e);
        }
    }

    /**
     * Get API key from AndroidManifest metadata
     */
    private String getApiKeyFromMetadata() {
        try {
            android.content.pm.ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), android.content.pm.PackageManager.GET_META_DATA);

            if (ai.metaData == null) {
                Log.e(TAG, "No metadata found in AndroidManifest.xml");
                return "";
            }

            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                Log.e(TAG, "API Key metadata exists but value is empty");
                return "";
            }

            Log.d(TAG, "API Key retrieved successfully from metadata");
            return apiKey;
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key from metadata: " + e.getMessage(), e);
            return "";
        }
    }

    /**
     * Find places with autocomplete
     * @param query Search query
     * @param callback Callback to handle results
     */
    public void searchPlaces(String query, AutocompleteCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Search query is empty");
            return;
        }

        // Create request
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .setTypesFilter(Arrays.asList(PlaceTypes.ESTABLISHMENT, PlaceTypes.GEOCODE))
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    Log.d(TAG, "Autocomplete predictions received: " + response.getAutocompletePredictions().size());
                    callback.onSuccess(response.getAutocompletePredictions());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error getting autocomplete predictions: " + exception.getMessage());
                    callback.onError("Failed to get place suggestions: " + exception.getMessage());
                });
    }

    /**
     * Search for places within specific bounds
     * @param query Search query
     * @param bounds Search bounds
     * @param callback Callback to handle results
     */
    public void searchPlacesInBounds(String query, RectangularBounds bounds, AutocompleteCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Search query is empty");
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .setLocationBias(bounds)
                .setTypesFilter(Arrays.asList(PlaceTypes.ESTABLISHMENT, PlaceTypes.GEOCODE))
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    Log.d(TAG, "Bounded autocomplete predictions received: " + response.getAutocompletePredictions().size());
                    callback.onSuccess(response.getAutocompletePredictions());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error getting bounded autocomplete predictions: " + exception.getMessage());
                    callback.onError("Failed to get place suggestions: " + exception.getMessage());
                });
    }

    /**
     * Get place details from place ID
     * @param placeId Place ID
     * @param callback Callback to handle results
     */
    public void getPlaceDetails(String placeId, PlaceDetailsCallback callback) {
        if (placeId == null || placeId.trim().isEmpty()) {
            callback.onError("Place ID is empty");
            return;
        }

        // Define fields to retrieve from Place
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI,
                Place.Field.RATING
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    Log.d(TAG, "Place details received: " + place.getName() + " at " + place.getLatLng());
                    callback.onSuccess(place);

                    // Create new session token for next search
                    sessionToken = AutocompleteSessionToken.newInstance();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error getting place details: " + exception.getMessage());
                    callback.onError("Failed to get place details: " + exception.getMessage());
                });
    }

    /**
     * Reverse geocoding: Convert coordinates to address
     * Using Geocoder instead of Places SDK as it's more efficient for this task
     */
    public void reverseGeocode(double latitude, double longitude, ReverseGeocodeCallback callback) {
        android.location.Geocoder geocoder = new android.location.Geocoder(context, java.util.Locale.getDefault());

        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();

                // Build readable address
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressString.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressString.append(", ");
                    }
                }

                callback.onSuccess(addressString.toString(), latitude, longitude);
                Log.d(TAG, "Reverse geocoding successful: " + addressString.toString());
            } else {
                callback.onError("No address found for the given coordinates");
            }
        } catch (Exception e) {
            Log.e(TAG, "Reverse geocoding failed: " + e.getMessage());
            callback.onError("Failed to get address: " + e.getMessage());
        }
    }

    /**
     * Callback interface for reverse geocoding
     */
    public interface ReverseGeocodeCallback {
        void onSuccess(String address, double latitude, double longitude);
        void onError(String error);
    }


}
