package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

import lendwise.models.Borrower;

public class BorrowerPanel extends JPanel {
    private final ArrayList<Borrower> borrowers;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField;

    public BorrowerPanel(ArrayList<Borrower> borrowers) {
        this.borrowers = borrowers;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        tableModel = new DefaultTableModel(new Object[]{"Borrower ID", "Full Name", "Contact No.", "Address"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.applyTable(table);

        installStripedRenderer();

        searchField = new JTextField(18);
        UITheme.applyTextField(searchField);

        add(buildSearchBar(), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UITheme.applyScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);

        refreshTable();
    }

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);
        JLabel searchLabel = new JLabel("Search by name:");
        searchLabel.setForeground(UITheme.TEXT_MUTED);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");
        UITheme.applySecondaryButton(searchBtn);
        UITheme.applySecondaryButton(clearBtn);

        searchBtn.addActionListener(e -> refreshTable());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        panel.add(searchLabel);
        panel.add(searchField);
        panel.add(searchBtn);
        panel.add(clearBtn);

        return panel;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        UITheme.applyPrimaryButton(addBtn);
        UITheme.applySecondaryButton(editBtn);
        UITheme.applySecondaryButton(deleteBtn);
        UITheme.applySecondaryButton(refreshBtn);

        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> handleDelete());
        refreshBtn.addActionListener(e -> refreshTable());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(refreshBtn);

        return toolbar;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);

        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        for (Borrower b : borrowers) {
            if (!keyword.isEmpty()) {
                String name = b.getFullName() == null ? "" : b.getFullName().toLowerCase();
                if (!name.contains(keyword)) {
                    continue;
                }
            }
            tableModel.addRow(new Object[]{
                    safe(b.getId()),
                    safe(b.getFullName()),
                    safe(b.getContactNumber()),
                    safe(b.getAddress())
            });
        }
    }

    private void showAddDialog() {
        BorrowerForm form = new BorrowerForm();
        int result = form.showDialog(this, "Add Borrower");
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Borrower b = new Borrower(
                form.getId(),
                form.getName(),
                form.getContactNumber(),
                form.getAddress()
        );
        borrowers.add(b);
        refreshTable();
    }

    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= borrowers.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a borrower to edit.",
                    "Borrowers",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Borrower existing = borrowers.get(row);
        BorrowerForm form = new BorrowerForm();
        form.setId(existing.getId());
        form.setName(existing.getFullName());
        form.setContactNumber(existing.getContactNumber());
        form.setAddress(existing.getAddress());

        int result = form.showDialog(this, "Edit Borrower");
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        existing.setId(form.getId());
        existing.setFullName(form.getName());
        existing.setContactNumber(form.getContactNumber());
        existing.setAddress(form.getAddress());

        refreshTable();
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= borrowers.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a borrower to delete.",
                    "Borrowers",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Borrower existing = borrowers.get(row);
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete borrower \"" + safe(existing.getFullName()) + "\"?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        borrowers.remove(row);
        refreshTable();
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

    /**
     * Simple modal form dialog to capture borrower fields.
     */
    private static class BorrowerForm {
        private final JTextField idField = new JTextField(16);
        private final JTextField nameField = new JTextField(18);
        private final JTextField contactField = new JTextField(16);
        private final JTextField addressField = new JTextField(18);

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
            panel.add(new JLabel("Contact No.:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(contactField, gbc);

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

        String getContactNumber() {
            return contactField.getText().trim();
        }

        void setContactNumber(String contact) {
            contactField.setText(contact == null ? "" : contact);
        }

        String getAddress() {
            return addressField.getText().trim();
        }

        void setAddress(String address) {
            addressField.setText(address == null ? "" : address);
        }
    }
}

