package lendwise.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    }

    public boolean accountExists(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return false;
        }
        return loadProperties().containsKey(normalized + PASSWORD_SUFFIX);
    }

    public boolean createAccount(String fullName, String email, String password) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
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
        Properties properties = loadProperties();
        String saved = properties.getProperty(normalized + PASSWORD_SUFFIX);
        return saved != null && saved.equals(password);
    }

    public String getDisplayName(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return "";
        }
        Properties properties = loadProperties();
        String fullName = properties.getProperty(normalized + NAME_SUFFIX, "").trim();
        return fullName.isEmpty() ? normalized : fullName;
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
