package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class PaymentPanel extends JPanel {
    private final ArrayList<Payment> payments;
    private final ArrayList<Loan> loans;
    private final LoanCalculator calculator;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PaymentPanel(ArrayList<Payment> payments, ArrayList<Loan> loans, LoanCalculator calculator) {
        this.payments = payments;
        this.loans = loans;
        this.calculator = calculator;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(new Object[]{"Payment ID", "Loan ID", "Amount", "Payment Date", "Method"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.applyTable(table);

        installStripedRenderer();

        JScrollPane scroll = new JScrollPane(table);
        UITheme.applyScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        JButton addBtn = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        UITheme.applyPrimaryButton(addBtn);
        UITheme.applySecondaryButton(deleteBtn);
        UITheme.applySecondaryButton(refreshBtn);

        addBtn.addActionListener(e -> showAddDialog());
        deleteBtn.addActionListener(e -> handleDelete());
        refreshBtn.addActionListener(e -> refreshTable());

        toolbar.add(addBtn);
        toolbar.add(deleteBtn);
        toolbar.add(refreshBtn);

        return toolbar;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Payment p : payments) {
            tableModel.addRow(new Object[]{
                    safe(p.getId()),
                    safe(p.getLoanId()),
                    String.format("%.2f", p.getAmount()),
                    p.getPaymentDate() == null ? "" : dateFmt.format(p.getPaymentDate()),
                    safe(p.getMethod())
            });
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void installStripedRenderer() {
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
                    Color bg = (row % 2 == 0) ? UITheme.CARD : UITheme.CARD_2;
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

    private void showAddDialog() {
        PaymentForm form = new PaymentForm();
        int result = form.showDialog(this, "Add Payment");
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Payment payment = form.toPayment();
        if (payment == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid amount and date (yyyy-MM-dd).",
                    "Payment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        payments.add(payment);
        updateLoanAfterPayment(payment.getLoanId());
        refreshTable();
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= payments.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a payment to delete.",
                    "Payments",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Payment existing = payments.get(row);
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
        payments.remove(row);
        updateLoanAfterPayment(loanId);
        refreshTable();
    }

    private void updateLoanAfterPayment(String loanId) {
        if (loanId == null) {
            return;
        }

        Loan loan = null;
        for (Loan l : loans) {
            if (loanId.equals(l.getId())) {
                loan = l;
                break;
            }
        }
        if (loan == null) {
            return;
        }

        double totalPaid = 0.0;
        for (Payment p : payments) {
            if (loanId.equals(p.getLoanId())) {
                totalPaid += p.getAmount();
            }
        }

        double remaining = calculator.computeRemainingBalance(loan, totalPaid);

        // update status based on remaining and due date
        if (remaining <= 0.0) {
            loan.setStatus("PAID");
        } else {
            LocalDate start = loan.getStartDate();
            LocalDate due = null;
            if (start != null && loan.getTermMonths() > 0) {
                due = start.plusMonths(loan.getTermMonths());
            }
            if (due != null && due.isBefore(LocalDate.now())) {
                loan.setStatus("OVERDUE");
            } else {
                loan.setStatus("ACTIVE");
            }
        }
    }

    private static class PaymentForm {
        private final JTextField idField = new JTextField(12);
        private final JTextField loanIdField = new JTextField(12);
        private final JTextField amountField = new JTextField(10);
        private final JTextField dateField = new JTextField(10);
        private final JTextField methodField = new JTextField(10);

        int showDialog(JPanel parent, String title) {
            JDialog dialog = new JDialog();
            dialog.setTitle(title);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(parent);

            JPanel panel = new JPanel(new java.awt.GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.insets = new java.awt.Insets(4, 4, 4, 4);
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            panel.add(new JLabel("Payment ID:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Loan ID:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(loanIdField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            panel.add(new JLabel("Amount:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(amountField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            panel.add(new JLabel("Payment Date (yyyy-MM-dd):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(dateField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0;
            panel.add(new JLabel("Method:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(methodField, gbc);

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

            Boolean cancelled = (Boolean) idField.getClientProperty("cancelled");
            idField.putClientProperty("cancelled", null);

            return Boolean.TRUE.equals(cancelled) ? JOptionPane.CANCEL_OPTION : JOptionPane.OK_OPTION;
        }

        Payment toPayment() {
            try {
                String id = idField.getText().trim();
                String loanId = loanIdField.getText().trim();
                double amount = Double.parseDouble(amountField.getText().trim());
                LocalDate date = null;
                String dateText = dateField.getText().trim();
                if (!dateText.isEmpty()) {
                    date = LocalDate.parse(dateText);
                }
                String method = methodField.getText().trim();

                return new Payment(id, loanId, amount, date, method);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}

