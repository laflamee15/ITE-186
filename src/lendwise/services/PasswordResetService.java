package lendwise.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import lendwise.utils.AccountStore;
import lendwise.utils.MailConfig;

public class PasswordResetService {
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(45);
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountStore accountStore;
    private final MailConfig mailConfig;
    private final Map<String, ResetEntry> entries = new ConcurrentHashMap<>();

    public PasswordResetService(AccountStore accountStore) {
        this.accountStore = accountStore;
        this.mailConfig = new MailConfig();
    }

    public ResetResult requestOtp(String email) {
        String normalized = normalize(email);
        if (normalized.isEmpty()) {
            return ResetResult.error("Enter your email first.");
        }
        if (!accountStore.accountExists(normalized)) {
            return ResetResult.error("No account is registered with that email.");
        }
        if (!mailConfig.isReady()) {
            return ResetResult.configRequired(mailConfig.getConfigPath().toString());
        }

        ResetEntry existing = entries.get(normalized);
        if (existing != null && Instant.now().isBefore(existing.cooldownEndsAt())) {
            long secondsLeft = Duration.between(Instant.now(), existing.cooldownEndsAt()).toSeconds();
            return ResetResult.error("Please wait " + Math.max(1, secondsLeft) + " seconds before requesting another OTP.");
        }

        String otp = generateOtp();
        Instant now = Instant.now();
        entries.put(normalized, new ResetEntry(
            otp,
            now.plus(OTP_VALIDITY),
            now.plus(RESEND_COOLDOWN),
            MAX_VERIFY_ATTEMPTS
        ));

        try {
            sendOtpMessage(normalized, otp);
            return ResetResult.success("OTP sent to " + normalized + ". It expires in 5 minutes.");
        } catch (MessagingException ex) {
            entries.remove(normalized);
            return ResetResult.error("Could not send the OTP email. Check your Gmail config and internet access.");
        }
    }

    public ResetResult resetPassword(String email, String otp, String newPassword) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || otp == null || otp.trim().isEmpty()) {
            return ResetResult.error("Enter the email and OTP.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            return ResetResult.error("Use a password with at least 8 characters.");
        }

        ResetEntry entry = entries.get(normalized);
        if (entry == null) {
            return ResetResult.error("Request a new OTP first.");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            entries.remove(normalized);
            return ResetResult.error("That OTP has expired. Request a new one.");
        }
        if (entry.remainingAttempts() <= 0) {
            entries.remove(normalized);
            return ResetResult.error("Too many incorrect OTP attempts. Request a new OTP.");
        }
        if (!entry.otp().equals(otp.trim())) {
            ResetEntry updated = entry.withRemainingAttempts(entry.remainingAttempts() - 1);
            entries.put(normalized, updated);
            if (updated.remainingAttempts() <= 0) {
                entries.remove(normalized);
                return ResetResult.error("Too many incorrect OTP attempts. Request a new OTP.");
            }
            return ResetResult.error("Incorrect OTP. You have " + updated.remainingAttempts() + " attempt(s) left.");
        }
        if (!accountStore.updatePassword(normalized, newPassword.trim())) {
            return ResetResult.error("Password could not be updated.");
        }

        entries.remove(normalized);
        return ResetResult.success("Password updated successfully. You can sign in now.");
    }

    private void sendOtpMessage(String recipient, String otp) throws MessagingException {
        Session session = createSession();
        MimeMessage message = new MimeMessage(session);
        String senderName = mailConfig.getSenderName().isEmpty() ? "LendWise" : mailConfig.getSenderName();
        try {
            message.setFrom(new InternetAddress(mailConfig.getUsername(), senderName));
        } catch (UnsupportedEncodingException ex) {
            message.setFrom(new InternetAddress(mailConfig.getUsername()));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("LendWise Password Reset OTP");
        message.setText("""
            Hello,

            You requested to reset your LendWise password.

            Your one-time password (OTP) is: %s

            This code expires in 5 minutes. If you did not request this reset, you can ignore this email.

            Thank you,
            LendWise
            """.formatted(otp));
        Transport.send(message);
    }

    private Session createSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getAppPassword());
            }
        });
    }

    private String generateOtp() {
        int value = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim();
    }

    public record ResetResult(boolean success, String message, String configPath) {
        public static ResetResult success(String message) {
            return new ResetResult(true, message, null);
        }

        public static ResetResult error(String message) {
            return new ResetResult(false, message, null);
        }

        public static ResetResult configRequired(String configPath) {
            return new ResetResult(false, "Mail is not configured yet.", configPath);
        }
    }

    private record ResetEntry(String otp, Instant expiresAt, Instant cooldownEndsAt, int remainingAttempts) {
        private ResetEntry withRemainingAttempts(int attempts) {
            return new ResetEntry(otp, expiresAt, cooldownEndsAt, attempts);
        }
    }
}
