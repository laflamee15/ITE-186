package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class AdminTransactionHistoryPanel extends JPanel {
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final LoanCalculator calculator;
    private final Runnable saveAction;

    private final JLabel totalEventsValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalEventsBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel paymentEventsValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel paymentEventsBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel loanStartsValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel loanStartsBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel latestActivityValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel latestActivityBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel subtitleLabel = new JLabel("", SwingConstants.LEFT);

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton deleteButton = new JButton("Delete");
    private final List<AdminEvent> currentEvents = new ArrayList<>();

    public AdminTransactionHistoryPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            Runnable saveAction
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;
        this.calculator = calculator == null ? new LoanCalculator() : calculator;
        this.saveAction = saveAction == null ? () -> {} : saveAction;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(
            new Object[]{"DATE", "EVENT", "LOAN #", "BORROWER", "DETAILS"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = createTable();

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Transaction History", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        title.setFont(UITheme.pageTitleFont());

        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setFont(UITheme.UI_FONT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitleLabel);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
        metrics.setOpaque(false);
        metrics.add(createMetricCard("TOTAL EVENTS", totalEventsValue, totalEventsBody, BLUE));
        metrics.add(createMetricCard("PAYMENTS", paymentEventsValue, paymentEventsBody, GREEN));
        metrics.add(createMetricCard("LOANS STARTED", loanStartsValue, loanStartsBody, ORANGE));
        metrics.add(createMetricCard("LATEST ACTIVITY", latestActivityValue, latestActivityBody, RED));

        RoundedPanel tableCard = new RoundedPanel(24, UITheme.PANEL_BG);
        tableCard.setBorderColor(UITheme.BORDER);
        tableCard.setBorderWidth(1);
        tableCard.setLayout(new BorderLayout(0, 16));
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel tableHeader = new JPanel();
        tableHeader.setOpaque(false);
        tableHeader.setLayout(new BorderLayout());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel tableTitle = new JLabel("Recorded Activity", SwingConstants.LEFT);
        tableTitle.setForeground(UITheme.TEXT);
        tableTitle.setFont(UITheme.sectionTitleFont());

        JLabel tableSubtitle = new JLabel("Loan creation and payment records across all borrowers.", SwingConstants.LEFT);
        tableSubtitle.setForeground(UITheme.TEXT_MUTED);
        tableSubtitle.setFont(UITheme.UI_FONT_SMALL);

        left.add(tableTitle);
        left.add(Box.createVerticalStrut(4));
        left.add(tableSubtitle);

        UITheme.applyHeaderActionButton(deleteButton, false);
        deleteButton.addActionListener(e -> handleDelete());

        tableHeader.add(left, BorderLayout.WEST);
        tableHeader.add(deleteButton, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 440));

        RoundedPanel tableShell = new RoundedPanel(18, UITheme.PANEL_BG);
        tableShell.setBorderColor(UITheme.BORDER);
        tableShell.setBorderWidth(1);
        tableShell.setLayout(new BorderLayout());
        tableShell.add(scroll, BorderLayout.CENTER);

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(tableShell, BorderLayout.CENTER);

        body.add(metrics, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        return body;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, JLabel bodyLabel, Color accent) {
        RoundedPanel card = new RoundedPanel(24, UITheme.metricCardFill(accent));
        card.setBorderColor(accent);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setForeground(UITheme.metricSecondaryText());
        titleLabel.setFont(UITheme.metricLabelFont());

        valueLabel.setForeground(UITheme.metricPrimaryText());
        valueLabel.setFont(UITheme.metricValueFont().deriveFont(30f));

        bodyLabel.setForeground(UITheme.metricSecondaryText());
        bodyLabel.setFont(UITheme.UI_FONT_SMALL);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(10));
        center.add(bodyLabel);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JTable createTable() {
        JTable table = new JTable(tableModel);
        table.setRowHeight(52);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFocusable(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(UITheme.UI_FONT);
        table.setBackground(UITheme.PANEL_BG);
        table.setForeground(UITheme.TEXT);
        table.setGridColor(UITheme.BORDER);
        table.setSelectionBackground(UITheme.isLightMode() ? new Color(0xD9, 0xE8, 0xFF) : new Color(0x24, 0x35, 0x52));
        table.setSelectionForeground(UITheme.TEXT);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, createRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableColumnModel columns = (DefaultTableColumnModel) table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(108);
        columns.getColumn(1).setPreferredWidth(156);
        columns.getColumn(2).setPreferredWidth(102);
        columns.getColumn(3).setPreferredWidth(158);
        columns.getColumn(4).setPreferredWidth(360);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(UITheme.tableHeaderBackground());
        header.setForeground(UITheme.tableHeaderForeground());
        header.setOpaque(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.tableHeaderDivider()));
        header.setDefaultRenderer(createHeaderRenderer());
        header.setFont(UITheme.captionFont().deriveFont(Font.BOLD, 11f));

        return table;
    }

    private DefaultTableCellRenderer createRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(isSelected
                    ? (UITheme.isLightMode() ? new Color(0xE8, 0xF1, 0xFF) : new Color(0x1E, 0x2A, 0x3F))
                    : UITheme.PANEL_BG);
                label.setForeground(UITheme.TEXT);
                label.setFont(UITheme.UI_FONT);
                label.setBorder(BorderFactory.createEmptyBorder(0, column == 0 && isSelected ? 8 : 12, 0, 12));

                Object firstCell = table.getValueAt(row, 0);
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No activity recorded");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }

                if (column == 0) {
                    label.setForeground(new Color(0x9C, 0xB8, 0xFF));
                    String text = value == null ? "" : value.toString();
                    label.setText(isSelected ? "\u25B6 " + text : text);
                }
                if (column == 4) {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                } else {
                    label.setHorizontalAlignment(column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                }
                return label;
            }
        };
    }

    private DefaultTableCellRenderer createHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(UITheme.tableHeaderBackground());
                label.setForeground(UITheme.tableHeaderForeground());
                label.setFont(UITheme.captionFont().deriveFont(Font.BOLD, 11f));
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, UITheme.tableHeaderDivider()),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        };
    }

    public void refresh() {
        List<AdminEvent> events = buildEvents();
        currentEvents.clear();
        currentEvents.addAll(events);
        tableModel.setRowCount(0);

        int paymentCount = 0;
        int loanStartCount = 0;
        for (AdminEvent event : events) {
            if ("Payment Recorded".equals(event.type())) {
                paymentCount++;
            }
            if ("Loan Started".equals(event.type())) {
                loanStartCount++;
            }
        }

        totalEventsValue.setText(Integer.toString(events.size()));
        totalEventsBody.setText(events.size() == 1 ? "recorded event" : "recorded events");

        paymentEventsValue.setText(Integer.toString(paymentCount));
        paymentEventsBody.setText(paymentCount == 1 ? "payment logged" : "payments logged");

        loanStartsValue.setText(Integer.toString(loanStartCount));
        loanStartsBody.setText(loanStartCount == 1 ? "loan created" : "loans created");

        if (events.isEmpty()) {
            latestActivityValue.setText("-");
            latestActivityBody.setText("No records yet");
            subtitleLabel.setText("No loan or payment records available.");
            tableModel.addRow(new Object[]{"No activity recorded yet.", "", "", "", ""});
            return;
        }

        AdminEvent latest = events.get(0);
        latestActivityValue.setText(formatDate(latest.date()));
        latestActivityBody.setText(latest.type() + " - " + safe(latest.loanId()));
        subtitleLabel.setText("Showing " + events.size() + " records across all borrowers.");

        for (AdminEvent event : events) {
            tableModel.addRow(new Object[]{
                formatDate(event.date()),
                event.type(),
                safe(event.loanId()),
                safe(event.borrowerName()),
                "<html><div style='width:320px;'>" + escapeHtml(event.details()) + "</div></html>"
            });
        }
    }

    private void handleDelete() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentEvents.size()) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a transaction row to delete.",
                "Transaction History",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        AdminEvent event = currentEvents.get(selectedRow);
        if ("Payment Recorded".equals(event.type())) {
            Payment payment = findMatchingPayment(event);
            if (payment == null) {
                JOptionPane.showMessageDialog(this, "The selected payment could not be resolved.", "Transaction History", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete payment for loan \"" + safe(payment.getLoanId()) + "\" dated " + formatDate(payment.getPaymentDate()) + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            payments.remove(payment);
            updateLoanAfterHistoryDelete(payment.getLoanId());
            saveAction.run();
            refresh();
            return;
        }

        if ("Loan Started".equals(event.type())) {
            Loan loan = findLoanById(event.loanId());
            if (loan == null) {
                JOptionPane.showMessageDialog(this, "The selected loan could not be resolved.", "Transaction History", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete loan \"" + safe(loan.getId()) + "\" and its related payments?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            payments.removeIf(payment -> payment != null && safe(loan.getId()).equalsIgnoreCase(safe(payment.getLoanId())));
            loans.remove(loan);
            saveAction.run();
            refresh();
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            "Only payment and loan-start records can be deleted from this view.",
            "Transaction History",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private List<AdminEvent> buildEvents() {
        List<AdminEvent> events = new ArrayList<>();

        if (loans != null) {
            for (Loan loan : loans) {
                if (loan == null) {
                    continue;
                }

                String loanId = safe(loan.getId());
                String borrowerName = resolveBorrowerName(loan.getBorrowerId());

                if (loan.getStartDate() != null) {
                    events.add(new AdminEvent(
                        loan.getStartDate(),
                        "Loan Started",
                        loanId,
                        borrowerName,
                        "Principal " + money(loan.getPrincipalAmount()) + " assigned to " + fallback(safe(loan.getCollectorName()), "no collector") + "."
                    ));
                }

                double paidForLoan = totalPaidForLoan(loanId);
                double remainingBalance = calculator.computeRemainingBalance(loan, paidForLoan);
                LocalDate latestPaymentDate = latestPaymentDateForLoan(loanId);
                if (remainingBalance <= 0.0 && latestPaymentDate != null) {
                    events.add(new AdminEvent(
                        latestPaymentDate,
                        "Loan Fully Paid",
                        loanId,
                        borrowerName,
                        "Loan balance reached " + money(0.0) + "."
                    ));
                }
            }
        }

        if (payments != null) {
            for (Payment payment : payments) {
                if (payment == null) {
                    continue;
                }
                String loanId = safe(payment.getLoanId());
                events.add(new AdminEvent(
                    payment.getPaymentDate(),
                    "Payment Recorded",
                    loanId,
                    resolveBorrowerNameForLoan(loanId),
                    money(payment.getAmount()) + " via " + fallback(safe(payment.getMethod()), "unspecified method") + "."
                ));
            }
        }

        events.sort(Comparator
            .comparing(AdminEvent::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed()
            .thenComparingInt(event -> eventPriority(event.type()))
            .thenComparing(AdminEvent::loanId, String.CASE_INSENSITIVE_ORDER));

        return events;
    }

    private int eventPriority(String type) {
        if ("Payment Recorded".equals(type)) {
            return 0;
        }
        if ("Loan Fully Paid".equals(type)) {
            return 1;
        }
        if ("Loan Started".equals(type)) {
            return 2;
        }
        return 3;
    }

    private String resolveBorrowerNameForLoan(String loanId) {
        Loan loan = findLoanById(loanId);
        return loan == null ? safe(loanId) : resolveBorrowerName(loan.getBorrowerId());
    }

    private String resolveBorrowerName(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return "";
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                return fallback(safe(borrower.getFullName()), safe(borrower.getId()));
            }
        }
        return borrowerId;
    }

    private Loan findLoanById(String loanId) {
        if (loans == null || loanId == null) {
            return null;
        }
        for (Loan loan : loans) {
            if (loan != null && loanId.equalsIgnoreCase(safe(loan.getId()))) {
                return loan;
            }
        }
        return null;
    }

    private double totalPaidForLoan(String loanId) {
        double total = 0.0;
        if (payments == null || loanId == null) {
            return total;
        }
        for (Payment payment : payments) {
            if (payment != null && loanId.equalsIgnoreCase(safe(payment.getLoanId()))) {
                total += payment.getAmount();
            }
        }
        return total;
    }

    private LocalDate latestPaymentDateForLoan(String loanId) {
        LocalDate latest = null;
        if (payments == null || loanId == null) {
            return latest;
        }
        for (Payment payment : payments) {
            if (payment == null || !loanId.equalsIgnoreCase(safe(payment.getLoanId()))) {
                continue;
            }
            LocalDate paymentDate = payment.getPaymentDate();
            if (paymentDate != null && (latest == null || paymentDate.isAfter(latest))) {
                latest = paymentDate;
            }
        }
        return latest;
    }

    private Payment findMatchingPayment(AdminEvent event) {
        if (payments == null || event == null) {
            return null;
        }
        for (Payment payment : payments) {
            if (payment == null) {
                continue;
            }
            if (safe(event.loanId()).equalsIgnoreCase(safe(payment.getLoanId()))
                    && ((event.date() == null && payment.getPaymentDate() == null)
                    || (event.date() != null && event.date().equals(payment.getPaymentDate())))) {
                return payment;
            }
        }
        return null;
    }

    private void updateLoanAfterHistoryDelete(String loanId) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            return;
        }

        double totalPaid = totalPaidForLoan(loanId);
        double remaining = calculator.computeRemainingBalance(loan, totalPaid);
        int remainingTerm = calculator.computeRemainingTermMonths(loan, totalPaid);
        loan.setTermMonths(remainingTerm);

        if (remaining <= 0.0) {
            loan.setStatus("PAID");
            return;
        }

        LocalDate dueDate = resolveDueDate(loan);
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            loan.setStatus("OVERDUE");
        } else {
            loan.setStatus("ACTIVE");
        }
    }

    private LocalDate resolveDueDate(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        double paidForLoan = totalPaidForLoan(safe(loan.getId()));
        int remainingTermMonths = calculator.computeRemainingTermMonths(loan, paidForLoan);
        if ("PAID".equalsIgnoreCase(safe(loan.getStatus())) || remainingTermMonths <= 0) {
            return null;
        }
        int completedInstallments = Math.max(0, loan.getOriginalTermMonths() - remainingTermMonths);
        int nextInstallmentNumber = completedInstallments + 1;
        if (nextInstallmentNumber > loan.getOriginalTermMonths()) {
            nextInstallmentNumber = loan.getOriginalTermMonths();
        }
        return loan.getStartDate().plusMonths(nextInstallmentNumber);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : HUMAN_DATE.format(date);
    }

    private String escapeHtml(String value) {
        return safe(value)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private String money(double amount) {
        return UITheme.formatCurrency(amount);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private record AdminEvent(LocalDate date, String type, String loanId, String borrowerName, String details) {
    }
}
