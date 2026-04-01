package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class BorrowerPanel extends JPanel {
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final LoanCalculator calculator;
    private final Runnable saveAction;

    private final JTextField searchField = new JTextField(18);
    private final JTextField newNameField = new JTextField(18);
    private final JTextField newGmailField = new JTextField(16);
    private final JTextField newAddressField = new JTextField(18);
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton saveBorrowerButton = new JButton("Save Borrower");
    private final JButton cancelBorrowerButton = new JButton("Cancel");
    private final JPanel inlineFormPanel = new JPanel(new BorderLayout(0, 12));
    private final JLabel inlineFormHelperLabel = new JLabel("", SwingConstants.LEFT);
    private Borrower editingBorrower;

    private final JLabel totalBorrowersValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel totalBorrowersBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeBorrowersValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel activeBorrowersBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel dueSoonValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel dueSoonBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel overdueValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel overdueBody = new JLabel("", SwingConstants.LEFT);

    public BorrowerPanel(
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
            "BORROWER ID", "NAME", "GMAIL", "ADDRESS", "ACTIVE LOANS", "TOTAL BALANCE"
        }, 0) {
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

        UITheme.applyTextField(searchField);
        UITheme.applyTextField(newNameField);
        UITheme.applyTextField(newGmailField);
        UITheme.applyTextField(newAddressField);
        inlineFormPanel.setOpaque(false);
        inlineFormPanel.setVisible(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Borrowers - Registry", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        JLabel subtitle = new JLabel("Maintain borrower profiles and view their loan history.", SwingConstants.LEFT);
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
        cards.add(createMetricCard("TOTAL BORROWERS", totalBorrowersValue, totalBorrowersBody, BLUE));
        cards.add(createMetricCard("ACTIVE LOANS", activeBorrowersValue, activeBorrowersBody, GREEN));
        cards.add(createMetricCard("DUE WITHIN 7 DAYS", dueSoonValue, dueSoonBody, ORANGE));
        cards.add(createMetricCard("OVERDUE BORROWERS", overdueValue, overdueBody, RED));

        body.add(cards, BorderLayout.NORTH);
        body.add(buildDirectoryCard(), BorderLayout.CENTER);
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

    private void styleHeaderActionButton(JButton button) {
        if (button == null) {
            return;
        }
        UITheme.applySecondaryButton(button);
        button.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        button.setFocusPainted(false);
    }

    private JPanel buildDirectoryCard() {
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

        JLabel title = new JLabel("Borrower Directory", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 22f));
        }

        JLabel subtitle = new JLabel("Add, update, or remove borrower records. Borrowers are required before creating loans.", SwingConstants.LEFT);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        searchField.setPreferredSize(new Dimension(180, 30));
        searchField.putClientProperty("JTextField.placeholderText", "Search borrower name...");

        JButton addBtn = new JButton("+ New borrower");
        styleHeaderActionButton(addBtn);
        addBtn.addActionListener(e -> toggleInlineBorrowerForm(true));

        styleHeaderActionButton(editButton);
        editButton.addActionListener(e -> {
            String selectedBorrowerId = getSelectedBorrowerId();
            if (selectedBorrowerId.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a borrower first.",
                    "Borrowers",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            showEditDialog(selectedBorrowerId);
        });

        styleHeaderActionButton(deleteButton);
        deleteButton.addActionListener(e -> {
            String selectedBorrowerId = getSelectedBorrowerId();
            if (selectedBorrowerId.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a borrower first.",
                    "Borrowers",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            handleDelete(selectedBorrowerId);
        });

        searchField.addActionListener(e -> refreshTable());
        right.add(searchField);
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
        content.add(buildInlineBorrowerForm(), BorderLayout.NORTH);
        content.add(tableShell, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInlineBorrowerForm() {
        RoundedPanel formShell = new RoundedPanel(18, UITheme.PANEL_BG);
        formShell.setBorderColor(UITheme.BORDER);
        formShell.setBorderWidth(1);
        formShell.setLayout(new BorderLayout(0, 12));
        formShell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel fields = new JPanel(new GridLayout(1, 3, 10, 8));
        fields.setOpaque(false);

        fields.add(createInlineField("Full name", newNameField));
        fields.add(createInlineField("Gmail", newGmailField));
        fields.add(createInlineField("Address", newAddressField));

        inlineFormHelperLabel.setForeground(UITheme.TEXT_MUTED);
        updateInlineFormState();

        UITheme.applySecondaryButton(cancelBorrowerButton);
        cancelBorrowerButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        cancelBorrowerButton.addActionListener(e -> toggleInlineBorrowerForm(false));

        UITheme.applyPrimaryButton(saveBorrowerButton);
        saveBorrowerButton.setBorder(new RoundedButtonBorder(18, UITheme.ACCENT, false));
        saveBorrowerButton.addActionListener(e -> handleInlineBorrowerSave());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelBorrowerButton);
        actions.add(saveBorrowerButton);

        formShell.add(fields, BorderLayout.CENTER);
        formShell.add(inlineFormHelperLabel, BorderLayout.NORTH);
        formShell.add(actions, BorderLayout.SOUTH);

        inlineFormPanel.removeAll();
        inlineFormPanel.add(formShell, BorderLayout.CENTER);
        return inlineFormPanel;
    }

    private JPanel createInlineField(String labelText, JTextField field) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText, SwingConstants.LEFT);
        label.setForeground(UITheme.TEXT_MUTED);

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(field);
        return wrapper;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);

        int totalBorrowers = borrowers == null ? 0 : borrowers.size();
        int activeLoanCount = 0;
        int dueSoonBorrowers = 0;
        int overdueBorrowers = 0;

        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (borrowers != null) {
            for (Borrower borrower : borrowers) {
                if (borrower == null) {
                    continue;
                }

                String borrowerId = safe(borrower.getId());
                String name = safe(borrower.getFullName());
                if (!keyword.isEmpty() && !name.toLowerCase().contains(keyword)) {
                    continue;
                }

                int activeLoans = countActiveLoans(borrowerId);
                double balance = totalBalanceForBorrower(borrowerId);
                boolean dueSoon = hasDueSoonLoan(borrowerId);
                boolean overdue = hasOverdueLoan(borrowerId);

                activeLoanCount += activeLoans;
                if (dueSoon) {
                    dueSoonBorrowers++;
                }
                if (overdue) {
                    overdueBorrowers++;
                }

                tableModel.addRow(new Object[]{
                    borrowerId,
                    name,
                    safe(borrower.getGmail()),
                    safe(borrower.getAddress()),
                    Integer.toString(activeLoans),
                    money(balance)
                });
            }
        }

        totalBorrowersValue.setText(Integer.toString(totalBorrowers));
        totalBorrowersBody.setText("All borrower profiles in the system.");
        activeBorrowersValue.setText(Integer.toString(activeLoanCount));
        activeBorrowersBody.setText("Loans currently active across all borrowers.");
        dueSoonValue.setText(Integer.toString(dueSoonBorrowers));
        dueSoonBody.setText("Borrowers with installments due soon.");
        overdueValue.setText(Integer.toString(overdueBorrowers));
        overdueBody.setText("Borrowers with overdue installments.");

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No borrower records yet.",
                "",
                "",
                "",
                ""
            });
        }

        table.clearSelection();
        handleSelectionChanged();
    }

    private int countActiveLoans(String borrowerId) {
        int count = 0;
        if (loans == null) {
            return count;
        }
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }
            String status = normalizeStatus(loan.getStatus());
            if ("ACTIVE".equals(status)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasDueSoonLoan(String borrowerId) {
        LocalDate today = LocalDate.now();
        if (loans == null) {
            return false;
        }
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }
            LocalDate dueDate = dueDateFor(loan);
            String status = normalizeStatus(loan.getStatus());
            if (dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7)) && !"PAID".equals(status)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOverdueLoan(String borrowerId) {
        LocalDate today = LocalDate.now();
        if (loans == null) {
            return false;
        }
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }
            LocalDate dueDate = dueDateFor(loan);
            String status = normalizeStatus(loan.getStatus());
            if (("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today))) && !"PAID".equals(status)) {
                return true;
            }
        }
        return false;
    }

    private double totalBalanceForBorrower(String borrowerId) {
        double total = 0.0;
        if (loans == null) {
            return total;
        }
        for (Loan loan : loans) {
            if (loan == null || !borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()))) {
                continue;
            }
            total += calculator.computeRemainingBalance(loan, paidAmountForBorrower(borrowerId));
        }
        return total;
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

    private LocalDate dueDateFor(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        return loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
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
                boolean emptyState = firstCell != null && firstCell.toString().startsWith("No borrower records");
                if (emptyState) {
                    label.setForeground(column == 0 ? UITheme.TEXT_MUTED : UITheme.PANEL_BG);
                    label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return label;
                }
                label.setHorizontalAlignment(column == 5 ? SwingConstants.RIGHT : SwingConstants.LEFT);
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
        getSelectedBorrower();
    }

    private Borrower getSelectedBorrower() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }

        Object idValue = table.getValueAt(selectedRow, 0);
        if (idValue == null) {
            return null;
        }

        String borrowerId = idValue.toString().trim();
        if (borrowerId.isEmpty()) {
            return null;
        }
        return findBorrowerById(borrowerId);
    }

    private String getSelectedBorrowerId() {
        Borrower selected = getSelectedBorrower();
        return selected == null ? "" : safe(selected.getId());
    }

    private void toggleInlineBorrowerForm(boolean visible) {
        inlineFormPanel.setVisible(visible);
        if (!visible) {
            clearInlineBorrowerForm();
        } else {
            updateInlineFormState();
            newNameField.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void clearInlineBorrowerForm() {
        editingBorrower = null;
        newNameField.setText("");
        newGmailField.setText("");
        newAddressField.setText("");
        updateInlineFormState();
    }

    private void updateInlineFormState() {
        boolean editing = editingBorrower != null;
        inlineFormHelperLabel.setText(editing
            ? "Update the selected borrower below, then click save to apply your changes."
            : "Borrower records are stored inline here. Fill in the details and save to add a new borrower.");
        saveBorrowerButton.setText(editing ? "Save Changes" : "Save Borrower");
    }

    private void startInlineEdit(Borrower borrower) {
        if (borrower == null) {
            return;
        }
        editingBorrower = borrower;
        newNameField.setText(safe(borrower.getFullName()));
        newGmailField.setText(safe(borrower.getGmail()));
        newAddressField.setText(safe(borrower.getAddress()));
        updateInlineFormState();
        toggleInlineBorrowerForm(true);
    }

    private void handleInlineBorrowerSave() {
        String name = newNameField.getText().trim();
        String gmail = newGmailField.getText().trim();
        String address = newAddressField.getText().trim();

        if (name.isEmpty() || gmail.isEmpty() || address.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please complete the full name, gmail, and address.",
                "Borrowers",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (editingBorrower != null) {
            editingBorrower.setFullName(name);
            editingBorrower.setGmail(gmail);
            editingBorrower.setAddress(address);
        } else {
            Borrower borrower = new Borrower(generateBorrowerId(), name, gmail, address);
            borrower.setGmail(gmail);
            borrowers.add(borrower);
        }
        saveAction.run();
        toggleInlineBorrowerForm(false);
        refreshTable();
    }

    private String generateBorrowerId() {
        int nextNumber = borrowers == null ? 1 : borrowers.size() + 1;
        String candidate = String.format("BW-%07d", nextNumber);
        while (findBorrowerById(candidate) != null) {
            nextNumber++;
            candidate = String.format("BW-%07d", nextNumber);
        }
        return candidate;
    }

    private void showEditDialog(String borrowerId) {
        Borrower existing = findBorrowerById(borrowerId);
        if (existing == null) {
            return;
        }
        startInlineEdit(existing);
    }

    private void handleDelete(String borrowerId) {
        Borrower existing = findBorrowerById(borrowerId);
        if (existing == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete borrower \"" + safe(existing.getFullName()) + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        borrowers.remove(existing);
        saveAction.run();
        refreshTable();
    }

    private Borrower findBorrowerById(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return null;
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                return borrower;
            }
        }
        return null;
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

    private static class BorrowerForm {
        private final JTextField idField = new JTextField(16);
        private final JTextField nameField = new JTextField(18);
        private final JTextField gmailField = new JTextField(16);
        private final JTextField addressField = new JTextField(18);

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
            panel.add(new JLabel("Borrower ID:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Full Name:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            panel.add(new JLabel("Gmail:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(gmailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            panel.add(new JLabel("Address:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(addressField, gbc);

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

        String getId() {
            return idField.getText().trim();
        }

        void setId(String id) {
            idField.setText(id == null ? "" : id);
        }

        String getName() {
            return nameField.getText().trim();
        }

        void setName(String name) {
            nameField.setText(name == null ? "" : name);
        }

        String getGmail() {
            return gmailField.getText().trim();
        }

        void setGmail(String gmail) {
            gmailField.setText(gmail == null ? "" : gmail);
        }

        String getAddress() {
            return addressField.getText().trim();
        }

        void setAddress(String address) {
            addressField.setText(address == null ? "" : address);
        }
    }
}
