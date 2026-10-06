package com.dolphin.repository.d1;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Minimal SQL access to a Cloudflare D1 (SQLite) database. Parameters are bound positionally to {@code ?}
 * placeholders and sent as strings (or null); SQLite column affinity converts them to the column type.
 */
public interface D1Client {

    /** Runs one statement and returns its rows as column-name → value maps. */
    List<Map<String, Object>> query(String sql, List<?> params);

    /** Runs several statements atomically: either all of them apply or none do. */
    void batch(List<D1Statement> statements);

    default List<Map<String, Object>> query(String sql, Object... params) {
        return query(sql, Arrays.asList(params));
    }

    default void execute(String sql, Object... params) {
        query(sql, Arrays.asList(params));
    }

    record D1Statement(String sql, List<?> params) {
        public static D1Statement of(String sql, Object... params) {
            return new D1Statement(sql, Arrays.asList(params));
        }
    }
}
