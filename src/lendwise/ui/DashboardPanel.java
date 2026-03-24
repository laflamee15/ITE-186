package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

public class DashboardPanel extends JPanel {
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final JLabel totalCollectionValue = new JLabel("₱0.00", SwingConstants.LEFT);
    private final JLabel totalCollectionBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalCollectionFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalCollectionPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel activeLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel activeLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel dueSoonValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel dueSoonBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel dueSoonFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel dueSoonPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel overdueValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel overdueBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel overdueFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel overduePill = new JLabel("", SwingConstants.CENTER);

    private final JLabel subTitleLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel tableDescriptionLabel = new JLabel("", SwingConstants.LEFT);

    private final DefaultTableModel tableModel;
    private final JTable collectionTable;

    public DashboardPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            String username
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(new Object[]{"LOAN #", "BORROWER", "AMOUNT", "RECORDED AT", "COLLECTOR"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        collectionTable = new JTable(tableModel);
        collectionTable.setRowHeight(44);
        collectionTable.setShowHorizontalLines(true);
        collectionTable.setShowVerticalLines(false);
        collectionTable.setFocusable(false);
        collectionTable.setBackground(UITheme.PANEL_BG);
        collectionTable.setForeground(UITheme.TEXT);
        collectionTable.setGridColor(UITheme.BORDER);
        collectionTable.setSelectionBackground(UITheme.CARD_2);
        collectionTable.setSelectionForeground(UITheme.TEXT);
        collectionTable.setFillsViewportHeight(true);
        collectionTable.setDefaultRenderer(Object.class, createCollectionRenderer());

        JTableHeader header = collectionTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(UITheme.tableHeaderBackground());
        header.setForeground(UITheme.tableHeaderForeground());
        header.setOpaque(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.tableHeaderDivider()));
        header.setDefaultRenderer(createHeaderRenderer());
        Font headerFont = header.getFont();
        if (headerFont != null) {
            header.setFont(headerFont.deriveFont(Font.BOLD, 11f));
        }

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard - Daily collection", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        subTitleLabel.setForeground(UITheme.TEXT_MUTED);
        Font subtitleFont = subTitleLabel.getFont();
        if (subtitleFont != null) {
            subTitleLabel.setFont(subtitleFont.deriveFont(14f));
        }

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subTitleLabel);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(createMetricCard("TOTAL DAILY COLLECTION", totalCollectionValue, totalCollectionBody, totalCollectionFooter, totalCollectionPill, "Today", BLUE));
        cards.add(createMetricCard("ACTIVE LOANS", activeLoansValue, activeLoansBody, activeLoansFooter, activeLoansPill, "On track", GREEN));
        cards.add(createMetricCard("DUE WITHIN 7 DAYS", dueSoonValue, dueSoonBody, dueSoonFooter, dueSoonPill, "Attention", ORANGE));
        cards.add(createMetricCard("OVERDUE LOANS", overdueValue, overdueBody, overdueFooter, overduePill, "Risk", RED));

        body.add(cards, BorderLayout.NORTH);
        body.add(buildCollectionsCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel createMetricCard(
            String title,
            JLabel valueLabel,
            JLabel bodyLabel,
            JLabel footerLabel,
            JLabel footerPill,
            String badgeText,
            Color accent
    ) {
        RoundedPanel card = new RoundedPanel(24, UITheme.metricCardFill(accent));
        card.setBorderColor(accent);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setForeground(UITheme.metricSecondaryText());
        Font titleFont = titleLabel.getFont();
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(Font.BOLD, 12f));
        }

        JLabel badge = new JLabel(buildPillText(dotSymbol(accent), badgeText), SwingConstants.CENTER);
        configurePill(
            badge,
            UITheme.metricPillBorder(),
            UITheme.metricPrimaryText(),
            18,
            8
        );
        badge.setBackground(UITheme.metricPillFill(accent));

        top.add(titleLabel, BorderLayout.WEST);
        top.add(badge, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        valueLabel.setForeground(UITheme.metricPrimaryText());
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 34f));
        }

        bodyLabel.setForeground(UITheme.metricSecondaryText());
        footerLabel.setForeground(UITheme.metricSecondaryText());

        center.add(valueLabel);
        center.add(Box.createVerticalStrut(10));
        center.add(bodyLabel);
        center.add(Box.createVerticalStrut(16));
        center.add(footerLabel);

        configurePill(
            footerPill,
            UITheme.metricPillBorder(),
            UITheme.metricSecondaryText(),
            16,
            7
        );
        footerPill.setBackground(UITheme.metricPillFill(accent));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(footerPill, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCollectionsCard() {
        RoundedPanel card = new RoundedPanel(24, UITheme.PANEL_BG);
        card.setBorderColor(UITheme.BORDER);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Today's Collections", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 22f));
        }

        tableDescriptionLabel.setForeground(UITheme.TEXT_MUTED);
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(tableDescriptionLabel);

        header.add(left, BorderLayout.WEST);

        JScrollPane scroll = new JScrollPane(collectionTable);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 420));

        RoundedPanel tableShell = new RoundedPanel(18, UITheme.PANEL_BG);
        tableShell.setBorderColor(UITheme.BORDER);
        tableShell.setBorderWidth(1);
        tableShell.setLayout(new BorderLayout());
        tableShell.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tableShell.add(scroll, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(tableShell, BorderLayout.CENTER);
        return card;
    }

    private DefaultTableCellRenderer createCollectionRenderer() {
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
                label.setBackground(UITheme.PANEL_BG);
                label.setForeground(UITheme.TEXT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                Object firstCell = table.getValueAt(row, 0);
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No payments recorded");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }
                if (column == 0) {
                    label.setForeground(new Color(0x9C, 0xB8, 0xFF));
                }
                if (column == 2) {
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
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, UITheme.tableHeaderDivider()),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        };
    }

    private void configurePill(JLabel label, Color borderColor, Color textColor, int arc, int padding) {
        label.setForeground(textColor);
        label.setOpaque(false);
        label.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(arc, borderColor, false),
            BorderFactory.createEmptyBorder(5, padding, 5, padding)
        ));
    }

    public void refresh() {
        LocalDate today = LocalDate.now();
        double todayCollection = 0.0;
        int todayPaymentCount = 0;
        int activeCount = 0;
        int dueSoonCount = 0;
        int overdueCount = 0;
        double activePrincipal = 0.0;

        tableModel.setRowCount(0);

        if (payments != null) {
            for (Payment payment : payments) {
                if (payment == null || payment.getPaymentDate() == null || !today.equals(payment.getPaymentDate())) {
                    continue;
                }

                todayCollection += payment.getAmount();
                todayPaymentCount++;

                tableModel.addRow(new Object[]{
                    safe(payment.getLoanId()),
                    resolveBorrowerName(payment.getLoanId()),
                    money(payment.getAmount()),
                    HUMAN_DATE.format(payment.getPaymentDate()),
                    resolveCollectorName(payment.getLoanId())
                });
            }
        }

        if (loans != null) {
            for (Loan loan : loans) {
                if (loan == null) {
                    continue;
                }

                String status = normalizeStatus(loan.getStatus());
                LocalDate dueDate = resolveDueDate(loan);

                if ("ACTIVE".equals(status)) {
                    activeCount++;
                    activePrincipal += loan.getPrincipalAmount();
                }

                if (dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7)) && !"PAID".equals(status)) {
                    dueSoonCount++;
                }

                if ("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today) && !"PAID".equals(status))) {
                    overdueCount++;
                }
            }
        }

        totalCollectionValue.setText(money(todayCollection));
        totalCollectionBody.setText("Based on payments recorded for " + HUMAN_DATE.format(today) + ".");
        totalCollectionFooter.setText("Cash inflow today");
        totalCollectionPill.setText(todayPaymentCount + (todayPaymentCount == 1 ? " payment" : " payments"));

        activeLoansValue.setText(Integer.toString(activeCount));
        activeLoansBody.setText("Total principal of " + money(activePrincipal) + ".");
        activeLoansFooter.setText("Green - on-time");
        activeLoansPill.setText(money(activePrincipal));

        dueSoonValue.setText(Integer.toString(dueSoonCount));
        dueSoonBody.setText("Orange loans are due in the next 7 days.");
        dueSoonFooter.setText("Smart due date alert");
        dueSoonPill.setText(money(sumPrincipalForDueWindow(today, today.plusDays(7))));

        overdueValue.setText(Integer.toString(overdueCount));
        overdueBody.setText("Red loans are past due by at least 1 day.");
        overdueFooter.setText("Requires follow up");
        overduePill.setText(money(sumOverduePrincipal(today)));

        tableDescriptionLabel.setText("List of installment payments recorded for " + HUMAN_DATE.format(today) + ".");

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No payments recorded for today yet.",
                "",
                "",
                "",
                ""
            });
        }
    }

    private String resolveBorrowerName(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return "";
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                return safe(borrower.getFullName());
            }
        }
        return borrowerId;
    }

    private String resolveCollectorName(String borrowerId) {
        if (loans == null || borrowerId == null) {
            return "";
        }
        for (Loan loan : loans) {
            if (loan != null && borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                return safe(loan.getCollectorName());
            }
        }
        return "";
    }

    private LocalDate resolveDueDate(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        return loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
    }

    private double sumPrincipalForDueWindow(LocalDate from, LocalDate to) {
        double total = 0.0;
        if (loans == null) {
            return total;
        }
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            LocalDate dueDate = resolveDueDate(loan);
            String status = normalizeStatus(loan.getStatus());
            if (dueDate != null && !dueDate.isBefore(from) && !dueDate.isAfter(to) && !"PAID".equals(status)) {
                total += loan.getPrincipalAmount();
            }
        }
        return total;
    }

    private double sumOverduePrincipal(LocalDate today) {
        double total = 0.0;
        if (loans == null) {
            return total;
        }
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            LocalDate dueDate = resolveDueDate(loan);
            String status = normalizeStatus(loan.getStatus());
            if (("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today))) && !"PAID".equals(status)) {
                total += loan.getPrincipalAmount();
            }
        }
        return total;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String money(double amount) {
        return String.format("₱%,.2f", amount);
    }

    private String dotSymbol(Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return "<span style='color:" + hex + ";'>●</span>";
    }

    private String buildPillText(String dotHtml, String text) {
        return "<html>" + dotHtml + "&nbsp;" + safe(text) + "</html>";
    }
}
