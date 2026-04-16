package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class LoanPanel extends JPanel {
    public interface LoanSelectionListener {
        void onLoanSelected(Loan loan);
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
    private final Runnable saveAction;
    private final Runnable remindersAction;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton remindersButton = new JButton("Send Reminders");
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{
        "All statuses", "Active", "Pending", "Paid", "Overdue"
    });
    private final JComboBox<String> newBorrowerCombo = new JComboBox<>();
    private final JTextField newPrincipalField = new JTextField(10);
    private final JTextField newRateField = new JTextField(8);
    private final JTextField newTermField = new JTextField(6);
    private final JTextField newStartDateField = new JTextField(10);
    private final JTextField newCollectorField = new JTextField(10);
    private final JComboBox<String> newStatusCombo = new JComboBox<>(new String[]{"ACTIVE", "OVERDUE", "PAID", "PENDING"});
    private final JButton saveLoanButton = new JButton("Save Loan");
    private final JButton cancelLoanButton = new JButton("Cancel");
    private final JPanel inlineFormPanel = new JPanel(new BorderLayout(0, 12));
    private final JLabel inlineFormHelperLabel = new JLabel("", SwingConstants.LEFT);
    private Loan editingLoan;
    private LoanSelectionListener selectionListener;

    private final JLabel totalLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel activeLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalLoanValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel totalLoanBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel outstandingValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel outstandingBody = new JLabel("", SwingConstants.LEFT);
    public LoanPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            String username,
            Runnable saveAction,
            Runnable remindersAction
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;
        this.calculator = calculator;
        this.saveAction = saveAction == null ? () -> {} : saveAction;
        this.remindersAction = remindersAction == null ? () -> {} : remindersAction;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(new Object[]{
            "LOAN #", "BORROWER", "PRINCIPAL", "INT. RATE", "TOTAL PAYABLE",
            "MONTHLY DUE", "NEXT DUE", "TERMS", "STATUS", "BALANCE"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(46);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(UITheme.BORDER);
        table.setBackground(UITheme.PANEL_BG);
        table.setForeground(UITheme.TEXT);
        table.setFocusable(true);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setSelectionBackground(UITheme.CARD_2);
        table.setSelectionForeground(UITheme.TEXT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setDefaultRenderer(Object.class, createTableRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleSelectionChanged();
            }
        });

        JTableHeader header = table.getTableHeader();
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

        table.getColumnModel().getColumn(8).setCellRenderer(new StatusPillRenderer());
        configureTableColumns();
        UITheme.applyTextField(newPrincipalField);
        UITheme.applyTextField(newRateField);
        UITheme.applyTextField(newTermField);
        UITheme.applyTextField(newStartDateField);
        UITheme.applyTextField(newCollectorField);
        styleStatusFilterCombo();
        styleBorrowerCombo();
        styleStatusCombo();
        inlineFormPanel.setOpaque(false);
        inlineFormPanel.setVisible(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    public void setSelectionListener(LoanSelectionListener listener) {
        this.selectionListener = listener;
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Loans - Portfolio", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        JLabel subtitle = new JLabel("Create, monitor, and update loan records with smart due dates.", SwingConstants.LEFT);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(createMetricCard("TOTAL LOANS", totalLoansValue, totalLoansBody, BLUE));
        cards.add(createMetricCard("ACTIVE LOANS", activeLoansValue, activeLoansBody, GREEN));
        cards.add(createMetricCard("TOTAL LOAN VALUE", totalLoanValue, totalLoanBody, ORANGE));
        cards.add(createMetricCard("OUTSTANDING BALANCE", outstandingValue, outstandingBody, RED));

        body.add(cards, BorderLayout.NORTH);
        body.add(buildManagementCard(), BorderLayout.CENTER);
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
        Font titleFont = titleLabel.getFont();
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(Font.BOLD, 12f));
        }

        valueLabel.setForeground(UITheme.metricPrimaryText());
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 34f));
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

    private void configureTableColumns() {
        TableColumnModel columns = table.getColumnModel();
        setColumnWidth(columns, 0, 98, 108);
        setColumnWidth(columns, 1, 146, 180);
        setColumnWidth(columns, 2, 104, 118);
        setColumnWidth(columns, 3, 78, 88);
        setColumnWidth(columns, 4, 128, 152);
        setColumnWidth(columns, 5, 116, 132);
        setColumnWidth(columns, 6, 116, 130);
        setColumnWidth(columns, 7, 92, 100);
        setColumnWidth(columns, 8, 110, 126);
        setColumnWidth(columns, 9, 118, 140);
    }

    private void setColumnWidth(TableColumnModel columns, int index, int minWidth, int preferredWidth) {
        columns.getColumn(index).setMinWidth(minWidth);
        columns.getColumn(index).setPreferredWidth(preferredWidth);
    }

    private JPanel buildManagementCard() {
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

        JLabel title = new JLabel("Loan Management", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 22f));
        }

        left.add(title);

        JButton addBtn = new JButton("+ New loan");
        styleHeaderActionButton(addBtn, true);
        addBtn.addActionListener(e -> {
            clearInlineLoanForm();
            toggleInlineLoanForm(true);
        });

        JLabel filterLabel = new JLabel("Status", SwingConstants.LEFT);
        filterLabel.setForeground(UITheme.TEXT_MUTED);
        filterLabel.setFont(UITheme.captionFont());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);

        styleHeaderActionButton(editButton, false);
        editButton.addActionListener(e -> {
            String selectedLoanId = getSelectedLoanId();
            if (selectedLoanId.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a loan first.",
                    "Loans",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            showEditDialog(selectedLoanId);
        });

        styleHeaderActionButton(deleteButton, false);
        deleteButton.addActionListener(e -> {
            String selectedLoanId = getSelectedLoanId();
            if (selectedLoanId.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a loan first.",
                    "Loans",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            handleDelete(selectedLoanId);
        });

        styleHeaderActionButton(remindersButton, false);
        remindersButton.addActionListener(e -> remindersAction.run());

        right.add(filterLabel);
        right.add(statusFilterCombo);
        right.add(remindersButton);
        right.add(editButton);
        right.add(deleteButton);
        right.add(addBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 450));

        RoundedPanel tableShell = new RoundedPanel(18, UITheme.PANEL_BG);
        tableShell.setBorderColor(UITheme.BORDER);
        tableShell.setBorderWidth(1);
        tableShell.setLayout(new BorderLayout());
        tableShell.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tableShell.add(scroll, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.add(buildInlineLoanForm(), BorderLayout.NORTH);
        content.add(tableShell, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void styleHeaderActionButton(JButton button, boolean primary) {
        UITheme.applyHeaderActionButton(button, primary);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);

        int totalLoans = 0;
        int activeLoans = 0;
        int riskLoans = 0;
        double totalPrincipal = 0.0;
        double totalOutstanding = 0.0;

        if (loans != null) {
            for (Loan loan : loans) {
                if (loan == null) {
                    continue;
                }

                totalLoans++;
                totalPrincipal += loan.getPrincipalAmount();

                String status = normalizeStatus(loan.getStatus());
                if ("ACTIVE".equals(status)) {
                    activeLoans++;
                }
                if ("OVERDUE".equals(status)) {
                    riskLoans++;
                }

                double totalPayable = calculator.computeTotalPayable(loan);
                double installment = calculator.computeMonthlyInstallment(loan);
                double paid = paidAmountForLoan(safe(loan.getId()));
                double remaining = calculator.computeRemainingBalance(loan, paid);
                int termsLeft = remainingTerms(loan);
                totalOutstanding += remaining;

                if (!matchesSelectedStatusFilter(status)) {
                    continue;
                }

                tableModel.addRow(new Object[]{
                    safe(loan.getId()),
                    resolveBorrowerName(loan.getBorrowerId()),
                    money(loan.getPrincipalAmount()),
                    String.format("%.0f%%", loan.getInterestRateAnnual()),
                    money(totalPayable),
                    money(installment),
                    formatDate(nextDueDate(loan)),
                    Integer.toString(termsLeft),
                    prettyStatus(status),
                    money(remaining)
                });
            }
        }

        totalLoansValue.setText(Integer.toString(totalLoans));
        totalLoansBody.setText("All loans created in the system.");
        activeLoansValue.setText(Integer.toString(activeLoans));
        activeLoansBody.setText("Loans currently being paid.");
        totalLoanValue.setText(money(totalPrincipal));
        totalLoanBody.setText("Total principal issued.");
        outstandingValue.setText(money(totalOutstanding));
        outstandingBody.setText("Remaining balance across all loans.");

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No loan records yet.",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            });
        }

        table.clearSelection();
        handleSelectionChanged();
    }

    private String resolveBorrowerName(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return borrowerId == null ? "" : borrowerId;
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                String name = safe(borrower.getFullName());
                return name.isEmpty() ? borrowerId : name;
            }
        }
        return borrowerId;
    }

    private LocalDate nextDueDate(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        String status = normalizeStatus(loan.getStatus());
        if ("PAID".equals(status)) {
            return null;
        }
        int completedTerms = Math.max(0, loan.getOriginalTermMonths() - loan.getTermMonths());
        if (completedTerms >= loan.getOriginalTermMonths()) {
            return null;
        }
        return loan.getStartDate().plusMonths(completedTerms + 1L);
    }

    private int remainingTerms(Loan loan) {
        if (loan == null || loan.getOriginalTermMonths() <= 0) {
            return 0;
        }
        String status = normalizeStatus(loan.getStatus());
        if ("PAID".equals(status)) {
            return 0;
        }
        return Math.max(0, loan.getTermMonths());
    }

    private double paidAmountForLoan(String loanId) {
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

    private String formatDate(LocalDate date) {
        return date == null ? "" : HUMAN_DATE.format(date);
    }

    private String prettyStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "";
        }
        String lowered = status.toLowerCase();
        return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private boolean matchesSelectedStatusFilter(String status) {
        Object selectedItem = statusFilterCombo.getSelectedItem();
        if (selectedItem == null) {
            return true;
        }
        String filter = selectedItem.toString().trim().toUpperCase();
        if (filter.isEmpty() || "ALL STATUSES".equals(filter)) {
            return true;
        }
        return filter.equals(normalizeStatus(status));
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
                label.setBackground(isSelected ? UITheme.CARD_2 : UITheme.PANEL_BG);
                label.setForeground(UITheme.TEXT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                Object firstCell = table.getValueAt(row, 0);
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No loan records");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }
                label.setHorizontalAlignment((column == 2 || column == 4 || column == 5 || column == 7 || column == 9)
                    ? SwingConstants.RIGHT
                    : SwingConstants.LEFT);
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

    private JPanel buildInlineLoanForm() {
        RoundedPanel formShell = new RoundedPanel(18, UITheme.PANEL_BG);
        formShell.setBorderColor(UITheme.BORDER);
        formShell.setBorderWidth(1);
        formShell.setLayout(new BorderLayout(0, 12));
        formShell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        refreshBorrowerOptions();

        JPanel fields = new JPanel(new GridLayout(2, 4, 10, 10));
        fields.setOpaque(false);
        fields.add(createInlineField("Borrower", newBorrowerCombo));
        fields.add(createInlineField("Principal amount (₱)", newPrincipalField));
        fields.add(createInlineField("Interest rate (%)", newRateField));
        fields.add(createInlineField("Term (months)", newTermField));
        fields.add(createInlineField("Start date (yyyy-MM-dd)", newStartDateField));
        fields.add(createInlineField("Collector name", newCollectorField));
        fields.add(createInlineField("Status", newStatusCombo));

        inlineFormHelperLabel.setForeground(UITheme.TEXT_MUTED);
        updateInlineFormState();

        UITheme.applySecondaryButton(cancelLoanButton);
        cancelLoanButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        cancelLoanButton.addActionListener(e -> toggleInlineLoanForm(false));

        UITheme.applyPrimaryButton(saveLoanButton);
        saveLoanButton.setBorder(new RoundedButtonBorder(18, UITheme.ACCENT, false));
        saveLoanButton.addActionListener(e -> handleInlineLoanSave());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelLoanButton);
        actions.add(saveLoanButton);

        formShell.add(inlineFormHelperLabel, BorderLayout.NORTH);
        formShell.add(fields, BorderLayout.CENTER);
        formShell.add(actions, BorderLayout.SOUTH);

        inlineFormPanel.removeAll();
        inlineFormPanel.add(formShell, BorderLayout.CENTER);
        return inlineFormPanel;
    }

    private void styleBorrowerCombo() {
        newBorrowerCombo.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(UITheme.CARD_2);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("v");
                button.setBackground(UITheme.CARD_2);
                button.setForeground(UITheme.TEXT_MUTED);
                button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.BORDER));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                return button;
            }
        });
        newBorrowerCombo.setBackground(UITheme.CARD_2);
        newBorrowerCombo.setForeground(UITheme.TEXT);
        newBorrowerCombo.setFocusable(false);
        newBorrowerCombo.setOpaque(false);
        newBorrowerCombo.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        newBorrowerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                );
                boolean inDropdown = index >= 0;
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setBackground(isSelected
                    ? (inDropdown ? UITheme.ACCENT : UITheme.CARD_2)
                    : UITheme.CARD_2);
                label.setForeground(isSelected && inDropdown ? Color.WHITE : UITheme.TEXT);
                if (value == null || value.toString().trim().isEmpty()) {
                    label.setText("Select borrower...");
                    label.setForeground(UITheme.TEXT_MUTED);
                }
                return label;
            }
        });
    }

    private void styleStatusFilterCombo() {
        statusFilterCombo.setPreferredSize(new Dimension(132, 32));
        statusFilterCombo.setMaximumSize(new Dimension(132, 32));
        statusFilterCombo.setMinimumSize(new Dimension(132, 32));
        statusFilterCombo.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(UITheme.CARD_2);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("v");
                button.setBackground(UITheme.CARD_2);
                button.setForeground(UITheme.TEXT_MUTED);
                button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.BORDER));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                return button;
            }
        });
        statusFilterCombo.setBackground(UITheme.CARD_2);
        statusFilterCombo.setForeground(UITheme.TEXT);
        statusFilterCombo.setFocusable(false);
        statusFilterCombo.setOpaque(false);
        statusFilterCombo.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        statusFilterCombo.setFont(UITheme.captionFont().deriveFont(12.5f));
        statusFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                );
                boolean inDropdown = index >= 0;
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(6, 10, 6, 10));
                label.setBackground(isSelected
                    ? (inDropdown ? UITheme.ACCENT : UITheme.CARD_2)
                    : UITheme.CARD_2);
                label.setForeground(isSelected && inDropdown ? Color.WHITE : UITheme.TEXT);
                return label;
            }
        });
        statusFilterCombo.addActionListener(e -> refreshTable());
    }

    private void styleStatusCombo() {
        newStatusCombo.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(UITheme.CARD_2);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("v");
                button.setBackground(UITheme.CARD_2);
                button.setForeground(UITheme.TEXT_MUTED);
                button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UITheme.BORDER));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                return button;
            }
        });
        newStatusCombo.setBackground(UITheme.CARD_2);
        newStatusCombo.setForeground(UITheme.TEXT);
        newStatusCombo.setFocusable(false);
        newStatusCombo.setOpaque(false);
        newStatusCombo.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        newStatusCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                );
                boolean inDropdown = index >= 0;
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setBackground(isSelected
                    ? (inDropdown ? UITheme.ACCENT : UITheme.CARD_2)
                    : UITheme.CARD_2);
                label.setForeground(isSelected && inDropdown ? Color.WHITE : UITheme.TEXT);
                return label;
            }
        });
    }
    private JPanel createInlineField(String labelText, Component input) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText, SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT_MUTED);

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(input);
        return wrapper;
    }

    private void handleSelectionChanged() {
        Loan selected = getSelectedLoan();
        if (selectionListener != null) {
            selectionListener.onLoanSelected(selected);
        }
    }

    private Loan getSelectedLoan() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }

        Object idValue = table.getValueAt(selectedRow, 0);
        if (idValue == null) {
            return null;
        }

        String loanId = idValue.toString().trim();
        if (loanId.isEmpty()) {
            return null;
        }
        return findLoanById(loanId);
    }

    private String getSelectedLoanId() {
        Loan selected = getSelectedLoan();
        return selected == null ? "" : safe(selected.getId());
    }

    private void toggleInlineLoanForm(boolean visible) {
        refreshBorrowerOptions();
        inlineFormPanel.setVisible(visible);
        if (!visible) {
            clearInlineLoanForm();
        } else {
            updateInlineFormState();
            newPrincipalField.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void clearInlineLoanForm() {
        editingLoan = null;
        if (newBorrowerCombo.getItemCount() > 0) {
            newBorrowerCombo.setSelectedIndex(0);
        }
        newPrincipalField.setText("");
        newRateField.setText("");
        newTermField.setText("");
        newStartDateField.setText(LocalDate.now().toString());
        newCollectorField.setText("");
        newStatusCombo.setSelectedItem("ACTIVE");
        updateInlineFormState();
    }

    private void updateInlineFormState() {
        boolean editing = editingLoan != null;
        inlineFormHelperLabel.setText(editing
            ? "Update the selected loan below, then click save to apply your changes."
            : "Create a loan inline without opening a popup. Fill in the details then save.");
        saveLoanButton.setText(editing ? "Save Changes" : "Save Loan");
    }

    private void startInlineEdit(Loan loan) {
        if (loan == null) {
            return;
        }
        editingLoan = loan;
        refreshBorrowerOptions();
        newBorrowerCombo.setSelectedItem(safe(loan.getBorrowerId()));
        newPrincipalField.setText(Double.toString(loan.getPrincipalAmount()));
        newRateField.setText(Double.toString(loan.getInterestRateAnnual()));
        newTermField.setText(Integer.toString(loan.getTermMonths()));
        newStartDateField.setText(loan.getStartDate() == null ? "" : loan.getStartDate().toString());
        newCollectorField.setText(safe(loan.getCollectorName()));
        newStatusCombo.setSelectedItem(normalizeStatus(loan.getStatus()).isEmpty() ? "ACTIVE" : normalizeStatus(loan.getStatus()));
        updateInlineFormState();
        toggleInlineLoanForm(true);
    }

    private void refreshBorrowerOptions() {
        String currentSelection = newBorrowerCombo.getSelectedItem() == null
            ? ""
            : newBorrowerCombo.getSelectedItem().toString();
        newBorrowerCombo.removeAllItems();
        newBorrowerCombo.addItem("");
        if (borrowers != null) {
            for (Borrower borrower : borrowers) {
                if (borrower == null) {
                    continue;
                }
                String borrowerId = safe(borrower.getId());
                if (!borrowerId.isEmpty()) {
                    newBorrowerCombo.addItem(borrowerId);
                }
            }
        }
        if (!currentSelection.isEmpty()) {
            newBorrowerCombo.setSelectedItem(currentSelection);
        }
    }

    private void handleInlineLoanSave() {
        try {
            String borrowerId = newBorrowerCombo.getSelectedItem() == null
                ? ""
                : newBorrowerCombo.getSelectedItem().toString().trim();
            double principal = Double.parseDouble(newPrincipalField.getText().trim());
            double rate = Double.parseDouble(newRateField.getText().trim());
            int term = Integer.parseInt(newTermField.getText().trim());
            LocalDate startDate = LocalDate.parse(newStartDateField.getText().trim());
            String collectorName = newCollectorField.getText().trim();
            String status = newStatusCombo.getSelectedItem() == null ? "ACTIVE" : newStatusCombo.getSelectedItem().toString().trim();

            if (borrowerId.isEmpty()) {
                throw new IllegalArgumentException("borrower");
            }

            if (editingLoan != null) {
                editingLoan.setBorrowerId(borrowerId);
                editingLoan.setPrincipalAmount(principal);
                editingLoan.setInterestRateAnnual(rate);
                editingLoan.setTermMonths(term);
                editingLoan.setStartDate(startDate);
                editingLoan.setCollectorName(collectorName);
                editingLoan.setStatus(status);
            } else {
                loans.add(new Loan(generateLoanId(), borrowerId, principal, rate, term, startDate, collectorName, status));
            }
            saveAction.run();
            toggleInlineLoanForm(false);
            refreshTable();
        } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a borrower, valid numbers for principal/rate/term, and a valid date (yyyy-MM-dd).",
                "Loan",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private String generateLoanId() {
        int nextNumber = loans == null ? 1 : loans.size() + 1001;
        String candidate = String.format("LN-%07d", nextNumber);
        while (findLoanById(candidate) != null) {
            nextNumber++;
            candidate = String.format("LN-%07d", nextNumber);
        }
        return candidate;
    }

    private void showEditDialog(String loanId) {
        Loan existing = findLoanById(loanId);
        if (existing == null) {
            return;
        }
        startInlineEdit(existing);
    }

    private void handleDelete(String loanId) {
        Loan existing = findLoanById(loanId);
        if (existing == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete loan \"" + safe(existing.getId()) + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (payments != null) {
            payments.removeIf(payment -> payment != null && loanId.equalsIgnoreCase(safe(payment.getLoanId())));
        }
        loans.remove(existing);
        saveAction.run();
        refreshTable();
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

    private void configurePill(JLabel label, Color borderColor, Color textColor, int arc, int padding) {
        label.setForeground(textColor);
        label.setOpaque(true);
        label.setBackground(UITheme.PANEL_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(arc, borderColor, false),
            BorderFactory.createEmptyBorder(5, padding, 5, padding)
        ));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String money(double amount) {
        return UITheme.formatCurrency(amount);
    }

    private String dotSymbol(Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return "<span style='color:" + hex + ";'>&#9679;</span>";
    }

    private String buildPillText(String dotHtml, String text) {
        return "<html>" + dotHtml + "&nbsp;" + safe(text) + "</html>";
    }

    private class StatusPillRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            String status = value == null ? "" : value.toString().toUpperCase();
            Color accent = switch (status) {
                case "ACTIVE" -> GREEN;
                case "OVERDUE" -> RED;
                case "PAID" -> BLUE;
                case "PENDING" -> ORANGE;
                default -> UITheme.BORDER;
            };

            boolean filled = "ACTIVE".equals(status)
                || "OVERDUE".equals(status)
                || "PAID".equals(status)
                || "PENDING".equals(status);
            Color textColor = filled ? Color.WHITE : UITheme.TEXT;
            JLabel label = new JLabel(prettyStatus(status), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics g2 = g.create();
                    try {
                        g2.setColor(isSelected ? UITheme.CARD_2 : UITheme.PANEL_BG);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        if (filled) {
                            g2.setColor(accent);
                            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 32, 32);
                        }
                    } finally {
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            label.setOpaque(false);
            label.setText(prettyStatus(status));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setForeground(textColor);
            Font baseFont = label.getFont();
            if (baseFont != null) {
                label.setFont(baseFont.deriveFont(Font.BOLD, 13f));
            }
            label.setBorder(BorderFactory.createCompoundBorder(
                new RoundedButtonBorder(16, accent, false),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            return label;
        }
    }

}


