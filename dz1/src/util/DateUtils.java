package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter SIMPLE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        if (date == null || date.isEmpty()) return null;

        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate newDate = localDate.plusDays(days);
            return newDate.format(DATE_FORMATTER);
        } catch (Exception e) {
            // В случае ошибки возвращаем исходную дату
            return date;
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null || date.isEmpty()) return "";

        try {
            LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            long daysDiff = ChronoUnit.DAYS.between(today, targetDate);

            if (daysDiff == 0) {
                return "today";
            } else if (daysDiff < 0) {
                long absDays = Math.abs(daysDiff);
                if (absDays == 1) return "yesterday";
                if (absDays < 7) return absDays + " days ago";
                if (absDays < 30) return (absDays / 7) + " weeks ago";
                if (absDays < 365) return (absDays / 30) + " months ago";
                return (absDays / 365) + " years ago";
            } else {
                if (daysDiff == 1) return "tomorrow";
                if (daysDiff < 7) return "in " + daysDiff + " days";
                if (daysDiff < 30) return "in " + (daysDiff / 7) + " weeks";
                if (daysDiff < 365) return "in " + (daysDiff / 30) + " months";
                return "in " + (daysDiff / 365) + " years";
            }
        } catch (Exception e) {
            return "invalid date";
        }
    }

    public static boolean isPast(String date) {
        if (date == null) return false;
        return isBefore(date, getCurrentDate());
    }

    public static boolean isFuture(String date) {
        if (date == null) return false;
        return isAfter(date, getCurrentDate());
    }

    public static boolean isToday(String date) {
        if (date == null) return false;
        return date.equals(getCurrentDate());
    }

    public static long daysBetween(String date1, String date2) {
        try {
            LocalDate d1 = LocalDate.parse(date1, DATE_FORMATTER);
            LocalDate d2 = LocalDate.parse(date2, DATE_FORMATTER);
            return ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatDate(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            return localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (Exception e) {
            return date;
        }
    }
}
