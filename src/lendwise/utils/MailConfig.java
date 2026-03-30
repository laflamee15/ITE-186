package lendwise.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MailConfig {
    private static final Path CONFIG_PATH = Paths.get("data", "mail.properties");

    public MailConfig() {
        ensureTemplateExists();
    }

    public boolean isReady() {
        Properties properties = load();
        return Boolean.parseBoolean(properties.getProperty("mail.enabled", "false"))
            && !getUsername().isEmpty()
            && !getAppPassword().isEmpty();
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(load().getProperty("mail.enabled", "false"));
    }

    public String getUsername() {
        return load().getProperty("smtp.gmail.username", "").trim();
    }

    public String getAppPassword() {
        return load().getProperty("smtp.gmail.appPassword", "").trim();
    }

    public String getSenderName() {
        return load().getProperty("smtp.gmail.senderName", "LendWise").trim();
    }

    public Path getConfigPath() {
        return CONFIG_PATH;
    }

    private Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(CONFIG_PATH)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void ensureTemplateExists() {
        if (Files.exists(CONFIG_PATH)) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("mail.enabled", "false");
        properties.setProperty("smtp.gmail.username", "");
        properties.setProperty("smtp.gmail.appPassword", "");
        properties.setProperty("smtp.gmail.senderName", "LendWise");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output,
                    """
                    Set mail.enabled=true after entering your sender Gmail and Gmail App Password.
                    smtp.gmail.username = the Gmail account that will send reminders
                    smtp.gmail.appPassword = Google App Password, not your normal Gmail password
                    smtp.gmail.senderName = display name shown to borrowers
                    """);
            }
        } catch (IOException ignored) {
        }
    }
}
