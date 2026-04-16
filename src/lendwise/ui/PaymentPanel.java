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
import java.time.YearMonth;
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
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class PaymentPanel extends JPanel {
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final ArrayList<Payment> payments;
    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final LoanCalculator calculator;
    private final Runnable saveAction;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JComboBox<String> newLoanCombo = new JComboBox<>();
    private final JTextField newAmountField = new JTextField(10);
    private final JTextField newDateField = new JTextField(10);
    private final JButton savePaymentButton = new JButton("Save Payment");
    private final JButton cancelPaymentButton = new JButton("Cancel");
    private final JPanel inlineFormPanel = new JPanel(new BorderLayout(0, 12));
    private final JLabel paymentPreviewLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel collectorHintLabel = new JLabel("", SwingConstants.RIGHT);
    private String lastSuggestedLoanId = "";
    private Payment editingPayment;

    private final JLabel totalPaymentsValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalPaymentsBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel todayCollectionValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel todayCollectionBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel monthlyCollectionValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel monthlyCollectionBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel profitValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel profitBody = new JLabel("", SwingConstants.LEFT);

    public PaymentPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Payment> payments,
            ArrayList<Loan> loans,
            LoanCalculator calculator,
            Runnable saveAction
    ) {
        this.payments = payments;
        this.borrowers = borrowers;
        this.loans = loans;
        this.calculator = calculator;
        this.saveAction = saveAction == null ? () -> {} : saveAction;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(
            new Object[]{"PAYMENT ID", "LOAN #", "BORROWER", "AMOUNT", "DATE", "METHOD"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(44);
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

        UITheme.applyTextField(newAmountField);
        UITheme.applyTextField(newDateField);
        styleLoanCombo();
        inlineFormPanel.setOpaque(false);
        inlineFormPanel.setVisible(false);
        paymentPreviewLabel.setForeground(UITheme.TEXT_MUTED);
        collectorHintLabel.setForeground(UITheme.TEXT_MUTED);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Payments - Monitoring", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        left.add(title);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(createMetricCard("TOTAL PAYMENTS", totalPaymentsValue, totalPaymentsBody, BLUE));
        cards.add(createMetricCard("TODAY'S COLLECTION", todayCollectionValue, todayCollectionBody, GREEN));
        cards.add(createMetricCard("MONTHLY COLLECTION", monthlyCollectionValue, monthlyCollectionBody, ORANGE));
        cards.add(createMetricCard("TOTAL PROFIT EARNED", profitValue, profitBody, RED));

        body.add(cards, BorderLayout.NORTH);
        body.add(buildMonitoringCard(), BorderLayout.CENTER);
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

    private JPanel buildMonitoringCard() {
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

        JLabel title = new JLabel("Payment Monitoring", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 22f));
        }

        left.add(title);

        JButton addButton = new JButton("+ Record payment");
        styleHeaderActionButton(addButton, true);
        addButton.addActionListener(e -> toggleInlinePaymentForm(true));

        styleHeaderActionButton(editButton, false);
        editButton.addActionListener(e -> {
            int selectedIndex = getSelectedPaymentIndex();
            if (selectedIndex < 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a payment first.",
                    "Payments",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            showEditDialog(selectedIndex);
        });

        styleHeaderActionButton(deleteButton, false);
        deleteButton.addActionListener(e -> {
            if (getSelectedPaymentIndex() < 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a payment first.",
                    "Payments",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            handleDelete();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(editButton);
        right.add(deleteButton);
        right.add(addButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 500));

        RoundedPanel tableShell = new RoundedPanel(18, UITheme.PANEL_BG);
        tableShell.setBorderColor(UITheme.BORDER);
        tableShell.setBorderWidth(1);
        tableShell.setLayout(new BorderLayout());
        tableShell.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tableShell.add(scroll, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.add(buildInlinePaymentForm(), BorderLayout.NORTH);
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

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        int totalPayments = 0;
        double todayCollection = 0.0;
        double monthlyCollection = 0.0;
        double totalProfit = 0.0;

        if (payments != null) {
            for (Payment payment : payments) {
                if (payment == null) {
                    continue;
                }

                totalPayments++;
                LocalDate paymentDate = payment.getPaymentDate();
                if (paymentDate != null && paymentDate.equals(today)) {
                    todayCollection += payment.getAmount();
                }
                if (paymentDate != null && YearMonth.from(paymentDate).equals(currentMonth)) {
                    monthlyCollection += payment.getAmount();
                }

                Loan loan = findLoanById(payment.getLoanId());
                totalProfit += estimateProfitPortion(payment, loan);

                tableModel.addRow(new Object[]{
                    safe(payment.getId()),
                    resolveLoanNumber(payment.getLoanId()),
                    resolveBorrowerNameForLoan(payment.getLoanId()),
                    money(payment.getAmount()),
                    paymentDate == null ? "" : HUMAN_DATE.format(paymentDate),
                    safe(payment.getMethod())
                });
            }
        }

        totalPaymentsValue.setText(Integer.toString(totalPayments));
        totalPaymentsBody.setText("All payment records logged.");
        todayCollectionValue.setText(money(todayCollection));
        todayCollectionBody.setText("Sum of payments recorded today.");
        monthlyCollectionValue.setText(money(monthlyCollection));
        monthlyCollectionBody.setText("Sum of payments recorded this month.");
        profitValue.setText(money(totalProfit));
        profitBody.setText("Estimated interest collected so far.");

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No payment records yet.",
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
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No payment records");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }
                label.setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
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

    private void handleSelectionChanged() {
        table.repaint();
    }

    private JPanel buildInlinePaymentForm() {
        RoundedPanel formShell = new RoundedPanel(18, UITheme.PANEL_BG);
        formShell.setBorderColor(UITheme.BORDER);
        formShell.setBorderWidth(1);
        formShell.setLayout(new BorderLayout(0, 12));
        formShell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        refreshLoanOptions();

        JPanel fields = new JPanel(new GridLayout(1, 3, 10, 8));
        fields.setOpaque(false);
        fields.add(createInlineField("Loan", newLoanCombo));
        fields.add(createInlineField("Amount (\u20B1)", newAmountField));
        fields.add(createInlineField("Payment date", newDateField));

        JPanel hints = new JPanel(new BorderLayout());
        hints.setOpaque(false);
        hints.add(paymentPreviewLabel, BorderLayout.WEST);
        hints.add(collectorHintLabel, BorderLayout.EAST);

        UITheme.applySecondaryButton(cancelPaymentButton);
        cancelPaymentButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        cancelPaymentButton.addActionListener(e -> toggleInlinePaymentForm(false));

        UITheme.applyPrimaryButton(savePaymentButton);
        savePaymentButton.setBorder(new RoundedButtonBorder(18, UITheme.ACCENT, false));
        savePaymentButton.addActionListener(e -> handleInlinePaymentSave());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelPaymentButton);
        actions.add(savePaymentButton);

        formShell.add(fields, BorderLayout.NORTH);
        formShell.add(hints, BorderLayout.CENTER);
        formShell.add(actions, BorderLayout.SOUTH);

        inlineFormPanel.removeAll();
        inlineFormPanel.add(formShell, BorderLayout.CENTER);
        return inlineFormPanel;
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

    private void styleLoanCombo() {
        newLoanCombo.setUI(new BasicComboBoxUI() {
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
        newLoanCombo.setBackground(UITheme.CARD_2);
        newLoanCombo.setForeground(UITheme.TEXT);
        newLoanCombo.setFocusable(false);
        newLoanCombo.setOpaque(false);
        newLoanCombo.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        newLoanCombo.setRenderer(new DefaultListCellRenderer() {
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
                    label.setText("Select active loan...");
                    label.setForeground(UITheme.TEXT_MUTED);
                }
                return label;
            }
        });
        newLoanCombo.addActionListener(e -> updatePaymentPreview());
    }

    private void toggleInlinePaymentForm(boolean visible) {
        refreshLoanOptions();
        inlineFormPanel.setVisible(visible);
        if (!visible) {
            clearInlinePaymentForm();
        } else {
            newAmountField.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void clearInlinePaymentForm() {
        editingPayment = null;
        if (newLoanCombo.getItemCount() > 0) {
            newLoanCombo.setSelectedIndex(0);
        }
        newAmountField.setText("");
        lastSuggestedLoanId = "";
        newDateField.setText(LocalDate.now().toString());
        savePaymentButton.setText("Save Payment");
        updatePaymentPreview();
    }

    private void refreshLoanOptions() {
        Loan currentLoan = resolveSelectedLoan();
        String currentLoanId = currentLoan == null ? "" : safe(currentLoan.getId());
        newLoanCombo.removeAllItems();
        newLoanCombo.addItem("");
        if (loans != null) {
            for (Loan loan : loans) {
                if (loan == null) {
                    continue;
                }
                String loanId = safe(loan.getId());
                String status = safe(loan.getStatus()).toUpperCase();
                if (!loanId.isEmpty() && ("ACTIVE".equals(status) || "OVERDUE".equals(status) || "PENDING".equals(status))) {
                    newLoanCombo.addItem(buildLoanOptionLabel(loan));
                }
            }
        }
        if (!currentLoanId.isEmpty()) {
            selectLoanOption(currentLoanId);
        }
        if (newDateField.getText().trim().isEmpty()) {
            newDateField.setText(LocalDate.now().toString());
        }
        updatePaymentPreview();
    }

    private void updatePaymentPreview() {
        Loan selectedLoan = resolveSelectedLoan();
        if (selectedLoan == null) {
            paymentPreviewLabel.setText("Remaining balance after payment: " + money(0.0));
            collectorHintLabel.setText("Collector: -");
            lastSuggestedLoanId = "";
            return;
        }
        double paid = totalPaidForLoan(safe(selectedLoan.getId()));
        double remaining = calculator.computeRemainingBalance(selectedLoan, paid);
        double dueAmount = suggestedDueAmount(selectedLoan, remaining);
        String selectedLoanId = safe(selectedLoan.getId());
        if (newAmountField.getText().trim().isEmpty() || !selectedLoanId.equals(lastSuggestedLoanId)) {
            newAmountField.setText(formatEditableAmount(dueAmount));
        }
        lastSuggestedLoanId = selectedLoanId;
        paymentPreviewLabel.setText("Remaining balance after payment: " + money(remaining));
        String collectorName = safe(selectedLoan.getCollectorName());
        collectorHintLabel.setText("Due: " + money(dueAmount) + "  |  Collector: " + (collectorName.isEmpty() ? "-" : collectorName));
    }

    private double suggestedDueAmount(Loan loan, double remaining) {
        if (loan == null || remaining <= 0.0) {
            return 0.0;
        }
        double paid = totalPaidForLoan(safe(loan.getId()));
        double dueAmount = calculator.computeCurrentInstallmentDue(loan, paid);
        return dueAmount <= 0.0 ? remaining : Math.min(dueAmount, remaining);
    }

    private void handleInlinePaymentSave() {
        try {
            Loan selectedLoan = resolveSelectedLoan();
            if (selectedLoan == null) {
                throw new IllegalArgumentException("loan");
            }

            double amount = Double.parseDouble(newAmountField.getText().trim());
            LocalDate date = LocalDate.parse(newDateField.getText().trim());
            String loanId = safe(selectedLoan.getId());
            String previousLoanId = editingPayment == null ? "" : safe(editingPayment.getLoanId());
            validatePaymentAmount(selectedLoan, amount, date);

            if (editingPayment != null) {
                editingPayment.setLoanId(loanId);
                editingPayment.setAmount(amount);
                editingPayment.setPaymentDate(date);
                editingPayment.setMethod("CASH");
            } else {
                payments.add(new Payment(generatePaymentId(), loanId, amount, date, "CASH"));
            }

            if (!previousLoanId.isEmpty() && !previousLoanId.equalsIgnoreCase(loanId)) {
                updateLoanAfterPayment(previousLoanId);
            }
            updateLoanAfterPayment(loanId);
            saveAction.run();
            toggleInlinePaymentForm(false);
            refreshTable();
        } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a loan and enter a valid amount and date (yyyy-MM-dd).",
                "Payment",
                JOptionPane.WARNING_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            String message = safe(ex.getMessage()).trim();
            if (message.isEmpty() || "loan".equalsIgnoreCase(message) || "amount".equalsIgnoreCase(message) || "paid".equalsIgnoreCase(message)) {
                message = "Please select a loan and enter a valid payment amount and date.";
            }
            JOptionPane.showMessageDialog(
                this,
                message,
                "Payment",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void showEditDialog(int selectedIndex) {
        Payment existing = findPaymentByIndex(selectedIndex);
        if (existing == null) {
            return;
        }
        startInlineEdit(existing);
    }

    private void handleDelete() {
        int selectedIndex = getSelectedPaymentIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a payment to delete.",
                "Payments",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Payment existing = findPaymentByIndex(selectedIndex);
        if (existing == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete payment \"" + safe(existing.getId()) + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String loanId = existing.getLoanId();
        payments.remove(existing);
        updateLoanAfterPayment(loanId);
        saveAction.run();
        refreshTable();
    }

    private int getSelectedPaymentIndex() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || tableModel.getRowCount() == 0) {
            return -1;
        }
        Object paymentId = table.getValueAt(selectedRow, 0);
        if (paymentId == null || paymentId.toString().trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (payment != null && safe(payment.getId()).equals(paymentId.toString().trim())) {
                return i;
            }
        }
        return -1;
    }

    private Payment findPaymentByIndex(int index) {
        if (index < 0 || index >= payments.size()) {
            return null;
        }
        return payments.get(index);
    }

    private Loan resolveSelectedLoan() {
        String selected = newLoanCombo.getSelectedItem() == null ? "" : newLoanCombo.getSelectedItem().toString().trim();
        String loanId = extractLoanId(selected);
        if (loanId.isEmpty() || loans == null) {
            return null;
        }
        for (Loan loan : loans) {
            if (loan != null && loanId.equalsIgnoreCase(safe(loan.getId()))) {
                return loan;
            }
        }
        return null;
    }

    private void updateLoanAfterPayment(String loanId) {
        if (loanId == null || loanId.trim().isEmpty()) {
            return;
        }

        Loan loan = findLoanById(loanId);
        if (loan == null) {
            return;
        }

        double totalPaid = 0.0;
        for (Payment payment : payments) {
            if (payment != null && loanId.equalsIgnoreCase(safe(payment.getLoanId()))) {
                totalPaid += payment.getAmount();
            }
        }

        double remaining = calculator.computeRemainingBalance(loan, totalPaid);
        int originalTerm = loan.getOriginalTermMonths();
        int remainingTerm = calculator.computeRemainingTermMonths(loan, totalPaid);
        loan.setTermMonths(remainingTerm);

        if (remaining <= 0.0) {
            loan.setStatus("PAID");
        } else {
            LocalDate start = loan.getStartDate();
            LocalDate due = null;
            if (start != null && originalTerm > 0) {
                due = start.plusMonths(originalTerm);
            }
            if (due != null && due.isBefore(LocalDate.now())) {
                loan.setStatus("OVERDUE");
            } else {
                loan.setStatus("ACTIVE");
            }
        }
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
        if (payments == null) {
            return total;
        }
        for (Payment payment : payments) {
            if (payment != null && loanId.equalsIgnoreCase(safe(payment.getLoanId()))) {
                if (editingPayment != null && payment == editingPayment) {
                    continue;
                }
                total += payment.getAmount();
            }
        }
        return total;
    }

    private void validatePaymentAmount(Loan loan, double amount, LocalDate paymentDate) {
        if (loan == null) {
            throw new IllegalArgumentException("loan");
        }
        if (amount <= 0.0) {
            throw new IllegalArgumentException("amount");
        }

        double paidBeforeThisEntry = totalPaidForLoan(safe(loan.getId()));
        double remaining = calculator.computeRemainingBalance(loan, paidBeforeThisEntry);
        if (remaining <= 0.0) {
            throw new IllegalArgumentException("paid");
        }

        LocalDate nextDueDate = nextDueDateFor(loan, paidBeforeThisEntry);
        double dueAmount = calculator.computeCurrentInstallmentDue(loan, paidBeforeThisEntry);
        if (dueAmount <= 0.0) {
            dueAmount = remaining;
        }

        if (nextDueDate != null && isDueMonthOrLater(paymentDate, nextDueDate)) {
            double normalizedAmount = roundCurrency(amount);
            double normalizedDue = roundCurrency(Math.min(dueAmount, remaining));
            if (Math.abs(normalizedAmount - normalizedDue) > 0.009) {
                throw new IllegalArgumentException(
                    "Payment for the due month must exactly match the current amount due: " + money(normalizedDue)
                );
            }
            return;
        }

        if (amount - remaining > 0.009) {
            throw new IllegalArgumentException("amount");
        }
    }

    private LocalDate nextDueDateFor(Loan loan, double amountPaid) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        int remainingTerms = calculator.computeRemainingTermMonths(loan, amountPaid);
        if (remainingTerms <= 0) {
            return null;
        }
        int completedTerms = Math.max(0, loan.getOriginalTermMonths() - remainingTerms);
        return loan.getStartDate().plusMonths(completedTerms + 1L);
    }

    private boolean isDueMonthOrLater(LocalDate paymentDate, LocalDate dueDate) {
        if (paymentDate == null || dueDate == null) {
            return false;
        }
        YearMonth paymentMonth = YearMonth.from(paymentDate);
        YearMonth dueMonth = YearMonth.from(dueDate);
        return !paymentMonth.isBefore(dueMonth);
    }

    private double roundCurrency(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private void selectLoanOption(String loanId) {
        for (int i = 0; i < newLoanCombo.getItemCount(); i++) {
            String option = newLoanCombo.getItemAt(i);
            if (loanId.equalsIgnoreCase(extractLoanId(option))) {
                newLoanCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String extractLoanId(String option) {
        String value = safe(option).trim();
        int sep = value.indexOf(" - ");
        return sep >= 0 ? value.substring(0, sep).trim() : value;
    }

    private String buildLoanOptionLabel(Loan loan) {
        if (loan == null) {
            return "";
        }
        String loanId = safe(loan.getId());
        String borrowerName = resolveBorrowerName(safe(loan.getBorrowerId()));
        return borrowerName.isEmpty() ? loanId : loanId + " - " + borrowerName;
    }

    private String resolveBorrowerNameForLoan(String loanId) {
        Loan loan = findLoanById(loanId);
        return loan == null ? safe(loanId) : resolveBorrowerName(safe(loan.getBorrowerId()));
    }

    private String resolveBorrowerName(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return safe(borrowerId);
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                String name = safe(borrower.getFullName());
                return name.isEmpty() ? safe(borrowerId) : name;
            }
        }
        return safe(borrowerId);
    }

    private String resolveLoanNumber(String loanId) {
        Loan loan = findLoanById(loanId);
        return loan == null ? "" : safe(loan.getId());
    }

    private double estimateProfitPortion(Payment payment, Loan loan) {
        if (payment == null || loan == null) {
            return 0.0;
        }
        double totalPayable = calculator.computeTotalPayable(loan);
        if (totalPayable <= 0.0) {
            return 0.0;
        }
        double interestPortion = totalPayable - loan.getPrincipalAmount();
        return payment.getAmount() * (interestPortion / totalPayable);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String money(double amount) {
        return UITheme.formatCurrency(amount);
    }

    private String formatEditableAmount(double amount) {
        return String.format("%.2f", amount);
    }

    private String generatePaymentId() {
        int next = payments == null ? 1 : payments.size() + 1;
        String candidate = String.format("PM-%08d", next);
        while (findPaymentById(candidate) != null) {
            next++;
            candidate = String.format("PM-%08d", next);
        }
        return candidate;
    }

    private Payment findPaymentById(String paymentId) {
        if (payments == null || paymentId == null) {
            return null;
        }
        for (Payment payment : payments) {
            if (payment != null && paymentId.equalsIgnoreCase(safe(payment.getId()))) {
                return payment;
            }
        }
        return null;
    }

    private void startInlineEdit(Payment payment) {
        if (payment == null) {
            return;
        }
        editingPayment = payment;
        refreshLoanOptions();
        selectLoanOption(safe(payment.getLoanId()));
        newAmountField.setText(formatEditableAmount(payment.getAmount()));
        newDateField.setText(payment.getPaymentDate() == null ? LocalDate.now().toString() : payment.getPaymentDate().toString());
        savePaymentButton.setText("Save Changes");
        updatePaymentPreview();
        toggleInlinePaymentForm(true);
    }
}

