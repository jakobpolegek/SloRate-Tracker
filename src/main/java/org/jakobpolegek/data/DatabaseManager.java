package org.jakobpolegek.data;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./rates_db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS exchange_rates (" +
                    "rate_date DATE, " +
                    "currency VARCHAR(3), " +
                    "rate DECIMAL(20, 10), " +
                    "PRIMARY KEY (rate_date, currency))";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Optional<Double> getMostRecentRate(LocalDate date, String currency) {
        String sql = "SELECT rate FROM exchange_rates WHERE currency = ? AND rate_date <= ? " +
                "ORDER BY rate_date DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, currency);
            pstmt.setDate(2, java.sql.Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getDouble("rate"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static List<Map<String, Object>> getRatesForPeriod(LocalDate startDate, LocalDate endDate, List<String> currencies) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT rate_date, currency, rate FROM exchange_rates " +
                "WHERE rate_date BETWEEN ? AND ? AND currency IN (%s) " +
                "ORDER BY rate_date, currency";

        String inClause = String.join(",", Collections.nCopies(currencies.size(), "?"));
        sql = String.format(sql, inClause);

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "")) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));
            for (int i = 0; i < currencies.size(); i++) {
                pstmt.setString(3 + i, currencies.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("date", rs.getDate("rate_date").toLocalDate());
                row.put("currency", rs.getString("currency"));
                row.put("rate", rs.getBigDecimal("rate"));
                results.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}