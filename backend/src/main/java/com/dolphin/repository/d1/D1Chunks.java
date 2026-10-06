package com.dolphin.repository.d1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Splits {@code IN (...)} queries, because D1 allows at most 100 bound parameters per statement. */
final class D1Chunks {

    private static final int MAX_PARAMS = 90;

    private D1Chunks() {
    }

    /**
     * @param sqlTemplate SQL with one {@code %s} where the IN-list placeholders go; it must take no other parameters
     * @return the rows of all chunks, in chunk order (callers re-sort if they need a global order)
     */
    static List<Map<String, Object>> query(D1Client d1, String sqlTemplate, Collection<String> values) {
        List<String> all = new ArrayList<>(values);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int start = 0; start < all.size(); start += MAX_PARAMS) {
            List<String> chunk = all.subList(start, Math.min(start + MAX_PARAMS, all.size()));
            rows.addAll(d1.query(sqlTemplate.formatted(D1Values.placeholders(chunk.size())), chunk));
        }
        return rows;
    }
}
