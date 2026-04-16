package lendwise.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
                    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
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
            ensureColumnExists(connection, "borrowers", "owner_email", "TEXT");
            ensureColumnExists(connection, "borrowers", "linked_account_email", "TEXT");
            ensureColumnExists(connection, "borrowers", "government_id_photo_path", "TEXT");
            ensureColumnExists(connection, "loans", "owner_email", "TEXT");
            ensureColumnExists(connection, "payments", "owner_email", "TEXT");
            ensureColumnExists(connection, "accounts", "role", "TEXT");
            ensurePaymentsTableReferencesLoans(connection);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_borrowers_owner_email ON borrowers(owner_email)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_borrowers_linked_account_email ON borrowers(linked_account_email)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_loans_owner_email ON loans(owner_email)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_payments_owner_email ON payments(owner_email)");
            statement.execute("UPDATE accounts SET role = 'LENDER' WHERE role IS NULL OR TRIM(role) = ''");
        }
    }

    private static void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (tableHasColumn(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static boolean tableHasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void ensurePaymentsTableReferencesLoans(Connection connection) throws SQLException {
        boolean referencesLoans = false;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_list(payments)")) {
            while (resultSet.next()) {
                String fromColumn = resultSet.getString("from");
                String referencedTable = resultSet.getString("table");
                if ("loan_id".equalsIgnoreCase(fromColumn) && "loans".equalsIgnoreCase(referencedTable)) {
                    referencesLoans = true;
                    break;
                }
            }
        }

        if (referencesLoans) {
            return;
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("""
                CREATE TABLE payments_new (
                    id TEXT PRIMARY KEY,
                    loan_id TEXT NOT NULL,
                    amount REAL NOT NULL,
                    payment_date TEXT,
                    method TEXT,
                    owner_email TEXT,
                    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                INSERT INTO payments_new(id, loan_id, amount, payment_date, method, owner_email)
                SELECT
                    p.id,
                    COALESCE(
                        (SELECT l.id FROM loans l WHERE l.id = p.loan_id ORDER BY l.id LIMIT 1),
                        (SELECT l.id FROM loans l WHERE l.borrower_id = p.loan_id ORDER BY l.id LIMIT 1)
                    ) AS mapped_loan_id,
                    p.amount,
                    p.payment_date,
                    p.method,
                    p.owner_email
                FROM payments p
                WHERE COALESCE(
                    (SELECT l.id FROM loans l WHERE l.id = p.loan_id ORDER BY l.id LIMIT 1),
                    (SELECT l.id FROM loans l WHERE l.borrower_id = p.loan_id ORDER BY l.id LIMIT 1)
                ) IS NOT NULL
                """);
            statement.execute("DROP TABLE payments");
            statement.execute("ALTER TABLE payments_new RENAME TO payments");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_payments_owner_email ON payments(owner_email)");
            statement.execute("PRAGMA foreign_keys = ON");
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
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
