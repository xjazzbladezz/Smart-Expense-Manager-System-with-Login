package com.smartexpense.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates the database, tables and the default admin account on first run. */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void init() throws SQLException {
        try (Connection c = DBConnection.getServerConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DBConnection.dbName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            INT AUTO_INCREMENT PRIMARY KEY,
                        username      VARCHAR(50)  NOT NULL UNIQUE,
                        password_hash CHAR(64)     NOT NULL,
                        full_name     VARCHAR(100) NOT NULL,
                        created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB
                    """);

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id           INT AUTO_INCREMENT PRIMARY KEY,
                        user_id      INT           NOT NULL,
                        txn_date     DATE          NOT NULL,
                        type         ENUM('INCOME','EXPENSE') NOT NULL,
                        category     VARCHAR(60)   NOT NULL,
                        amount       DECIMAL(12,2) NOT NULL,
                        description  VARCHAR(255),
                        payment_mode VARCHAR(30)   NOT NULL DEFAULT 'UPI',
                        created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_user_date (user_id, txn_date),
                        INDEX idx_user_cat  (user_id, category),
                        CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id            INT AUTO_INCREMENT PRIMARY KEY,
                        user_id       INT           NOT NULL,
                        category      VARCHAR(60)   NOT NULL,
                        monthly_limit DECIMAL(12,2) NOT NULL,
                        UNIQUE KEY uq_user_cat (user_id, category),
                        CONSTRAINT fk_bud_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);
        }

        new UserDAO().ensureDefaultAdmin();
    }
}
