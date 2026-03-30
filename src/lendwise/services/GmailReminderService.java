package lendwise.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.utils.MailConfig;

public class GmailReminderService {
    private final MailConfig mailConfig = new MailConfig();

    public ReminderResult sendNewOverdueReminders(List<Borrower> borrowers, List<Loan> loans) {
        return sendSelectedOverdueReminders(borrowers, loans, null);
    }

    public ReminderResult sendSelectedOverdueReminders(List<Borrower> borrowers, List<Loan> loans, Set<String> targetLoanKeys) {
        if (!mailConfig.isReady()) {
            return ReminderResult.notConfigured(mailConfig.getConfigPath().toString());
        }

        Session session = createSession();
        int sentCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (Borrower borrower : borrowers == null ? List.<Borrower>of() : borrowers) {
            if (borrower == null) {
                continue;
            }

            String borrowerId = safe(borrower.getId());
            String recipient = safe(borrower.getGmail());
            if (recipient.isEmpty()) {
                continue;
            }

            for (Loan loan : findOverdueLoansForBorrower(borrowerId, loans)) {
                String loanKey = buildLoanKey(borrowerId, safe(loan.getId()));
                if (targetLoanKeys != null && !targetLoanKeys.contains(loanKey)) {
                    skippedCount++;
                    continue;
                }

                try {
                    sendMessage(session, recipient, borrower, loan);
                    sentCount++;
                } catch (MessagingException ex) {
                    errors.add("Could not send reminder to " + recipient + " for loan " + safe(loan.getId()) + ".");
                }
            }
        }

        return new ReminderResult(sentCount, skippedCount, errors, null);
    }

    public MailConfig getMailConfig() {
        return mailConfig;
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

    private void sendMessage(Session session, String recipient, Borrower borrower, Loan loan) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailConfig.getUsername()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("Payment Reminder from LendWise");
        message.setText(buildMessageBody(borrower, loan));
        Transport.send(message);
    }

    private String buildMessageBody(Borrower borrower, Loan loan) {
        String name = safe(borrower.getFullName());
        String dueDate = loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0
            ? "Not available"
            : loan.getStartDate().plusMonths(loan.getOriginalTermMonths()).toString();
        return "Good day, " + name + ".\n\n"
            + "This is a friendly reminder from LendWise that your loan payment is now overdue. "
            + "We kindly ask that you settle your outstanding balance at your earliest convenience "
            + "to avoid any further charges or inconveniences.\n\n"
            + "Due date: " + dueDate + "\n\n"
            + "If you have already made the payment, please disregard this message. "
            + "Otherwise, we appreciate your prompt attention to this matter.\n\n"
            + "Thank you.";
    }

    private List<Loan> findOverdueLoansForBorrower(String borrowerId, List<Loan> loans) {
        List<Loan> overdueLoans = new ArrayList<>();
        if (loans == null) {
            return overdueLoans;
        }
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }
            String status = safe(loan.getStatus()).trim().toUpperCase();
            java.time.LocalDate dueDate = loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0
                ? null
                : loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
            if (("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(java.time.LocalDate.now())))
                && !"PAID".equals(status)) {
                overdueLoans.add(loan);
            }
        }
        return overdueLoans;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildLoanKey(String borrowerId, String loanId) {
        return safe(borrowerId) + "::" + safe(loanId);
    }

    public record ReminderResult(int sentCount, int skippedCount, List<String> errors, String configPath) {
        public static ReminderResult notConfigured(String configPath) {
            return new ReminderResult(0, 0, List.of(), configPath);
        }

        public static ReminderResult success(int sentCount, int skippedCount) {
            return new ReminderResult(sentCount, skippedCount, List.of(), null);
        }

        public boolean isConfigured() {
            return configPath == null;
        }
    }
}
