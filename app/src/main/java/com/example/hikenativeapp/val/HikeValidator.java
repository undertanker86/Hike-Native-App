package com.example.hikenativeapp.val;

import android.text.TextUtils;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Validator class for Hike related validations
 */
public class HikeValidator {

    /**
     * Validate required text field
     * @param inputLayout The TextInputLayout to validate
     * @param value The value to check
     * @param fieldName The name of the field for error message
     * @return true if valid, false otherwise
     */
    public static boolean validateRequiredField(TextInputLayout inputLayout, String value, String fieldName) {
        if (TextUtils.isEmpty(value)) {
            inputLayout.setError(fieldName + " is required");
            return false;
        }
        return true;
    }

    /**
     * Validate numeric field
     * @param inputLayout The TextInputLayout to validate
     * @param value The value to check
     * @param fieldName The name of the field for error message
     * @param minValue Minimum allowed value
     * @return true if valid, false otherwise
     */
    public static boolean validateNumericField(TextInputLayout inputLayout, String value, String fieldName, double minValue) {
        if (TextUtils.isEmpty(value)) {
            inputLayout.setError(fieldName + " is required");
            return false;
        }

        try {
            double numericValue = Double.parseDouble(value);
            if (numericValue <= minValue) {
                inputLayout.setError(fieldName + " must be greater than " + minValue);
                return false;
            }
        } catch (NumberFormatException e) {
            inputLayout.setError("Please enter a valid number");
            return false;
        }

        return true;
    }

    /**
     * Validate date field
     * @param date The date string to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateDate(String date) {
        return !TextUtils.isEmpty(date);
    }

    /**
     * Clear all errors from input layouts
     * @param inputLayouts TextInputLayouts to clear errors from
     */
    public static void clearErrors(TextInputLayout... inputLayouts) {
        for (TextInputLayout inputLayout : inputLayouts) {
            inputLayout.setError(null);
        }
    }
}
