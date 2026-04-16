package lendwise.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class AccountStore {
    public static final String ROLE_LENDER = "LENDER";
    public static final String ROLE_BORROWER = "BORROWER";

    public static final class AccountProfile {
        private final String email;
        private final String displayName;
        private final String role;

        public AccountProfile(String email, String displayName, String role) {
            this.email = email == null ? "" : email.trim();
            this.displayName = displayName == null ? "" : displayName.trim();
            this.role = normalizeRole(role);
        }

        public String getEmail() {
            return email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getRole() {
            return role;
        }
    }

    private final Path accountsPath;
    private static final String PASSWORD_SUFFIX = ".password";
    private static final String NAME_SUFFIX = ".name";
    private static final String ROLE_SUFFIX = ".role";

    public AccountStore() {
        this(Paths.get("data", "accounts.properties"));
    }

    public AccountStore(Path accountsPath) {
        this.accountsPath = accountsPath;
        try {
            DatabaseManager.initializeSchema();
            migratePropertiesToDatabaseIfNeeded();
        } catch (SQLException ignored) {
        }
    }

    public boolean accountExists(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return false;
        }

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT 1 FROM accounts WHERE email = ?"
             )) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ignored) {
        }

        return loadProperties().containsKey(normalized + PASSWORD_SUFFIX);
    }

    public boolean canCreateBorrowerAccount(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return false;
        }

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT 1
                 FROM borrowers
                 WHERE LOWER(TRIM(gmail)) = LOWER(TRIM(?))
                   AND (
                        linked_account_email IS NULL
                        OR TRIM(linked_account_email) = ''
                        OR LOWER(TRIM(linked_account_email)) = LOWER(TRIM(?))
                   )
                 LIMIT 1
                 """
             )) {
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ignored) {
        }

        return false;
    }

    public boolean ensureBorrowerAccountLink(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return false;
        }

        try (Connection connection = DatabaseManager.openConnection()) {
            try (PreparedStatement existingLink = connection.prepareStatement(
                """
                SELECT id
                FROM borrowers
                WHERE LOWER(TRIM(linked_account_email)) = LOWER(TRIM(?))
                LIMIT 1
                """
            )) {
                existingLink.setString(1, normalized);
                try (ResultSet resultSet = existingLink.executeQuery()) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }

            try (PreparedStatement update = connection.prepareStatement(
                """
                UPDATE borrowers
                SET linked_account_email = ?
                WHERE id = (
                    SELECT id
                    FROM borrowers
                    WHERE LOWER(TRIM(gmail)) = LOWER(TRIM(?))
                      AND (linked_account_email IS NULL OR TRIM(linked_account_email) = '')
                    ORDER BY owner_email IS NULL OR TRIM(owner_email) = '', owner_email, id
                    LIMIT 1
                )
                """
            )) {
                update.setString(1, normalized);
                update.setString(2, normalized);
                return update.executeUpdate() > 0;
            }
        } catch (SQLException ignored) {
        }

        return false;
    }

    public boolean createAccount(String fullName, String email, String password, String role) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }
        String normalizedRole = normalizeRole(role);
        if (ROLE_BORROWER.equals(normalizedRole) && !canCreateBorrowerAccount(normalized)) {
            return false;
        }
        String securedPassword = PasswordSecurity.hashPassword(password.trim());

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO accounts(email, full_name, password, role) VALUES (?, ?, ?, ?)"
             )) {
            statement.setString(1, normalized);
            statement.setString(2, fullName == null ? "" : fullName.trim());
            statement.setString(3, securedPassword);
            statement.setString(4, normalizedRole);
            statement.executeUpdate();
            if (ROLE_BORROWER.equals(normalizedRole)) {
                ensureBorrowerAccountLink(normalized);
            }
            return true;
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        if (properties.containsKey(normalized + PASSWORD_SUFFIX)) {
            return false;
        }

        properties.setProperty(normalized + PASSWORD_SUFFIX, securedPassword);
        properties.setProperty(normalized + NAME_SUFFIX, fullName == null ? "" : fullName.trim());
        properties.setProperty(normalized + ROLE_SUFFIX, normalizedRole);
        saveProperties(properties);
        return true;
    }

    public AccountProfile authenticate(String email, String password, String role) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || password == null) {
            return null;
        }
        String normalizedRole = normalizeRole(role);

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT full_name, password, role FROM accounts WHERE email = ?"
             )) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String saved = resultSet.getString("password");
                    String savedRole = normalizeRole(resultSet.getString("role"));
                    if (PasswordSecurity.verifyPassword(password, saved) && savedRole.equals(normalizedRole)) {
                        upgradePasswordIfNeeded(normalized, saved, password);
                        String displayName = resultSet.getString("full_name");
                        displayName = displayName == null ? "" : displayName.trim();
                        return new AccountProfile(normalized, displayName.isEmpty() ? normalized : displayName, savedRole);
                    }
                    return null;
                }
            }
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        String saved = properties.getProperty(normalized + PASSWORD_SUFFIX);
        String savedRole = normalizeRole(properties.getProperty(normalized + ROLE_SUFFIX, ROLE_LENDER));
        if (PasswordSecurity.verifyPassword(password, saved) && savedRole.equals(normalizedRole)) {
            upgradePasswordIfNeeded(normalized, saved, password);
            String displayName = properties.getProperty(normalized + NAME_SUFFIX, "").trim();
            return new AccountProfile(normalized, displayName.isEmpty() ? normalized : displayName, savedRole);
        }
        return null;
    }

    public boolean updatePassword(String email, String newPassword) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        String securedPassword = PasswordSecurity.hashPassword(newPassword.trim());
        boolean updated = false;

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE accounts SET password = ? WHERE email = ?"
             )) {
            statement.setString(1, securedPassword);
            statement.setString(2, normalized);
            updated = statement.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        if (properties.containsKey(normalized + PASSWORD_SUFFIX)) {
            properties.setProperty(normalized + PASSWORD_SUFFIX, securedPassword);
            saveProperties(properties);
            updated = true;
        }

        return updated;
    }

    public String getDisplayName(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return "";
        }

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT full_name FROM accounts WHERE email = ?"
             )) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String fullName = resultSet.getString("full_name");
                    fullName = fullName == null ? "" : fullName.trim();
                    return fullName.isEmpty() ? normalized : fullName;
                }
            }
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        String fullName = properties.getProperty(normalized + NAME_SUFFIX, "").trim();
        return fullName.isEmpty() ? normalized : fullName;
    }

    private void migratePropertiesToDatabaseIfNeeded() {
        Properties properties = loadProperties();
        if (properties.isEmpty()) {
            return;
        }

        try (Connection connection = DatabaseManager.openConnection()) {
            for (String key : properties.stringPropertyNames()) {
                if (!key.endsWith(PASSWORD_SUFFIX)) {
                    continue;
                }

                String email = key.substring(0, key.length() - PASSWORD_SUFFIX.length());
                if (email.isEmpty()) {
                    continue;
                }

                try (PreparedStatement exists = connection.prepareStatement(
                    "SELECT 1 FROM accounts WHERE email = ?"
                )) {
                    exists.setString(1, email);
                    try (ResultSet resultSet = exists.executeQuery()) {
                        if (resultSet.next()) {
                            continue;
                        }
                    }
                }

                try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO accounts(email, full_name, password, role) VALUES (?, ?, ?, ?)"
                )) {
                    insert.setString(1, email);
                    insert.setString(2, properties.getProperty(email + NAME_SUFFIX, ""));
                    insert.setString(3, secureStoredPassword(properties.getProperty(key, "")));
                    insert.setString(4, normalizeRole(properties.getProperty(email + ROLE_SUFFIX, ROLE_LENDER)));
                    insert.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim();
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        return ROLE_BORROWER.equals(normalized) ? ROLE_BORROWER : ROLE_LENDER;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        if (!Files.exists(accountsPath)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(accountsPath)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void saveProperties(Properties properties) {
        try {
            Path parent = accountsPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(accountsPath)) {
                properties.store(output, "LendWise Accounts");
            }
        } catch (IOException ignored) {
        }
    }

    private void upgradePasswordIfNeeded(String email, String savedValue, String plainPassword) {
        if (PasswordSecurity.isHashed(savedValue) || plainPassword == null || plainPassword.isEmpty()) {
            return;
        }
        updatePassword(email, plainPassword);
    }

    private String secureStoredPassword(String storedPassword) {
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            return "";
        }
        return PasswordSecurity.isHashed(storedPassword)
            ? storedPassword.trim()
            : PasswordSecurity.hashPassword(storedPassword.trim());
    }
}
