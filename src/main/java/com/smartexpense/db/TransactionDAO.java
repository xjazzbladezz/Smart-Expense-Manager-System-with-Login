package com.smartexpense.db;

import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private static final String INSERT =
            "INSERT INTO transactions (user_id, txn_date, type, category, amount, description, payment_mode) "
                    + "VALUES (?,?,?,?,?,?,?)";

    private static void bind(PreparedStatement ps, Transaction t) throws SQLException {
        ps.setInt(1, t.getUserId());
        ps.setDate(2, java.sql.Date.valueOf(t.getDate()));
        ps.setString(3, t.getType().name());
        ps.setString(4, t.getCategory());
        ps.setBigDecimal(5, BigDecimal.valueOf(t.getAmount()).setScale(2, RoundingMode.HALF_UP));
        ps.setString(6, t.getDescription());
        ps.setString(7, t.getPaymentMode());
    }

    public int insert(Transaction t) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, t);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /** Fast bulk insert in a single transaction (used by Excel import and demo data). */
    public void insertBatch(List<Transaction> list) throws SQLException {
        if (list.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(INSERT)) {
                for (Transaction t : list) {
                    bind(ps, t);
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void update(Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET txn_date=?, type=?, category=?, amount=?, description=?, "
                + "payment_mode=? WHERE id=? AND user_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(t.getDate()));
            ps.setString(2, t.getType().name());
            ps.setString(3, t.getCategory());
            ps.setBigDecimal(4, BigDecimal.valueOf(t.getAmount()).setScale(2, RoundingMode.HALF_UP));
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getPaymentMode());
            ps.setInt(7, t.getId());
            ps.setInt(8, t.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id=? AND user_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void deleteAll(int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE user_id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public List<Transaction> findAll(int userId) throws SQLException {
        String sql = "SELECT id, user_id, txn_date, type, category, amount, description, payment_mode "
                + "FROM transactions WHERE user_id=? ORDER BY txn_date DESC, id DESC";
        List<Transaction> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Transaction(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getDate("txn_date").toLocalDate(),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getString("category"),
                            rs.getBigDecimal("amount").doubleValue(),
                            rs.getString("description"),
                            rs.getString("payment_mode")));
                }
            }
        }
        return out;
    }
}
