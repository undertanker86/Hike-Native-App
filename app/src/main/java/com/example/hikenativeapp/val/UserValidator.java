package com.example.hikenativeapp.val;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Validator for User-related data
 */
public class UserValidator {

    // Minimum and maximum length for display name
    private static final int MIN_DISPLAY_NAME_LENGTH = 2;
    private static final int MAX_DISPLAY_NAME_LENGTH = 50;

    /**
     * Validates display name
     * @param displayName The display name to validate
     * @return ValidationResult containing validity status and error message
     */
    public static ValidationResult validateDisplayName(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            return new ValidationResult(false, "Display name cannot be empty");
        }

        String trimmed = displayName.trim();

        if (trimmed.isEmpty()) {
            return new ValidationResult(false, "Display name cannot be empty or contain only spaces");
        }

        if (trimmed.length() < MIN_DISPLAY_NAME_LENGTH) {
            return new ValidationResult(false, "Display name must be at least " + MIN_DISPLAY_NAME_LENGTH + " characters");
        }

        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            return new ValidationResult(false, "Display name cannot exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }

        // Check for invalid characters (optional - you can customize this)
        if (!trimmed.matches("^[a-zA-Z0-9\\s._-]+$")) {
            return new ValidationResult(false, "Display name can only contain letters, numbers, spaces, and ._- characters");
        }

        return new ValidationResult(true, "Valid display name");
    }

    /**
     * Validates email address
     * @param email The email to validate
     * @return ValidationResult containing validity status and error message
     */
    public static ValidationResult validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return new ValidationResult(false, "Email cannot be empty");
        }

        String trimmed = email.trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return new ValidationResult(false, "Invalid email format");
        }

        return new ValidationResult(true, "Valid email");
    }

    /**
     * Validates Google ID
     * @param googleId The Google ID to validate
     * @return ValidationResult containing validity status and error message
     */
    public static ValidationResult validateGoogleId(String googleId) {
        if (TextUtils.isEmpty(googleId)) {
            return new ValidationResult(false, "Google ID cannot be empty");
        }

        String trimmed = googleId.trim();

        if (trimmed.isEmpty()) {
            return new ValidationResult(false, "Google ID cannot be empty");
        }

        // Google IDs are typically alphanumeric
        if (!trimmed.matches("^[a-zA-Z0-9]+$")) {
            return new ValidationResult(false, "Invalid Google ID format");
        }

        return new ValidationResult(true, "Valid Google ID");
    }

    /**
     * Validates photo URL
     * @param photoUrl The photo URL to validate
     * @return ValidationResult containing validity status and error message
     */
    public static ValidationResult validatePhotoUrl(String photoUrl) {
        // Photo URL is optional, so null or empty is valid
        if (TextUtils.isEmpty(photoUrl)) {
            return new ValidationResult(true, "Photo URL is optional");
        }

        String trimmed = photoUrl.trim();

        if (!Patterns.WEB_URL.matcher(trimmed).matches()) {
            return new ValidationResult(false, "Invalid photo URL format");
        }

        return new ValidationResult(true, "Valid photo URL");
    }

    /**
     * Validates complete user data
     * @param googleId Google ID
     * @param email Email address
     * @param displayName Display name
     * @param photoUrl Photo URL (optional)
     * @return ValidationResult containing validity status and error message
     */
    public static ValidationResult validateUserData(String googleId, String email,
                                                    String displayName, String photoUrl) {
        // Validate Google ID
        ValidationResult googleIdResult = validateGoogleId(googleId);
        if (!googleIdResult.isValid()) {
            return googleIdResult;
        }

        // Validate Email
        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.isValid()) {
            return emailResult;
        }

        // Validate Display Name
        ValidationResult displayNameResult = validateDisplayName(displayName);
        if (!displayNameResult.isValid()) {
            return displayNameResult;
        }

        // Validate Photo URL (optional)
        ValidationResult photoUrlResult = validatePhotoUrl(photoUrl);
        if (!photoUrlResult.isValid()) {
            return photoUrlResult;
        }

        return new ValidationResult(true, "All user data is valid");
    }

    /**
     * Result class for validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}

