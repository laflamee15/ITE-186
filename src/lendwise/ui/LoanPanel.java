package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import lendwise.models.Loan;
import lendwise.services.LoanCalculator;

public class LoanPanel extends JPanel {
    public interface LoanSelectionListener {
        void onLoanSelected(Loan loan);
    }

    private final ArrayList<Loan> loans;
    private final LoanCalculator calculator;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private LoanSelectionListener selectionListener;
    private final JComboBox<String> statusFilter;

    public LoanPanel(ArrayList<Loan> loans, LoanCalculator calculator) {
        this.loans = loans;
        this.calculator = calculator;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(
                new Object[]{"Loan ID", "Borrower ID", "Principal", "Rate (Annual %)", "Term (Months)", "Start Date", "Due Date", "Status"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.applyTable(table);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                notifySelection();
            }
        });

        // status filter
        statusFilter = new JComboBox<>(new String[]{"All", "Active", "Overdue", "Paid", "Pending", "Due Within 7 Days"});
        statusFilter.setBackground(UITheme.CARD_2);
        statusFilter.setForeground(UITheme.TEXT);

        add(buildToolbar(), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UITheme.applyScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

        refreshTable();
        installRowColoringRenderer();
    }

    public void setSelectionListener(LoanSelectionListener listener) {
        this.selectionListener = listener;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        JLabel filterLabel = new JLabel("Status:");
        filterLabel.setForeground(UITheme.TEXT_MUTED);
        UITheme.applyPrimaryButton(addBtn);
        UITheme.applySecondaryButton(editBtn);
        UITheme.applySecondaryButton(deleteBtn);
        UITheme.applySecondaryButton(refreshBtn);

        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> handleDelete());
        refreshBtn.addActionListener(e -> refreshTable());
        statusFilter.addActionListener(e -> refreshTable());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(refreshBtn);
        toolbar.add(filterLabel);
        toolbar.add(statusFilter);

        return toolbar;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        String filter = statusFilter.getSelectedItem() == null ? "All" : statusFilter.getSelectedItem().toString();

        for (Loan l : loans) {
            String status = safe(l.getStatus()).toUpperCase();
            if (!"ALL".equalsIgnoreCase(filter)) {
                if (!status.equalsIgnoreCase(filter)) {
                    continue;
                }
            }

            LocalDate start = l.getStartDate();
            LocalDate due = null;
            if (start != null && l.getTermMonths() > 0) {
                due = start.plusMonths(l.getTermMonths());
            }

            tableModel.addRow(new Object[]{
                    safe(l.getId()),
                    safe(l.getBorrowerId()),
                    String.format("%.2f", l.getPrincipalAmount()),
                    String.format("%.2f", l.getInterestRateAnnual()),
                    Integer.toString(l.getTermMonths()),
                    start == null ? "" : dateFmt.format(start),
                    due == null ? "" : dateFmt.format(due),
                    status
            });
        }
        notifySelection();
    }

    private void showAddDialog() {
        LoanForm form = new LoanForm();
        int result = form.showDialog(this, "Add Loan");
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Loan loan = form.toLoan();
        if (loan == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numeric values for principal, rate, term and valid date (yyyy-MM-dd).",
                    "Loan",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        // show computed amounts to the user
        double total = calculator.computeTotalPayable(loan);
        double monthly = calculator.computeMonthlyInstallment(loan);
        JOptionPane.showMessageDialog(
                this,
                String.format("Total Payable: %.2f%nMonthly Installment: %.2f", total, monthly),
                "Loan Summary",
                JOptionPane.INFORMATION_MESSAGE
        );

        loans.add(loan);
        refreshTable();
    }

    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= loans.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a loan to edit.",
                    "Loans",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Loan existing = loans.get(row);
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
        existing.setStatus(updated.getStatus());

        refreshTable();
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= loans.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a loan to delete.",
                    "Loans",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Loan existing = loans.get(row);
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete loan \"" + safe(existing.getId()) + "\"?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        loans.remove(row);
        refreshTable();
    }

    private void notifySelection() {
        if (selectionListener == null) {
            return;
        }

        int row = table.getSelectedRow();
        if (row < 0 || row >= loans.size()) {
            selectionListener.onLoanSelected(null);
            return;
        }

        selectionListener.onLoanSelected(loans.get(row));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void installRowColoringRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                javax.swing.JComponent c = (javax.swing.JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setOpaque(true);
                if (isSelected) {
                    c.setBackground(UITheme.ACCENT);
                    c.setForeground(java.awt.Color.WHITE);
                } else {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object statusObj = table.getModel().getValueAt(modelRow, 7); // Status column
                    Object dueObj = table.getModel().getValueAt(modelRow, 6);    // Due Date column

                    String status = statusObj == null ? "" : statusObj.toString().toUpperCase();
                    String dueText = dueObj == null ? "" : dueObj.toString();

                    LocalDate due = null;
                    if (!dueText.isEmpty()) {
                        try {
                            due = LocalDate.parse(dueText);
                        } catch (Exception ignored) {
                        }
                    }
                    Color bg = UITheme.CARD;
                    if ("PAID".equals(status)) {
                        bg = UITheme.CARD_2;
                    } else if ("OVERDUE".equals(status)) {
                        bg = new Color(0x3A, 0x16, 0x16);
                    } else if ("DUE WITHIN 7 DAYS".equals(status)) {
                        bg = new Color(0x3A, 0x2A, 0x12);
                    } else if (due != null) {
                        LocalDate today = LocalDate.now();
                        if (due.isBefore(today)) {
                            bg = new Color(0x3A, 0x16, 0x16);
                        } else if (!due.isBefore(today) && !due.isAfter(today.plusDays(7))) {
                            bg = new Color(0x3A, 0x2A, 0x12);
                        }
                    }
                    c.setBackground(bg);
                    c.setForeground(UITheme.TEXT);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /**
     * Simple modal form dialog to capture loan fields.
     * Status is chosen from a fixed list (not typed).
     */
    private static class LoanForm {
        private final JTextField idField = new JTextField(12);
        private final JTextField borrowerIdField = new JTextField(12);
        private final JTextField principalField = new JTextField(10);
        private final JTextField rateField = new JTextField(8);
        private final JTextField termField = new JTextField(6);
        private final JTextField startDateField = new JTextField(10); // yyyy-MM-dd
        private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "OVERDUE", "PAID", "PENDING", "DUE WITHIN 7 DAYS"});

        int showDialog(JPanel parent, String title) {
            JDialog dialog = new JDialog();
            dialog.setTitle(title);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(parent);

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

            dialog.setVisible(true);

            if (statusCombo.getSelectedItem() == null) {
                statusCombo.setSelectedItem("ACTIVE");
            }

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
            if (loan.getStartDate() != null) {
                startDateField.setText(loan.getStartDate().toString());
            } else {
                startDateField.setText("");
            }
            String status = loan.getStatus() == null ? "ACTIVE" : loan.getStatus().toUpperCase();
            statusCombo.setSelectedItem(status);
        }

        Loan toLoan() {
            try {
                String id = idField.getText().trim();
                String borrowerId = borrowerIdField.getText().trim();
                double principal = Double.parseDouble(principalField.getText().trim());
                double rate = Double.parseDouble(rateField.getText().trim());
                int term = Integer.parseInt(termField.getText().trim());
                LocalDate start = null;
                String startText = startDateField.getText().trim();
                if (!startText.isEmpty()) {
                    start = LocalDate.parse(startText);
                }
                String status = statusCombo.getSelectedItem() == null
                        ? "ACTIVE"
                        : statusCombo.getSelectedItem().toString();

                return new Loan(id, borrowerId, principal, rate, term, start, status);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}

