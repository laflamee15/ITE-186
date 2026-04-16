package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class UserPanel extends JPanel {
    public enum ViewMode {
        ACCOUNT,
        LOANS,
        PAYMENTS,
        TIMELINE
    }

    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final LoanCalculator calculator;
    private final String username;
    private final String accountEmail;
    private final ViewMode viewMode;

    private final JLabel totalLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalPaidValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel totalPaidBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel remainingValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel remainingBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel nextDueValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel nextDueBody = new JLabel("", SwingConstants.LEFT);

    private final JLabel profileNameValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel profileEmailValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel profileAddressValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel profileBorrowerIdValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel userSubtitleLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel progressPercentValue = new JLabel("0%", SwingConstants.LEFT);
    private final JLabel progressSummaryLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel progressBodyLabel = new JLabel("", SwingConstants.LEFT);
    private final RepaymentProgressBar repaymentProgressBar = new RepaymentProgressBar();
    private final JLabel breakdownPrincipalValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel breakdownInterestValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel breakdownPayableValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel breakdownPaidValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel breakdownRemainingValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel priorityLoanValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel priorityStatusValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel priorityDueValue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel priorityBalanceValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel portfolioPrincipalValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel portfolioMonthlyValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel portfolioCollectedValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel portfolioRemainingValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);

    private final JPanel notificationsList = new JPanel();
    private final JPanel timelineList = new JPanel();
    private final JPanel paymentProgressList = new JPanel();
    private final DefaultTableModel loanTableModel;
    private final DefaultTableModel paymentTableModel;

    public UserPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            String username,
            String accountEmail,
            ViewMode viewMode
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;
        this.calculator = calculator;
        this.username = username == null || username.trim().isEmpty() ? "User" : username.trim();
        this.accountEmail = accountEmail == null ? "" : accountEmail.trim();
        this.viewMode = viewMode == null ? ViewMode.ACCOUNT : viewMode;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        loanTableModel = new DefaultTableModel(
            new Object[]{"LOAN #", "STATUS", "PRINCIPAL", "MONTHLY DUE", "NEXT DUE", "REMAINING"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTableModel = new DefaultTableModel(
            new Object[]{"PAYMENT #", "LOAN #", "AMOUNT", "DATE", "METHOD"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(resolveTitle(), SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        userSubtitleLabel.setForeground(UITheme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(userSubtitleLabel);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        switch (viewMode) {
            case LOANS -> {
                stack.add(buildMetricsRow());
                stack.add(Box.createVerticalStrut(16));
                stack.add(buildLoanInsightsRow());
                stack.add(Box.createVerticalStrut(16));
                stack.add(buildLoanTableCard());
            }
            case PAYMENTS -> {
                stack.add(buildPaymentProgressListCard());
                stack.add(Box.createVerticalStrut(16));
                stack.add(buildPaymentTableCard());
            }
            case TIMELINE -> stack.add(buildAccountTimelineCard());
            default -> {
                stack.add(buildMetricsRow());
                stack.add(Box.createVerticalStrut(16));
                stack.add(buildOverviewRow());
            }
        }

        body.add(stack, BorderLayout.CENTER);
        return body;
    }

    private String resolveTitle() {
        return switch (viewMode) {
            case LOANS -> "Loan Overview";
            case PAYMENTS -> "Payment History";
            case TIMELINE -> "Account Timeline";
            default -> "My Account";
        };
    }

    private JPanel buildMetricsRow() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        if (viewMode == ViewMode.LOANS) {
            cards.add(createMetricCard("ACTIVE", totalLoansValue, totalLoansBody, BLUE));
            cards.add(createMetricCard("PAID", totalPaidValue, totalPaidBody, GREEN));
            cards.add(createMetricCard("DUE IN 7 DAYS", remainingValue, remainingBody, ORANGE));
            cards.add(createMetricCard("OVERDUE", nextDueValue, nextDueBody, RED));
            return cards;
        }
        cards.add(createMetricCard("TOTAL LOANS", totalLoansValue, totalLoansBody, BLUE));
        cards.add(createMetricCard("TOTAL PAID", totalPaidValue, totalPaidBody, GREEN));
        cards.add(createMetricCard("REMAINING BALANCE", remainingValue, remainingBody, ORANGE));
        cards.add(createMetricCard("NEXT DUE", nextDueValue, nextDueBody, RED));
        return cards;
    }

    private JPanel buildOverviewRow() {
        JPanel overview = new JPanel(new GridLayout(1, 2, 14, 0));
        overview.setOpaque(false);
        overview.add(buildProfileCard());
        overview.add(buildNotificationsCard());
        return overview;
    }

    private JPanel buildLoanInsightsRow() {
        JPanel overview = new JPanel(new GridLayout(1, 2, 14, 0));
        overview.setOpaque(false);
        overview.add(buildPriorityCard());
        overview.add(buildPortfolioSnapshotCard());
        return overview;
    }

    private JPanel buildPriorityCard() {
        RoundedPanel card = createSectionCard("Priority Loan", "The loan that needs your attention first.");

        JPanel content = new JPanel(new GridLayout(2, 2, 12, 12));
        content.setOpaque(false);
        content.add(createDetailTile("Loan #", priorityLoanValue));
        content.add(createDetailTile("Status", priorityStatusValue));
        content.add(createDetailTile("Next Due", priorityDueValue));
        content.add(createDetailTile("Remaining", priorityBalanceValue));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPortfolioSnapshotCard() {
        RoundedPanel card = createSectionCard("Portfolio Snapshot", "A quick view of the loan totals behind this account.");

        JPanel content = new JPanel(new GridLayout(2, 2, 12, 12));
        content.setOpaque(false);
        content.add(createDetailTile("Principal", portfolioPrincipalValue));
        content.add(createDetailTile("Monthly Due", portfolioMonthlyValue));
        content.add(createDetailTile("Collected", portfolioCollectedValue));
        content.add(createDetailTile("Remaining", portfolioRemainingValue));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDetailTile(String labelText, JLabel valueLabel) {
        RoundedPanel item = new RoundedPanel(18, UITheme.CARD_2);
        item.setBorderColor(UITheme.BORDER);
        item.setBorderWidth(1);
        item.setLayout(new BorderLayout(0, 8));
        item.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel label = new JLabel(labelText.toUpperCase(), SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT_MUTED);
        Font labelFont = label.getFont();
        if (labelFont != null) {
            label.setFont(labelFont.deriveFont(Font.BOLD, 11f));
        }

        valueLabel.setForeground(UITheme.TEXT);
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 18f));
        }

        item.add(label, BorderLayout.NORTH);
        item.add(valueLabel, BorderLayout.CENTER);
        return item;
    }

    private JPanel buildProgressCard() {
        RoundedPanel card = createSectionCard("Repayment Progress", "");
        card.setBorderColor(UITheme.isLightMode() ? new Color(0xD6, 0xE3, 0xF8) : new Color(0x2A, 0x34, 0x49));
        card.setBorderWidth(1);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        progressSummaryLabel.setForeground(UITheme.TEXT);
        Font summaryFont = progressSummaryLabel.getFont();
        if (summaryFont != null) {
            progressSummaryLabel.setFont(summaryFont.deriveFont(Font.BOLD, 22f));
        }
        progressBodyLabel.setForeground(UITheme.TEXT_MUTED);
        Font bodyFont = progressBodyLabel.getFont();
        if (bodyFont != null) {
            progressBodyLabel.setFont(bodyFont.deriveFont(14f));
        }

        textBlock.add(progressSummaryLabel);
        textBlock.add(Box.createVerticalStrut(8));
        textBlock.add(progressBodyLabel);

        JPanel progressRow = new JPanel(new BorderLayout(14, 0));
        progressRow.setOpaque(false);

        repaymentProgressBar.setPreferredSize(new Dimension(0, 22));

        progressPercentValue.setForeground(UITheme.isLightMode() ? new Color(0x1D, 0x4E, 0xD8) : Color.WHITE);
        Font percentFont = progressPercentValue.getFont();
        if (percentFont != null) {
            progressPercentValue.setFont(percentFont.deriveFont(Font.BOLD, 24f));
        }
        progressPercentValue.setHorizontalAlignment(SwingConstants.RIGHT);
        progressPercentValue.setPreferredSize(new Dimension(72, 22));

        progressRow.add(repaymentProgressBar, BorderLayout.CENTER);
        progressRow.add(progressPercentValue, BorderLayout.EAST);

        content.add(textBlock, BorderLayout.NORTH);
        content.add(progressRow, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPaymentProgressListCard() {
        RoundedPanel card = createSectionCard("Payment Progress", "");

        paymentProgressList.setOpaque(false);
        paymentProgressList.setLayout(new BoxLayout(paymentProgressList, BoxLayout.Y_AXIS));
        card.add(paymentProgressList, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBreakdownCard() {
        RoundedPanel card = createSectionCard("Total Payable Breakdown", "Overall payment summary for all loans linked to this account.");

        JPanel grid = new JPanel(new GridLayout(1, 5, 12, 0));
        grid.setOpaque(false);
        grid.add(createBreakdownItem("Principal", breakdownPrincipalValue));
        grid.add(createBreakdownItem("Interest", breakdownInterestValue));
        grid.add(createBreakdownItem("Total Payable", breakdownPayableValue));
        grid.add(createBreakdownItem("Total Paid", breakdownPaidValue));
        grid.add(createBreakdownItem("Remaining", breakdownRemainingValue));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBreakdownItem(String labelText, JLabel valueLabel) {
        RoundedPanel item = new RoundedPanel(18, UITheme.CARD_2);
        item.setBorderColor(UITheme.BORDER);
        item.setBorderWidth(1);
        item.setLayout(new BorderLayout(0, 8));
        item.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));

        JLabel label = new JLabel(labelText.toUpperCase(), SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT_MUTED);
        Font labelFont = label.getFont();
        if (labelFont != null) {
            label.setFont(labelFont.deriveFont(Font.BOLD, 11f));
        }

        valueLabel.setForeground(UITheme.TEXT);
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 18f));
        }

        item.add(label, BorderLayout.NORTH);
        item.add(valueLabel, BorderLayout.CENTER);
        return item;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, JLabel bodyLabel, Color accent) {
        RoundedPanel card = new RoundedPanel(24, UITheme.metricCardFill(accent));
        card.setBorderColor(accent);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setForeground(UITheme.metricSecondaryText());
        Font titleFont = titleLabel.getFont();
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(Font.BOLD, 12f));
        }

        valueLabel.setForeground(UITheme.metricPrimaryText());
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 30f));
        }

        bodyLabel.setForeground(UITheme.metricSecondaryText());

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

    private JPanel buildProfileCard() {
        RoundedPanel card = createSectionCard("User Profile", "Read-only account snapshot for the selected borrower.");

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 10));
        content.setOpaque(false);
        content.add(createProfileRow("Full name", profileNameValue));
        content.add(createProfileRow("Email", profileEmailValue));
        content.add(createProfileRow("Address", profileAddressValue));
        content.add(createProfileRow("Borrower ID", profileBorrowerIdValue));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildNotificationsCard() {
        RoundedPanel card = createSectionCard("Notifications", "Important updates visible to one user only.");
        notificationsList.setOpaque(false);
        notificationsList.setLayout(new BoxLayout(notificationsList, BoxLayout.Y_AXIS));
        card.add(notificationsList, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAccountTimelineCard() {
        RoundedPanel card = createSectionCard("Account Timeline", "");
        timelineList.setOpaque(false);
        timelineList.setLayout(new BoxLayout(timelineList, BoxLayout.Y_AXIS));
        card.add(timelineList, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLoanTableCard() {
        JTable table = createTable(loanTableModel);
        return buildTableCard("Loan Overview", "All loans tied to this user account.", table);
    }

    private JPanel buildPaymentTableCard() {
        JTable table = createTable(paymentTableModel);
        return buildTableCard("Payment History", "", table);
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(42);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFocusable(false);
        table.setBackground(UITheme.PANEL_BG);
        table.setForeground(UITheme.TEXT);
        table.setGridColor(UITheme.BORDER);
        table.setSelectionBackground(UITheme.CARD_2);
        table.setSelectionForeground(UITheme.TEXT);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, createTableRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(UITheme.tableHeaderBackground());
        header.setForeground(UITheme.tableHeaderForeground());
        header.setOpaque(true);
        header.setDefaultRenderer(createHeaderRenderer());
        Font headerFont = header.getFont();
        if (headerFont != null) {
            header.setFont(headerFont.deriveFont(Font.BOLD, 11f));
        }

        return table;
    }

    private JPanel buildTableCard(String titleText, String subtitleText, JTable table) {
        RoundedPanel card = createSectionCard(titleText, subtitleText);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(0, 320));

        RoundedPanel shell = new RoundedPanel(18, UITheme.PANEL_BG);
        shell.setBorderColor(UITheme.BORDER);
        shell.setBorderWidth(1);
        shell.setLayout(new BorderLayout());
        shell.add(scroll, BorderLayout.CENTER);

        card.add(shell, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel createSectionCard(String titleText, String subtitleText) {
        RoundedPanel card = new RoundedPanel(24, UITheme.PANEL_BG);
        card.setBorderColor(UITheme.BORDER);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText, SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 20f));
        }

        JLabel subtitle = new JLabel(subtitleText, SwingConstants.LEFT);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JPanel createProfileRow(String labelText, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);

        JLabel label = new JLabel(labelText.toUpperCase(), SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setForeground(UITheme.TEXT);
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 15f));
        }

        row.add(label, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private DefaultTableCellRenderer createTableRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(UITheme.PANEL_BG);
                label.setForeground(UITheme.TEXT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                Object firstCell = table.getValueAt(row, 0);
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No ");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }
                if (column == 2 || column == 3 || column == 5) {
                    label.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return label;
            }
        };
    }

    private DefaultTableCellRenderer createHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
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
        Borrower borrower = resolveBorrower();
        ArrayList<Loan> userLoans = loansForBorrower(borrower);
        ArrayList<Payment> userPayments = paymentsForLoans(userLoans);

        profileNameValue.setText(borrower == null ? username : safe(borrower.getFullName()));
        profileEmailValue.setText(borrower == null ? safe(accountEmail) : safe(borrower.getGmail()));
        profileAddressValue.setText(borrower == null ? "No address on file" : fallback(safe(borrower.getAddress()), "No address on file"));
        profileBorrowerIdValue.setText(borrower == null ? "-" : safe(borrower.getId()));

        userSubtitleLabel.setText(
            borrower == null
                ? "Previewing a read-only user view. No borrower matched the signed-in account yet."
                : "Showing a borrower-style view for " + safe(borrower.getFullName()) + "."
        );

        refreshMetrics(userLoans, userPayments);
        refreshProgress(userLoans, userPayments);
        refreshBreakdown(userLoans, userPayments);
        refreshPaymentProgressList(userLoans, userPayments);
        refreshLoanInsights(userLoans, userPayments);
        refreshLoanTable(userLoans, userPayments);
        refreshPaymentTable(userPayments);
        refreshNotifications(userLoans, userPayments);
        refreshAccountTimeline(userLoans, userPayments);
    }

    private void refreshMetrics(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        if (viewMode != ViewMode.LOANS) {
            refreshDefaultMetrics(userLoans, userPayments);
            return;
        }

        int activeCount = 0;
        int paidCount = 0;
        int dueSoonCount = 0;
        int overdueCount = 0;

        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }
            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            String smartStatus = smartStatusForLoan(loan, paidForLoan);
            switch (smartStatus) {
                case "PAID" -> paidCount++;
                case "OVERDUE" -> overdueCount++;
                default -> {
                    if (smartStatus.startsWith("DUE")) {
                        dueSoonCount++;
                    } else {
                        activeCount++;
                    }
                }
            }
        }

        totalLoansValue.setText(Integer.toString(activeCount));
        totalLoansBody.setText(activeCount == 1 ? "loan currently active" : "loans currently active");
        totalPaidValue.setText(Integer.toString(paidCount));
        totalPaidBody.setText(paidCount == 1 ? "loan fully paid" : "loans fully paid");
        remainingValue.setText(Integer.toString(dueSoonCount));
        remainingBody.setText(dueSoonCount == 1 ? "loan due soon" : "loans due soon");
        nextDueValue.setText(Integer.toString(overdueCount));
        nextDueBody.setText(overdueCount == 1 ? "loan needs follow-up" : "loans need follow-up");
    }

    private void refreshDefaultMetrics(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        double totalPaid = 0.0;
        for (Payment payment : userPayments) {
            if (payment != null) {
                totalPaid += payment.getAmount();
            }
        }

        double totalRemaining = 0.0;
        LocalDate nextDue = null;
        int activeCount = 0;
        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }
            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            String smartStatus = smartStatusForLoan(loan, paidForLoan);
            if (!"PAID".equals(smartStatus)) {
                activeCount++;
            }
            totalRemaining += calculator.computeRemainingBalance(loan, paidForLoan);
            LocalDate due = dueDateFor(loan);
            if (due != null && !"PAID".equals(smartStatus)
                    && (nextDue == null || due.isBefore(nextDue))) {
                nextDue = due;
            }
        }

        totalLoansValue.setText(Integer.toString(userLoans.size()));
        totalLoansBody.setText(activeCount + " active or collectible loans");
        totalPaidValue.setText(money(totalPaid));
        totalPaidBody.setText(userPayments.size() + (userPayments.size() == 1 ? " payment recorded" : " payments recorded"));
        remainingValue.setText(money(totalRemaining));
        remainingBody.setText("Outstanding balance across all loans");
        nextDueValue.setText(nextDue == null ? "-" : HUMAN_DATE.format(nextDue));
        nextDueBody.setText(nextDue == null ? "No upcoming due date" : dueStatusText(nextDue));
    }

    private void refreshProgress(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        double totalPaid = 0.0;
        for (Payment payment : userPayments) {
            if (payment != null) {
                totalPaid += payment.getAmount();
            }
        }

        double totalPayable = 0.0;
        for (Loan loan : userLoans) {
            if (loan != null) {
                totalPayable += calculator.computeTotalPayable(loan);
            }
        }

        double percent = totalPayable <= 0.0 ? 0.0 : Math.min(100.0, (totalPaid / totalPayable) * 100.0);
        progressPercentValue.setText(String.format("%.0f%%", percent));
        progressSummaryLabel.setText(money(totalPaid) + " paid out of " + money(totalPayable));
        progressBodyLabel.setText(
            totalPayable <= 0.0
                ? "No loan balance is available yet for progress tracking."
                : money(Math.max(0.0, totalPayable - totalPaid)) + " remaining before this account is fully paid."
        );
        repaymentProgressBar.setProgress(percent / 100.0);
    }

    private void refreshBreakdown(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        double totalPrincipal = 0.0;
        double totalPayable = 0.0;
        double totalPaid = 0.0;

        for (Loan loan : userLoans) {
            if (loan != null) {
                totalPrincipal += loan.getPrincipalAmount();
                totalPayable += calculator.computeTotalPayable(loan);
            }
        }

        for (Payment payment : userPayments) {
            if (payment != null) {
                totalPaid += payment.getAmount();
            }
        }

        double totalInterest = Math.max(0.0, totalPayable - totalPrincipal);
        double remaining = Math.max(0.0, totalPayable - totalPaid);

        breakdownPrincipalValue.setText(money(totalPrincipal));
        breakdownInterestValue.setText(money(totalInterest));
        breakdownPayableValue.setText(money(totalPayable));
        breakdownPaidValue.setText(money(totalPaid));
        breakdownRemainingValue.setText(money(remaining));
    }

    private void refreshPaymentProgressList(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        if (viewMode != ViewMode.PAYMENTS) {
            return;
        }

        paymentProgressList.removeAll();

        if (userLoans.isEmpty()) {
            JLabel empty = new JLabel("No loans found for this user.", SwingConstants.LEFT);
            empty.setForeground(UITheme.TEXT_MUTED);
            paymentProgressList.add(empty);
            paymentProgressList.revalidate();
            paymentProgressList.repaint();
            return;
        }

        boolean first = true;
        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }
            if (!first) {
                paymentProgressList.add(Box.createVerticalStrut(14));
            }
            paymentProgressList.add(createLoanPaymentProgressCard(loan, totalPaidForLoan(loan.getId(), userPayments)));
            first = false;
        }

        paymentProgressList.revalidate();
        paymentProgressList.repaint();
    }

    private JPanel createLoanPaymentProgressCard(Loan loan, double paidForLoan) {
        RoundedPanel card = new RoundedPanel(18, UITheme.CARD_2);
        card.setBorderColor(UITheme.BORDER);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel loanLabel = new JLabel("Loan " + safe(loan.getId()), SwingConstants.LEFT);
        loanLabel.setForeground(UITheme.TEXT);
        loanLabel.setFont(UITheme.createFont(Font.BOLD, 18f));

        double totalPayable = calculator.computeTotalPayable(loan);
        double remaining = calculator.computeRemainingBalance(loan, paidForLoan);
        double percent = totalPayable <= 0.0 ? 0.0 : Math.min(100.0, (paidForLoan / totalPayable) * 100.0);

        JLabel summary = new JLabel(money(paidForLoan) + " paid out of " + money(totalPayable), SwingConstants.LEFT);
        summary.setForeground(UITheme.TEXT);
        summary.setFont(UITheme.createFont(Font.BOLD, 22f));

        JLabel body = new JLabel(money(remaining) + " remaining before this loan is fully paid.", SwingConstants.LEFT);
        body.setForeground(UITheme.TEXT_MUTED);
        body.setFont(UITheme.createFont(Font.PLAIN, 14f));

        textBlock.add(loanLabel);
        textBlock.add(Box.createVerticalStrut(8));
        textBlock.add(summary);
        textBlock.add(Box.createVerticalStrut(8));
        textBlock.add(body);

        RepaymentProgressBar progressBar = new RepaymentProgressBar();
        progressBar.setPreferredSize(new Dimension(0, 22));
        progressBar.setProgress(percent / 100.0);

        JLabel percentLabel = new JLabel(String.format("%.0f%%", percent), SwingConstants.RIGHT);
        percentLabel.setForeground(UITheme.isLightMode() ? new Color(0x1D, 0x4E, 0xD8) : Color.WHITE);
        percentLabel.setFont(UITheme.createFont(Font.BOLD, 24f));
        percentLabel.setPreferredSize(new Dimension(72, 22));

        JPanel progressRow = new JPanel(new BorderLayout(14, 0));
        progressRow.setOpaque(false);
        progressRow.add(progressBar, BorderLayout.CENTER);
        progressRow.add(percentLabel, BorderLayout.EAST);

        content.add(textBlock, BorderLayout.NORTH);
        content.add(progressRow, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void refreshLoanInsights(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        if (viewMode != ViewMode.LOANS) {
            return;
        }

        double totalPrincipal = 0.0;
        double totalCollected = 0.0;
        double totalRemaining = 0.0;
        double totalMonthlyDue = 0.0;

        Loan priorityLoan = null;
        String priorityStatus = "ACTIVE";
        LocalDate priorityDue = null;
        double priorityRemaining = 0.0;

        for (Payment payment : userPayments) {
            if (payment != null) {
                totalCollected += payment.getAmount();
            }
        }

        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }

            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            double remaining = calculator.computeRemainingBalance(loan, paidForLoan);
            String smartStatus = smartStatusForLoan(loan, paidForLoan);
            LocalDate dueDate = dueDateFor(loan);

            totalPrincipal += loan.getPrincipalAmount();
            totalRemaining += remaining;
            totalMonthlyDue += calculator.computeMonthlyInstallment(loan);

            if (priorityLoan == null || compareLoanPriority(loan, smartStatus, dueDate, priorityLoan, priorityStatus, priorityDue) < 0) {
                priorityLoan = loan;
                priorityStatus = smartStatus;
                priorityDue = dueDate;
                priorityRemaining = remaining;
            }
        }

        if (priorityLoan == null) {
            priorityLoanValue.setText("-");
            priorityStatusValue.setText("No loans yet");
            priorityDueValue.setText("-");
            priorityBalanceValue.setText(UITheme.formatCurrency(0.0));
        } else {
            priorityLoanValue.setText(safe(priorityLoan.getId()));
            priorityStatusValue.setText(priorityStatus);
            priorityDueValue.setText(priorityDue == null ? "-" : HUMAN_DATE.format(priorityDue));
            priorityBalanceValue.setText(money(priorityRemaining));
        }

        portfolioPrincipalValue.setText(money(totalPrincipal));
        portfolioMonthlyValue.setText(money(totalMonthlyDue));
        portfolioCollectedValue.setText(money(totalCollected));
        portfolioRemainingValue.setText(money(totalRemaining));
    }

    private int compareLoanPriority(
            Loan candidate,
            String candidateStatus,
            LocalDate candidateDue,
            Loan current,
            String currentStatus,
            LocalDate currentDue
    ) {
        int candidateRank = statusPriorityRank(candidateStatus);
        int currentRank = statusPriorityRank(currentStatus);
        if (candidateRank != currentRank) {
            return Integer.compare(candidateRank, currentRank);
        }

        if (candidateDue == null && currentDue != null) {
            return 1;
        }
        if (candidateDue != null && currentDue == null) {
            return -1;
        }
        if (candidateDue != null && currentDue != null) {
            int dueComparison = candidateDue.compareTo(currentDue);
            if (dueComparison != 0) {
                return dueComparison;
            }
        }

        return safe(candidate.getId()).compareToIgnoreCase(safe(current.getId()));
    }

    private int statusPriorityRank(String status) {
        if ("OVERDUE".equals(status)) {
            return 0;
        }
        if ("DUE TODAY".equals(status)) {
            return 1;
        }
        if (status != null && status.startsWith("DUE IN ")) {
            return 2;
        }
        if ("ACTIVE".equals(status)) {
            return 3;
        }
        if ("PAID".equals(status)) {
            return 4;
        }
        return 5;
    }

    private void refreshLoanTable(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        loanTableModel.setRowCount(0);

        if (userLoans.isEmpty()) {
            loanTableModel.addRow(new Object[]{"No loans found for this user.", "", "", "", "", ""});
            return;
        }

        for (Loan loan : userLoans) {
            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            loanTableModel.addRow(new Object[]{
                safe(loan.getId()),
                smartStatusForLoan(loan, paidForLoan),
                money(loan.getPrincipalAmount()),
                money(calculator.computeMonthlyInstallment(loan)),
                formatDate(dueDateFor(loan)),
                money(calculator.computeRemainingBalance(loan, paidForLoan))
            });
        }
    }

    private void refreshPaymentTable(ArrayList<Payment> userPayments) {
        paymentTableModel.setRowCount(0);

        if (userPayments.isEmpty()) {
            paymentTableModel.addRow(new Object[]{"No payments recorded for this user.", "", "", "", ""});
            return;
        }

        for (Payment payment : userPayments) {
            paymentTableModel.addRow(new Object[]{
                safe(payment.getId()),
                safe(payment.getLoanId()),
                money(payment.getAmount()),
                formatDate(payment.getPaymentDate()),
                fallback(safe(payment.getMethod()), "-")
            });
        }
    }

    private void refreshNotifications(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        notificationsList.removeAll();

        if (userLoans.isEmpty()) {
            notificationsList.add(createNotificationItem("No linked borrower data yet.", UITheme.TEXT_MUTED));
            notificationsList.revalidate();
            notificationsList.repaint();
            return;
        }

        int dueSoonCount = 0;
        int overdueCount = 0;
        int paidCount = 0;
        int activeCount = 0;

        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }
            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            String smartStatus = smartStatusForLoan(loan, paidForLoan);
            switch (smartStatus) {
                case "PAID" -> paidCount++;
                case "OVERDUE" -> overdueCount++;
                default -> {
                    if (smartStatus.startsWith("DUE")) {
                        dueSoonCount++;
                    } else {
                        activeCount++;
                    }
                }
            }
        }

        notificationsList.add(createNotificationItem("Payments recorded: " + userPayments.size(), BLUE));
        notificationsList.add(Box.createVerticalStrut(10));
        notificationsList.add(createNotificationItem("Active loans: " + activeCount, BLUE));
        notificationsList.add(Box.createVerticalStrut(10));
        notificationsList.add(createNotificationItem("Due within 7 days: " + dueSoonCount, ORANGE));
        notificationsList.add(Box.createVerticalStrut(10));
        notificationsList.add(createNotificationItem("Overdue loans: " + overdueCount, RED));
        notificationsList.add(Box.createVerticalStrut(10));
        notificationsList.add(createNotificationItem("Fully paid loans: " + paidCount, GREEN));

        notificationsList.revalidate();
        notificationsList.repaint();
    }

    private void refreshAccountTimeline(ArrayList<Loan> userLoans, ArrayList<Payment> userPayments) {
        timelineList.removeAll();

        List<AccountEvent> events = new ArrayList<>();
        for (Loan loan : userLoans) {
            if (loan == null) {
                continue;
            }

            double paidForLoan = totalPaidForLoan(loan.getId(), userPayments);
            String smartStatus = smartStatusForLoan(loan, paidForLoan);
            LocalDate nextDueDate = dueDateFor(loan);

            if (loan.getStartDate() != null) {
                events.add(new AccountEvent(
                    loan.getStartDate(),
                    "Loan Started",
                    safe(loan.getId()),
                    "Your loan was created with a principal of " + money(loan.getPrincipalAmount()) + ".",
                    BLUE
                ));
            }

            if ("OVERDUE".equals(smartStatus) && nextDueDate != null) {
                events.add(new AccountEvent(
                    nextDueDate,
                    "Overdue",
                    safe(loan.getId()),
                    "This loan passed its scheduled installment date without full payment.",
                    RED
                ));
            } else if ("DUE TODAY".equals(smartStatus) && nextDueDate != null) {
                events.add(new AccountEvent(
                    nextDueDate,
                    "Payment Due Today",
                    safe(loan.getId()),
                    "Your next installment is due today.",
                    ORANGE
                ));
            } else if (smartStatus.startsWith("DUE IN ") && nextDueDate != null) {
                events.add(new AccountEvent(
                    nextDueDate,
                    "Upcoming Payment",
                    safe(loan.getId()),
                    "Your next installment is scheduled on " + HUMAN_DATE.format(nextDueDate) + ".",
                    ORANGE
                ));
            }

            if ("PAID".equals(smartStatus) && loan.getStartDate() != null) {
                LocalDate completionDate = latestPaymentDateForLoan(loan.getId(), userPayments);
                events.add(new AccountEvent(
                    completionDate == null ? loan.getStartDate() : completionDate,
                    "Loan Fully Paid",
                    safe(loan.getId()),
                    "This loan has been completed and no balance remains.",
                    GREEN
                ));
            }
        }

        for (Payment payment : userPayments) {
            if (payment == null) {
                continue;
            }
            events.add(new AccountEvent(
                payment.getPaymentDate(),
                "Paid",
                safe(payment.getLoanId()),
                money(payment.getAmount()) + " received" + (safe(payment.getMethod()).isBlank() ? "." : " via " + safe(payment.getMethod()) + "."),
                GREEN
            ));
        }

        events.sort(Comparator
            .comparing(AccountEvent::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(event -> timelineEventOrder(event.title()))
            .thenComparing(AccountEvent::title));

        if (events.isEmpty()) {
            timelineList.add(createTimelineItem(
                "-",
                "No activity yet",
                "Timeline events will appear here once a loan or payment is recorded.",
                UITheme.TEXT_MUTED
            ));
        } else {
            int maxItems = Math.min(events.size(), 8);
            for (int i = 0; i < maxItems; i++) {
                AccountEvent event = events.get(i);
                timelineList.add(createTimelineItem(
                    formatDate(event.date()),
                    event.title() + " • " + fallback(event.loanId(), "-"),
                    event.description(),
                    event.accent()
                ));
                if (i < maxItems - 1) {
                    timelineList.add(Box.createVerticalStrut(10));
                }
            }
        }

        timelineList.revalidate();
        timelineList.repaint();
    }

    private Component createNotificationItem(String text, Color accent) {
        RoundedPanel item = new RoundedPanel(16, UITheme.CARD_2);
        item.setBorderColor(accent);
        item.setBorderWidth(1);
        item.setLayout(new BorderLayout());
        item.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT);
        item.add(label, BorderLayout.CENTER);
        return item;
    }

    private Component createTimelineItem(String dateText, String titleText, String bodyText, Color accent) {
        RoundedPanel item = new RoundedPanel(18, UITheme.CARD_2);
        item.setBorderColor(accent == null ? UITheme.BORDER : accent);
        item.setBorderWidth(1);
        item.setLayout(new BorderLayout(14, 0));
        item.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel dateLabel = new JLabel(dateText, SwingConstants.LEFT);
        dateLabel.setForeground(UITheme.TEXT_MUTED);
        Font dateFont = dateLabel.getFont();
        if (dateFont != null) {
            dateLabel.setFont(dateFont.deriveFont(Font.BOLD, 12f));
        }
        dateLabel.setPreferredSize(new Dimension(120, 0));

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(titleText, SwingConstants.LEFT);
        titleLabel.setForeground(UITheme.TEXT);
        Font titleFont = titleLabel.getFont();
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(Font.BOLD, 15f));
        }

        JLabel bodyLabel = new JLabel("<html>" + bodyText + "</html>", SwingConstants.LEFT);
        bodyLabel.setForeground(UITheme.TEXT_MUTED);

        textWrap.add(titleLabel);
        textWrap.add(Box.createVerticalStrut(4));
        textWrap.add(bodyLabel);

        item.add(dateLabel, BorderLayout.WEST);
        item.add(textWrap, BorderLayout.CENTER);
        return item;
    }

    private Borrower resolveBorrower() {
        if (borrowers == null || borrowers.isEmpty()) {
            return null;
        }

        for (Borrower borrower : borrowers) {
            if (borrower == null) {
                continue;
            }
            if (!accountEmail.isBlank() && accountEmail.equalsIgnoreCase(safe(borrower.getLinkedAccountEmail()))) {
                return borrower;
            }
            if (!accountEmail.isBlank() && safe(borrower.getLinkedAccountEmail()).isBlank()
                    && accountEmail.equalsIgnoreCase(safe(borrower.getGmail()))) {
                return borrower;
            }
        }

        return borrowers.get(0);
    }

    private ArrayList<Loan> loansForBorrower(Borrower borrower) {
        ArrayList<Loan> filtered = new ArrayList<>();
        if (borrower == null || loans == null) {
            return filtered;
        }
        String borrowerId = safe(borrower.getId());
        for (Loan loan : loans) {
            if (loan != null && borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                filtered.add(loan);
            }
        }
        return filtered;
    }

    private ArrayList<Payment> paymentsForLoans(ArrayList<Loan> userLoans) {
        ArrayList<Payment> filtered = new ArrayList<>();
        if (payments == null || userLoans == null || userLoans.isEmpty()) {
            return filtered;
        }
        Set<String> loanIds = new HashSet<>();
        for (Loan loan : userLoans) {
            if (loan != null) {
                loanIds.add(safe(loan.getId()));
            }
        }
        for (Payment payment : payments) {
            if (payment != null && loanIds.contains(safe(payment.getLoanId()))) {
                filtered.add(payment);
            }
        }
        return filtered;
    }

    private double totalPaidForLoan(String loanId, ArrayList<Payment> userPayments) {
        double total = 0.0;
        if (userPayments == null) {
            return total;
        }
        for (Payment payment : userPayments) {
            if (payment != null && safe(loanId).equalsIgnoreCase(safe(payment.getLoanId()))) {
                total += payment.getAmount();
            }
        }
        return total;
    }

    private LocalDate latestPaymentDateForLoan(String loanId, ArrayList<Payment> userPayments) {
        LocalDate latest = null;
        if (userPayments == null) {
            return latest;
        }
        for (Payment payment : userPayments) {
            if (payment == null || !safe(loanId).equalsIgnoreCase(safe(payment.getLoanId()))) {
                continue;
            }
            LocalDate paymentDate = payment.getPaymentDate();
            if (paymentDate != null && (latest == null || paymentDate.isAfter(latest))) {
                latest = paymentDate;
            }
        }
        return latest;
    }

    private int timelineEventOrder(String title) {
        if ("Loan Started".equals(title)) {
            return 0;
        }
        if ("Upcoming Payment".equals(title)) {
            return 1;
        }
        if ("Payment Due Today".equals(title)) {
            return 2;
        }
        if ("Paid".equals(title)) {
            return 3;
        }
        if ("Overdue".equals(title)) {
            return 4;
        }
        if ("Loan Fully Paid".equals(title)) {
            return 5;
        }
        return 6;
    }

    private LocalDate dueDateFor(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        double paidForLoan = totalPaidForLoan(loan.getId(), payments);
        int remainingTermMonths = calculator.computeRemainingTermMonths(loan, paidForLoan);
        if ("PAID".equals(normalizeStatus(loan.getStatus())) || remainingTermMonths <= 0) {
            return null;
        }

        int completedInstallments = Math.max(0, loan.getOriginalTermMonths() - remainingTermMonths);
        int nextInstallmentNumber = completedInstallments + 1;
        if (nextInstallmentNumber > loan.getOriginalTermMonths()) {
            nextInstallmentNumber = loan.getOriginalTermMonths();
        }
        return loan.getStartDate().plusMonths(nextInstallmentNumber);
    }

    private String dueStatusText(LocalDate dueDate) {
        if (dueDate == null) {
            return "";
        }
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return "Past due";
        }
        if (dueDate.equals(today)) {
            return "Due today";
        }
        if (!dueDate.isAfter(today.plusDays(7))) {
            return "Due within 7 days";
        }
        return "Upcoming payment schedule";
    }

    private String smartStatusForLoan(Loan loan, double paidForLoan) {
        if (loan == null) {
            return "UNKNOWN";
        }

        String normalizedStatus = normalizeStatus(loan.getStatus());
        double remainingBalance = calculator.computeRemainingBalance(loan, paidForLoan);
        if (remainingBalance <= 0.0 || "PAID".equals(normalizedStatus)) {
            return "PAID";
        }
        if ("OVERDUE".equals(normalizedStatus)) {
            return "OVERDUE";
        }

        LocalDate dueDate = dueDateFor(loan);
        if (dueDate == null) {
            return "ACTIVE";
        }

        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return "OVERDUE";
        }
        if (dueDate.equals(today)) {
            return "DUE TODAY";
        }

        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
        if (daysLeft <= 7) {
            return "DUE IN " + daysLeft + " DAY" + (daysLeft == 1 ? "" : "S");
        }
        return "ACTIVE";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : HUMAN_DATE.format(date);
    }

    private String normalizeStatus(String status) {
        return fallback(safe(status).trim().toUpperCase(), "UNKNOWN");
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

    private static final class RepaymentProgressBar extends JPanel {
        private double progress;

        private RepaymentProgressBar() {
            setOpaque(false);
        }

        private void setProgress(double progress) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = height;

            Color trackTop = UITheme.isLightMode() ? new Color(0xE9, 0xF1, 0xFB) : new Color(0x1B, 0x23, 0x33);
            Color trackBottom = UITheme.isLightMode() ? new Color(0xD9, 0xE6, 0xF5) : new Color(0x14, 0x1B, 0x28);
            g2.setPaint(new GradientPaint(0, 0, trackTop, 0, height, trackBottom));
            g2.fillRoundRect(0, 0, width, height, arc, arc);

            int fillWidth = (int) Math.round(width * progress);
            if (fillWidth > 0) {
                Color fillStart = UITheme.isLightMode() ? new Color(0x34, 0xD3, 0x99) : new Color(0x22, 0xC5, 0x5E);
                Color fillEnd = UITheme.isLightMode() ? new Color(0x0F, 0xA5, 0xE9) : new Color(0x25, 0x63, 0xEB);
                g2.setPaint(new GradientPaint(0, 0, fillStart, fillWidth, 0, fillEnd));
                g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);

                int sheenWidth = Math.max(18, Math.min(56, fillWidth / 3));
                g2.setColor(new Color(255, 255, 255, UITheme.isLightMode() ? 70 : 42));
                g2.fillRoundRect(Math.max(0, fillWidth - sheenWidth), 2, sheenWidth, Math.max(4, height - 4), arc, arc);
            }

            g2.setColor(UITheme.BORDER);
            g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g2.dispose();
        }
    }

    private record AccountEvent(LocalDate date, String title, String loanId, String description, Color accent) {
    }
}
