package com.dolphin.d1;

import com.dolphin.repository.d1.D1Client;
import com.dolphin.repository.d1.D1Exception;
import com.dolphin.repository.d1.D1Values;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test stand-in for Cloudflare D1, backed by an in-memory SQLite database (D1 is SQLite). Parameters are bound
 * as strings exactly like {@code CloudflareD1Client} sends them, so type-affinity behaviour matches production.
 */
public class SqliteD1Client implements D1Client {

    private final Connection connection;

    public SqliteD1Client() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to open SQLite test database", ex);
        }
    }

    @Override
    public synchronized List<Map<String, Object>> query(String sql, List<?> params) {
        try {
            return run(sql, params);
        } catch (SQLException ex) {
            throw new D1Exception("SQLite error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public synchronized void batch(List<D1Statement> statements) {
        try {
            connection.setAutoCommit(false);
            try {
                for (D1Statement statement : statements) {
                    run(statement.sql(), statement.params());
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw new D1Exception("SQLite error: " + ex.getMessage(), ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new D1Exception("SQLite transaction error: " + ex.getMessage(), ex);
        }
    }

    private List<Map<String, Object>> run(String sql, List<?> params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, D1Values.toParam(params.get(i)));
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            if (!ps.execute()) {
                return rows;
            }
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= meta.getColumnCount(); c++) {
                        row.put(meta.getColumnLabel(c), rs.getObject(c));
                    }
                    rows.add(row);
                }
            }
            return rows;
        }
    }
}
