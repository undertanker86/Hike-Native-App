package com.example.hikenativeapp.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    public static final int REQUEST_LOCATION_PERMISSION = 1001;
    public static final int REQUEST_CHECK_SETTINGS = 1002;

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private LocationListener locationListener;

    public interface LocationListener {
        void onLocationReceived(String address, double latitude, double longitude);
        void onLocationFailed(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        createLocationRequest();
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5)
                .setMaxUpdateDelayMillis(10)
                .build();
    }

    public void setLocationListener(LocationListener listener) {
        this.locationListener = listener;
    }

    public boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION);
    }

    public void checkLocationSettings(Activity activity) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied, start location updates
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult()
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                        Log.e(TAG, "Error opening settings activity", sendEx);
                        if (locationListener != null) {
                            locationListener.onLocationFailed("Unable to open location settings");
                        }
                    }
                } else {
                    if (locationListener != null) {
                        locationListener.onLocationFailed("Location settings are not satisfied");
                    }
                }
            }
        });
    }

    public void getCurrentLocation() {
        if (!hasLocationPermissions()) {
            if (locationListener != null) {
                locationListener.onLocationFailed("Location permission is required");
            }
            return;
        }

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        getAddressFromLocation(location);
                    } else {
                        // Last location might be null, request new location updates
                        startLocationUpdates();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (locationListener != null) {
                        locationListener.onLocationFailed("Failed to get location: " + e.getMessage());
                    }
                }
            });
        } catch (SecurityException e) {
            if (locationListener != null) {
                locationListener.onLocationFailed("Location permission is required");
            }
        }
    }

    private void startLocationUpdates() {
        if (!hasLocationPermissions()) {
            if (locationListener != null) {
                locationListener.onLocationFailed("Location permission is required");
            }
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location);
                        stopLocationUpdates();
                        return;
                    }
                }

                if (locationListener != null) {
                    locationListener.onLocationFailed("Could not get a valid location");
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            if (locationListener != null) {
                locationListener.onLocationFailed("Location permission is required");
            }
        }
    }

    private void getAddressFromLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();

                // Build a readable address from available address lines
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressString.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressString.append(", ");
                    }
                }

                if (locationListener != null) {
                    locationListener.onLocationReceived(addressString.toString(), latitude, longitude);
                }
            } else {
                if (locationListener != null) {
                    // If no address found, just return the coordinates
                    String coords = "Lat: " + latitude + ", Long: " + longitude;
                    locationListener.onLocationReceived(coords, latitude, longitude);
                }
            }
        } catch (IOException e) {
            if (locationListener != null) {
                locationListener.onLocationFailed("Could not get address: " + e.getMessage());
            }
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}

