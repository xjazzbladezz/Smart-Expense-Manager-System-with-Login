package com.smartexpense.db;

import com.smartexpense.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

public class UserDAO {

    public static String hash(String username, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("smartspend::" + username.toLowerCase() + "::" + password)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns the user when credentials are valid, otherwise null. */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT id, username, full_name FROM users WHERE username = ? AND password_hash = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(username, password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt(1), rs.getString(2), rs.getString(3));
                }
            }
        }
        return null;
    }

    /** Returns the new user, or null when the username is already taken. */
    public User register(String username, String password, String fullName) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash(username, password));
            ps.setString(3, fullName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return new User(rs.getInt(1), username, fullName);
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            return null;
        }
        return null;
    }

    public void ensureDefaultAdmin() throws SQLException {
        String sql = "INSERT IGNORE INTO users (username, password_hash, full_name) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, hash("admin", "admin123"));
            ps.setString(3, "Surya Admin");
            ps.executeUpdate();
        }
    }
}
