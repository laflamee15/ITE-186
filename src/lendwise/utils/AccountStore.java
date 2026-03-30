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
    private final Path accountsPath;
    private static final String PASSWORD_SUFFIX = ".password";
    private static final String NAME_SUFFIX = ".name";

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

    public boolean createAccount(String fullName, String email, String password) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO accounts(email, full_name, password) VALUES (?, ?, ?)"
             )) {
            statement.setString(1, normalized);
            statement.setString(2, fullName == null ? "" : fullName.trim());
            statement.setString(3, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        if (properties.containsKey(normalized + PASSWORD_SUFFIX)) {
            return false;
        }

        properties.setProperty(normalized + PASSWORD_SUFFIX, password);
        properties.setProperty(normalized + NAME_SUFFIX, fullName == null ? "" : fullName.trim());
        saveProperties(properties);
        return true;
    }

    public boolean authenticate(String email, String password) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || password == null) {
            return false;
        }

        try (Connection connection = DatabaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT password FROM accounts WHERE email = ?"
             )) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String saved = resultSet.getString("password");
                    return saved != null && saved.equals(password);
                }
            }
        } catch (SQLException ignored) {
        }

        Properties properties = loadProperties();
        String saved = properties.getProperty(normalized + PASSWORD_SUFFIX);
        return saved != null && saved.equals(password);
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
                    "INSERT INTO accounts(email, full_name, password) VALUES (?, ?, ?)"
                )) {
                    insert.setString(1, email);
                    insert.setString(2, properties.getProperty(email + NAME_SUFFIX, ""));
                    insert.setString(3, properties.getProperty(key, ""));
                    insert.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
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
}
