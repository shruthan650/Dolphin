package com.dolphin.mapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MapperUtils {

    private MapperUtils() {
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Trims, drops blanks and removes case-insensitive duplicates while keeping order. */
    static List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        return values.stream()
                .map(MapperUtils::trimToNull)
                .filter(v -> v != null && seen.add(v.toLowerCase()))
                .toList();
    }
}
