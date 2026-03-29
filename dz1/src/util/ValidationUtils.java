package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtils {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]+$";

    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 20) {
            return false;
        }

        return normalized.matches(USERNAME_REGEX);
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalized = email.trim().toLowerCase();

        int atIndex = normalized.indexOf("@");
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return false;
        }

        String domain = normalized.substring(atIndex + 1);
        if (!domain.contains(".")) {
            return false;
        }

        return true;
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.isBlank()) {
            return false;
        }

        try {
            LocalDateTime.parse(date.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String normalizeString(String input, boolean toUpperCase) {
        if (input == null) {
            return "";
        }

        String normalized = input.trim().replaceAll("\\s+", " ");

        if (toUpperCase) {
            return normalized.toUpperCase();
        } else {
            return normalized.toLowerCase();
        }
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    String.format("Поле '%s' не может быть пустым", fieldName)
            );
        }
    }
}
