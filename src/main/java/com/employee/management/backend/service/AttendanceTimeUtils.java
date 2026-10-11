package com.employee.management.backend.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Check-in/check-out times are stored as plain strings. The employee attendance page sends
 * 12-hour browser times ("09:15:30 AM"), while older rows and API callers use 24-hour
 * ("09:15:30" / "09:15") or ISO ("09:15:30.123") - this reads all of them.
 */
public final class AttendanceTimeUtils {

    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            twelveHour("hh:mm:ss a"),
            twelveHour("h:mm:ss a"),
            twelveHour("hh:mm a"),
            twelveHour("h:mm a"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("H:mm"));

    private AttendanceTimeUtils() {
    }

    private static DateTimeFormatter twelveHour(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
    }

    /** Parsed time of day, or null when the value is blank or in an unknown format. */
    public static LocalTime parseTime(String value) {
        if (value == null) return null;
        // Browsers put a narrow/no-break space before AM/PM in toLocaleTimeString.
        String normalized = value.replace(' ', ' ').replace(' ', ' ').trim();
        if (normalized.isEmpty()) return null;
        for (DateTimeFormatter format : TIME_FORMATS) {
            try {
                return LocalTime.parse(normalized, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    /** Check-in moment for an attendance row (its yyyy-MM-dd date + check-in time), or null. */
    public static LocalDateTime checkInDateTime(String date, String checkInTime) {
        LocalTime time = parseTime(checkInTime);
        if (date == null || time == null) return null;
        try {
            return LocalDateTime.of(LocalDate.parse(date.trim()), time);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** Hours between two moments, rounded to 2 decimals. */
    public static double hoursBetween(LocalDateTime from, LocalDateTime to) {
        long minutes = Duration.between(from, to).toMinutes();
        return Math.round((minutes / 60.0) * 100.0) / 100.0;
    }
}
