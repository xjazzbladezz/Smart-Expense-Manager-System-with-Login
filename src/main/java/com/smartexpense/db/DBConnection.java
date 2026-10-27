package com.smartexpense.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection factory. Reads db.properties (a file next to the jar wins over
 * the one bundled inside the jar).
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    private static final String PARAMS =
            "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
                    + "&rewriteBatchedStatements=true&characterEncoding=UTF-8";

    static {
        boolean loaded = false;
        File external = new File("db.properties");
        if (external.exists()) {
            try (InputStream in = new FileInputStream(external)) {
                PROPS.load(in);
                loaded = true;
            } catch (IOException ignored) {
                // fall back to bundled copy
            }
        }
        if (!loaded) {
            try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
                if (in != null) PROPS.load(in);
            } catch (IOException ignored) {
                // defaults below
            }
        }
    }

    private DBConnection() {
    }

    private static String p(String key, String def) {
        return PROPS.getProperty(key, def).trim();
    }

    public static String host() { return p("db.host", "localhost"); }
    public static String port() { return p("db.port", "3306"); }
    public static String dbName() { return p("db.name", "smart_expense_db"); }
    public static String user() { return p("db.user", "root"); }
    private static String password() { return p("db.password", ""); }

    public static String url() {
        return "jdbc:mysql://" + host() + ":" + port() + "/" + dbName() + "?" + PARAMS;
    }

    private static String serverUrl() {
        return "jdbc:mysql://" + host() + ":" + port() + "/?" + PARAMS;
    }

    /** Connection to the application database. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url(), user(), password());
    }

    /** Connection to the MySQL server itself (used to CREATE DATABASE). */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(serverUrl(), user(), password());
    }

    public static String describe() {
        return host() + ":" + port() + "/" + dbName();
    }
}
