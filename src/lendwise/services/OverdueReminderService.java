package lendwise.services;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import lendwise.models.Borrower;
import lendwise.models.Loan;

public class OverdueReminderService {
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public List<ReminderEmail> getOverdueReminders(List<Borrower> borrowers, List<Loan> loans) {
        List<ReminderEmail> reminders = new ArrayList<>();
        if (borrowers == null || loans == null) {
            return reminders;
        }

        for (Borrower borrower : borrowers) {
            if (borrower == null) {
                continue;
            }
            String email = safe(borrower.getGmail()).trim();
            if (!looksLikeEmail(email)) {
                continue;
            }

            List<Loan> overdueLoans = findOverdueLoansForBorrower(safe(borrower.getId()), loans);
            if (overdueLoans.isEmpty()) {
                continue;
            }

            reminders.add(new ReminderEmail(
                email,
                "LendWise overdue payment reminder for " + safe(borrower.getFullName()),
                buildBody(borrower, overdueLoans)
            ));
        }
        return reminders;
    }

    public int sendOverdueReminders(Component parent, List<Borrower> borrowers, List<Loan> loans) {
        List<ReminderEmail> reminders = getOverdueReminders(borrowers, loans);
        if (reminders.isEmpty()) {
            JOptionPane.showMessageDialog(
                parent,
                "No overdue borrowers with a saved Gmail address were found.",
                "Overdue Reminders",
                JOptionPane.INFORMATION_MESSAGE
            );
            return 0;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            JOptionPane.showMessageDialog(
                parent,
                "Email launching is not supported on this device. A desktop mail app is required.",
                "Overdue Reminders",
                JOptionPane.WARNING_MESSAGE
            );
            return 0;
        }

        int opened = 0;
        Set<String> sentTargets = new LinkedHashSet<>();
        for (ReminderEmail reminder : reminders) {
            String email = safe(reminder.email()).trim().toLowerCase();
            if (!sentTargets.add(email)) {
                continue;
            }
            try {
                Desktop.getDesktop().mail(buildMailTo(reminder));
                opened++;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    parent,
                    "Could not open the mail app for " + reminder.email() + ".",
                    "Overdue Reminders",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        }
        return opened;
    }

    public int countOverdueBorrowers(List<Borrower> borrowers, List<Loan> loans) {
        return getOverdueReminders(borrowers, loans).size();
    }

    private List<Loan> findOverdueLoansForBorrower(String borrowerId, List<Loan> loans) {
        List<Loan> overdueLoans = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }

            String status = safe(loan.getStatus()).trim().toUpperCase();
            LocalDate dueDate = dueDateFor(loan);
            boolean overdue = ("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today)))
                && !"PAID".equals(status);
            if (overdue) {
                overdueLoans.add(loan);
            }
        }
        return overdueLoans;
    }

    private URI buildMailTo(ReminderEmail reminder) {
        String query = "subject=" + encode(reminder.subject()) + "&body=" + encode(reminder.body());
        return URI.create("mailto:" + encodeEmail(reminder.email()) + "?" + query);
    }

    private String buildBody(Borrower borrower, List<Loan> overdueLoans) {
        StringBuilder builder = new StringBuilder();
        builder.append("Good day ").append(safe(borrower.getFullName())).append(",\n\n");
        builder.append("This is a reminder that your loan account has overdue payment(s).\n\n");
        for (Loan loan : overdueLoans) {
            builder.append("Loan ID: ").append(safe(loan.getId())).append('\n');
            LocalDate dueDate = dueDateFor(loan);
            builder.append("Due Date: ").append(dueDate == null ? "Not available" : HUMAN_DATE.format(dueDate)).append('\n');
            String collector = safe(loan.getCollectorName());
            builder.append("Collector: ").append(collector.isEmpty() ? "-" : collector).append("\n\n");
        }
        builder.append("Please settle the overdue amount as soon as possible or contact the collector for assistance.\n\n");
        builder.append("Thank you,\nLendWise");
        return builder.toString();
    }

    private LocalDate dueDateFor(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        return loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
    }

    private boolean looksLikeEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeEmail(String value) {
        return safe(value).replace(" ", "%20");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record ReminderEmail(String email, String subject, String body) {
    }
}
