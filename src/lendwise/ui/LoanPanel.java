package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JComboBox<String> newBorrowerCombo = new JComboBox<>();
    private final JTextField newPrincipalField = new JTextField(10);
    private final JTextField newRateField = new JTextField(8);
    private final JTextField newTermField = new JTextField(6);
    private final JTextField newStartDateField = new JTextField(10);
    private final JTextField newCollectorField = new JTextField(10);
    private final JButton saveLoanButton = new JButton("Save Loan");
    private final JButton cancelLoanButton = new JButton("Cancel");
    private final JPanel inlineFormPanel = new JPanel(new BorderLayout(0, 12));
    private LoanSelectionListener selectionListener;

    private final JLabel totalLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel activeLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalLoanValue = new JLabel("₱0.00", SwingConstants.LEFT);
    private final JLabel totalLoanBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel outstandingValue = new JLabel("₱0.00", SwingConstants.LEFT);
    private final JLabel outstandingBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel healthPill = new JLabel("", SwingConstants.CENTER);
    public LoanPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            String username,
            Runnable saveAction
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;
        this.calculator = calculator;
        this.saveAction = saveAction == null ? () -> {} : saveAction;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(new Object[]{
            "LOAN #", "BORROWER", "PRINCIPAL", "INTEREST", "TOTAL PAYABLE",
            "INSTALLMENT", "NEXT DUE", "STATUS", "REMAINING BALANCE"
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

        table.getColumnModel().getColumn(7).setCellRenderer(new StatusPillRenderer());
        UITheme.applyTextField(newPrincipalField);
        UITheme.applyTextField(newRateField);
        UITheme.applyTextField(newTermField);
        UITheme.applyTextField(newStartDateField);
        UITheme.applyTextField(newCollectorField);
        styleBorrowerCombo();
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

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        configurePill(healthPill, UITheme.BORDER, UITheme.TEXT_MUTED, 18, 8);

        right.add(healthPill);

        wrapper.add(left, BorderLayout.WEST);
        wrapper.add(right, BorderLayout.EAST);
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

        JLabel subtitle = new JLabel("Record new loans and let LendWise calculate interest, installment amount, and smart due dates.", SwingConstants.LEFT);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JButton addBtn = new JButton("+ New loan");
        UITheme.applySecondaryButton(addBtn);
        addBtn.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        addBtn.addActionListener(e -> toggleInlineLoanForm(true));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);

        UITheme.applySecondaryButton(editButton);
        editButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
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

        UITheme.applySecondaryButton(deleteButton);
        deleteButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
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

        right.add(editButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(deleteButton);
        right.add(Box.createHorizontalStrut(8));
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
                double paid = paidAmountForBorrower(safe(loan.getBorrowerId()));
                double remaining = calculator.computeRemainingBalance(loan, paid);
                totalOutstanding += remaining;

                tableModel.addRow(new Object[]{
                    safe(loan.getId()),
                    resolveBorrowerName(loan.getBorrowerId()),
                    money(loan.getPrincipalAmount()),
                    String.format("%.0f%%", loan.getInterestRateAnnual()),
                    money(totalPayable),
                    money(installment),
                    formatDate(nextDueDate(loan)),
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

        if (riskLoans > 0) {
            healthPill.setText(buildPillText(dotSymbol(RED), riskLoans + " risk loan" + (riskLoans == 1 ? "" : "s")));
        } else {
            healthPill.setText(buildPillText(dotSymbol(GREEN), "All systems normal - No risk loans"));
        }

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No loan records yet.",
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
        return loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
    }

    private double paidAmountForBorrower(String borrowerId) {
        double total = 0.0;
        if (payments == null) {
            return total;
        }
        for (Payment payment : payments) {
            if (payment != null && borrowerId.equalsIgnoreCase(safe(payment.getLoanId()))) {
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
                label.setHorizontalAlignment((column == 2 || column == 4 || column == 5 || column == 8)
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

        JPanel fields = new JPanel(new GridLayout(2, 3, 10, 10));
        fields.setOpaque(false);
        fields.add(createInlineField("Borrower", newBorrowerCombo));
        fields.add(createInlineField("Principal amount (₱)", newPrincipalField));
        fields.add(createInlineField("Interest rate (%)", newRateField));
        fields.add(createInlineField("Term (months)", newTermField));
        fields.add(createInlineField("Start date (yyyy-MM-dd)", newStartDateField));
        fields.add(createInlineField("Collector name", newCollectorField));

        JLabel helper = new JLabel(
            "Create a loan inline without opening a popup. Fill in the details then save.",
            SwingConstants.LEFT
        );
        helper.setForeground(UITheme.TEXT_MUTED);

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

        formShell.add(helper, BorderLayout.NORTH);
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
            newPrincipalField.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void clearInlineLoanForm() {
        if (newBorrowerCombo.getItemCount() > 0) {
            newBorrowerCombo.setSelectedIndex(0);
        }
        newPrincipalField.setText("");
        newRateField.setText("");
        newTermField.setText("");
        newStartDateField.setText(LocalDate.now().toString());
        newCollectorField.setText("");
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

            if (borrowerId.isEmpty()) {
                throw new IllegalArgumentException("borrower");
            }

            loans.add(new Loan(generateLoanId(), borrowerId, principal, rate, term, startDate, collectorName, "ACTIVE"));
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

        LoanForm form = new LoanForm();
        form.fromLoan(existing);

        int result = form.showDialog(this, "Edit Loan");
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Loan updated = form.toLoan();
        if (updated == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter valid numeric values for principal, rate, term and valid date (yyyy-MM-dd).",
                "Loan",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        existing.setId(updated.getId());
        existing.setBorrowerId(updated.getBorrowerId());
        existing.setPrincipalAmount(updated.getPrincipalAmount());
        existing.setInterestRateAnnual(updated.getInterestRateAnnual());
        existing.setTermMonths(updated.getTermMonths());
        existing.setStartDate(updated.getStartDate());
        existing.setCollectorName(updated.getCollectorName());
        existing.setStatus(updated.getStatus());
        saveAction.run();
        refreshTable();
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
        return String.format("₱%,.2f", amount);
    }

    private String dotSymbol(Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return "<span style='color:" + hex + ";'>●</span>";
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

    private static class LoanForm {
        private final JTextField idField = new JTextField(12);
        private final JTextField borrowerIdField = new JTextField(12);
        private final JTextField principalField = new JTextField(10);
        private final JTextField rateField = new JTextField(8);
        private final JTextField termField = new JTextField(6);
        private final JTextField startDateField = new JTextField(10);
        private final JTextField collectorField = new JTextField(12);
        private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "OVERDUE", "PAID", "PENDING"});

        int showDialog(JPanel parent, String title) {
            java.awt.Window owner = SwingUtilities.getWindowAncestor(parent);
            JDialog dialog = new JDialog(owner);
            dialog.setTitle(title);
            dialog.setModal(true);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            panel.add(new JLabel("Loan ID:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Borrower ID:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(borrowerIdField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            panel.add(new JLabel("Principal:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(principalField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            panel.add(new JLabel("Rate (Annual %):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(rateField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0;
            panel.add(new JLabel("Term (Months):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(termField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.weightx = 0;
            panel.add(new JLabel("Start Date (yyyy-MM-dd):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(startDateField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.weightx = 0;
            panel.add(new JLabel("Collector Name:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(collectorField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.weightx = 0;
            panel.add(new JLabel("Status:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(statusCombo, gbc);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            buttons.add(ok);
            buttons.add(cancel);

            ok.addActionListener(e -> {
                dialog.setVisible(false);
                dialog.dispose();
            });
            cancel.addActionListener(e -> {
                idField.putClientProperty("cancelled", Boolean.TRUE);
                dialog.setVisible(false);
                dialog.dispose();
            });

            JPanel root = new JPanel(new BorderLayout(8, 8));
            root.add(panel, BorderLayout.CENTER);
            root.add(buttons, BorderLayout.SOUTH);

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setLocationRelativeTo(owner != null ? owner : parent);
            dialog.setVisible(true);

            Boolean cancelled = (Boolean) idField.getClientProperty("cancelled");
            idField.putClientProperty("cancelled", null);
            return Boolean.TRUE.equals(cancelled) ? JOptionPane.CANCEL_OPTION : JOptionPane.OK_OPTION;
        }

        void fromLoan(Loan loan) {
            if (loan == null) {
                return;
            }
            idField.setText(loan.getId());
            borrowerIdField.setText(loan.getBorrowerId());
            principalField.setText(Double.toString(loan.getPrincipalAmount()));
            rateField.setText(Double.toString(loan.getInterestRateAnnual()));
            termField.setText(Integer.toString(loan.getTermMonths()));
            startDateField.setText(loan.getStartDate() == null ? "" : loan.getStartDate().toString());
            collectorField.setText(loan.getCollectorName() == null ? "" : loan.getCollectorName());
            statusCombo.setSelectedItem(loan.getStatus() == null ? "ACTIVE" : loan.getStatus().toUpperCase());
        }

        Loan toLoan() {
            try {
                String id = idField.getText().trim();
                String borrowerId = borrowerIdField.getText().trim();
                double principal = Double.parseDouble(principalField.getText().trim());
                double rate = Double.parseDouble(rateField.getText().trim());
                int term = Integer.parseInt(termField.getText().trim());
                LocalDate start = null;
                String collectorName = collectorField.getText().trim();
                String startText = startDateField.getText().trim();
                if (!startText.isEmpty()) {
                    start = LocalDate.parse(startText);
                }
                String status = statusCombo.getSelectedItem() == null ? "ACTIVE" : statusCombo.getSelectedItem().toString();
                return new Loan(id, borrowerId, principal, rate, term, start, collectorName, status);
            } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
                return null;
            }
        }
    }
}
