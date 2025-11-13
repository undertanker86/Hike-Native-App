package com.example.hikenativeapp.val;

/**
 * Utility class for validating observation data
 */
public class ObservationValidation {

    /**
     * Validate observation text
     * @param observationText The observation text to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateObservationText(String observationText) {
        if (observationText == null || observationText.trim().isEmpty()) {
            return "Observation text is required";
        }
        return null;
    }

    /**
     * Validate observation time
     * @param observationTime The observation time to validate
     * @return Error message if invalid, null if valid
     */
    public static String validateObservationTime(String observationTime) {
        if (observationTime == null || observationTime.trim().isEmpty()) {
            return "Observation time is required";
        }
        return null;
    }

    /**
     * Validate complete observation
     * @param observationText The observation text
     * @param observationTime The observation time
     * @return Error message if invalid, null if valid
     */
    public static String validateObservation(String observationText, String observationTime) {
        String textError = validateObservationText(observationText);
        if (textError != null) {
            return textError;
        }

        String timeError = validateObservationTime(observationTime);
        if (timeError != null) {
            return timeError;
        }

        return null;
    }
}

