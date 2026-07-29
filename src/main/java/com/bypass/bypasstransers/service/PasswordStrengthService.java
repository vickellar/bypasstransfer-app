package com.bypass.bypasstransers.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordStrengthService {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]");
    private static final Pattern COMMON_PATTERNS = Pattern.compile(
            "(?i)(password|123456|qwerty|admin|letmein|welcome|monkey|dragon|master|sunshine|princess|abc123)"
    );

    public PasswordValidationResult validate(String password) {
        PasswordValidationResult result = new PasswordValidationResult();

        if (password == null || password.isEmpty()) {
            result.addError("Password is required");
            return result;
        }

        // Length check
        if (password.length() < MIN_LENGTH) {
            result.addError("Password must be at least " + MIN_LENGTH + " characters long");
        }

        // Uppercase check
        if (!UPPERCASE.matcher(password).find()) {
            result.addError("Password must contain at least one uppercase letter (A-Z)");
        }

        // Lowercase check
        if (!LOWERCASE.matcher(password).find()) {
            result.addError("Password must contain at least one lowercase letter (a-z)");
        }

        // Digit check
        if (!DIGIT.matcher(password).find()) {
            result.addError("Password must contain at least one number (0-9)");
        }

        // Special character check
        if (!SPECIAL_CHAR.matcher(password).find()) {
            result.addError("Password must contain at least one special character (!@#$%^&*)");
        }

        // Common patterns check
        if (COMMON_PATTERNS.matcher(password).find()) {
            result.addError("Password contains commonly used patterns - please choose a stronger password");
        }

        // Check for sequential characters
        if (hasSequentialCharacters(password)) {
            result.addError("Password contains sequential characters - please vary your password");
        }

        // Check for repeated characters
        if (hasRepeatedCharacters(password)) {
            result.addError("Password has too many repeated characters - please vary your password");
        }

        result.setStrength(calculateStrength(result));
        return result;
    }

    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRepeatedCharacters(String password) {
        int maxRepeat = 0;
        int currentRepeat = 1;

        for (int i = 0; i < password.length() - 1; i++) {
            if (password.charAt(i) == password.charAt(i + 1)) {
                currentRepeat++;
                maxRepeat = Math.max(maxRepeat, currentRepeat);
            } else {
                currentRepeat = 1;
            }
        }

        return maxRepeat > 3; // More than 3 repeated chars is weak
    }

    private String calculateStrength(PasswordValidationResult result) {
        if (!result.isValid()) {
            return "WEAK";
        }

        int score = 0;
        String password = result.getTestedPassword();

        if (password.length() >= 16) score += 2;
        else if (password.length() >= 12) score += 1;

        if (UPPERCASE.matcher(password).find()) score += 1;
        if (LOWERCASE.matcher(password).find()) score += 1;
        if (DIGIT.matcher(password).find()) score += 1;
        if (SPECIAL_CHAR.matcher(password).find()) score += 2;

        if (score >= 6) return "STRONG";
        else if (score >= 4) return "MEDIUM";
        else return "WEAK";
    }

    public static class PasswordValidationResult {
        private List<String> errors = new ArrayList<>();
        private String strength = "WEAK";
        private String testedPassword;

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getStrength() {
            return strength;
        }

        public void setStrength(String strength) {
            this.strength = strength;
        }

        public String getTestedPassword() {
            return testedPassword;
        }

        public void setTestedPassword(String testedPassword) {
            this.testedPassword = testedPassword;
        }

        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
    }
}