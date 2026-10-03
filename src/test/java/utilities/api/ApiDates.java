package utilities.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import utilities.FrameworkConfig;

/** The pinned local server runs in UTC; other deployments must configure their server timezone. */
public final class ApiDates {
    private ApiDates() { }

    public static LocalDate transactionDate(String value) {
        return transactionDate(value, ZoneId.of(FrameworkConfig.load().get("api.server.timezone")));
    }

    public static LocalDate transactionDate(String value, ZoneId zone) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing transaction date");
        }
        try {
            if (value.matches("-?[0-9]+")) {
                return Instant.ofEpochMilli(Long.parseLong(value)).atZone(zone).toLocalDate();
            }
            return OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate();
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    public static String day(String value) {
        return transactionDate(value).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
    }

    public static String month(String value) {
        return transactionDate(value).format(DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH));
    }
}
