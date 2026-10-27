package com.smartexpense.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BudgetDAO {

    public Map<String, Double> findAll(int userId) throws SQLException {
        Map<String, Double> out = new LinkedHashMap<>();
        String sql = "SELECT category, monthly_limit FROM budgets WHERE user_id=? ORDER BY category";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getBigDecimal(2).doubleValue());
            }
        }
        return out;
    }

    public void upsert(int userId, String category, double limit) throws SQLException {
        String sql = "INSERT INTO budgets (user_id, category, monthly_limit) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setBigDecimal(3, BigDecimal.valueOf(limit).setScale(2, RoundingMode.HALF_UP));
            ps.executeUpdate();
        }
    }

    public void delete(int userId, String category) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM budgets WHERE user_id=? AND category=?")) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.executeUpdate();
        }
    }
}
