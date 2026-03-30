package lendwise.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DB_PATH = DATA_DIR.resolve("lendwise.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH.toString().replace('\\', '/');

    private DatabaseManager() {
    }

    public static Connection openConnection() throws SQLException {
        ensureDriverLoaded();
        ensureDataDirectory();
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initializeSchema() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS borrowers (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    gmail TEXT,
                    address TEXT
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id TEXT PRIMARY KEY,
                    borrower_id TEXT NOT NULL,
                    principal_amount REAL NOT NULL,
                    interest_rate_annual REAL NOT NULL,
                    original_term_months INTEGER NOT NULL,
                    term_months INTEGER NOT NULL,
                    start_date TEXT,
                    collector_name TEXT,
                    status TEXT,
                    FOREIGN KEY (borrower_id) REFERENCES borrowers(id) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id TEXT PRIMARY KEY,
                    loan_id TEXT NOT NULL,
                    amount REAL NOT NULL,
                    payment_date TEXT,
                    method TEXT,
                    FOREIGN KEY (loan_id) REFERENCES borrowers(id) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    email TEXT PRIMARY KEY,
                    full_name TEXT,
                    password TEXT NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS reminder_log (
                    borrower_id TEXT NOT NULL,
                    loan_id TEXT NOT NULL,
                    reminder_type TEXT NOT NULL,
                    sent_at TEXT NOT NULL,
                    PRIMARY KEY (borrower_id, loan_id, reminder_type),
                    FOREIGN KEY (borrower_id) REFERENCES borrowers(id) ON DELETE CASCADE,
                    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
                )
                """);
        }
    }

    public static boolean isReady() {
        try {
            initializeSchema();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQLite JDBC driver not found.", ex);
        }
    }

    private static void ensureDataDirectory() throws SQLException {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException ex) {
            throw new SQLException("Unable to create data directory.", ex);
        }
    }
}
