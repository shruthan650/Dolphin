package com.dolphin.repository.d1;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

/** Conversions between Java values and the strings/numbers stored in D1. */
public final class D1Values {

    /** Fixed-width UTC timestamps, so ORDER BY on the text column sorts chronologically. */
    private static final DateTimeFormatter INSTANT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(ZoneOffset.UTC);

    private D1Values() {
    }

    /** Every parameter goes over the wire as a string or null. */
    public static String toParam(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (value instanceof Instant instant) {
            return INSTANT_FORMAT.format(instant);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return value.toString();
    }

    static String string(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }

    static long number(Map<String, Object> row, String column) {
        Object value = row.get(column);
        if (value instanceof Number n) {
            return n.longValue();
        }
        return value == null ? 0 : Long.parseLong(value.toString());
    }

    static boolean bool(Map<String, Object> row, String column) {
        return number(row, column) != 0;
    }

    static Instant instant(Map<String, Object> row, String column) {
        String value = string(row, column);
        return value == null ? null : Instant.parse(value);
    }

    static LocalDate date(Map<String, Object> row, String column) {
        String value = string(row, column);
        return value == null ? null : LocalDate.parse(value);
    }

    static <E extends Enum<E>> E enumValue(Map<String, Object> row, String column, Class<E> type) {
        String value = string(row, column);
        return value == null ? null : Enum.valueOf(type, value);
    }

    /** "?, ?, ?" for an IN (...) clause. */
    static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }
}
