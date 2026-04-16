package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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
import javax.swing.TransferHandler;
import javax.swing.plaf.basic.BasicButtonUI;
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
    private static final Path ID_PHOTO_DIR = Paths.get("data", "borrower-ids");
    private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "webp"};

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final LoanCalculator calculator;
    private final Runnable saveAction;

    private final JTextField searchField = new JTextField(18);
    private final JTextField newNameField = new JTextField(18);
    private final JTextField newGmailField = new JTextField(16);
    private final JTextField newAddressField = new JTextField(18);
    private final JLabel governmentIdFileLabel = new JLabel("No government ID uploaded", SwingConstants.LEFT);
    private final JLabel governmentIdPreviewLabel = new JLabel("Government ID preview", SwingConstants.CENTER);
    private final JButton uploadGovernmentIdButton = new JButton("Select Government ID");
    private final JButton viewGovernmentIdButton = new JButton("Preview ID");
    private final JButton removeGovernmentIdButton = new JButton("Remove");
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton saveBorrowerButton = new JButton("Save Borrower");
    private final JButton cancelBorrowerButton = new JButton("Cancel");
    private final JPanel inlineFormPanel = new JPanel(new BorderLayout(0, 12));
    private final JLabel inlineFormHelperLabel = new JLabel("", SwingConstants.LEFT);
    private Borrower editingBorrower;
    private Path pendingGovernmentIdSource;
    private boolean removeExistingGovernmentId;

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
            "BORROWER ID", "NAME", "GMAIL", "LINK STATUS", "ADDRESS", "ACTIVE LOANS", "TOTAL BALANCE"
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
        governmentIdFileLabel.setForeground(UITheme.TEXT_MUTED);
        governmentIdFileLabel.setFont(UITheme.createFont(Font.PLAIN, 12.5f));
        governmentIdPreviewLabel.setForeground(UITheme.TEXT_MUTED);
        governmentIdPreviewLabel.setFont(UITheme.createFont(Font.PLAIN, 12.5f));
        governmentIdPreviewLabel.setOpaque(true);
        governmentIdPreviewLabel.setBackground(UITheme.CARD_2);
        governmentIdPreviewLabel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(16, UITheme.BORDER, false),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        styleModernActionButton(uploadGovernmentIdButton, UITheme.ACCENT, true, false);
        styleGovernmentIdActionButton(viewGovernmentIdButton, new Color(0x38, 0x7B, 0xF6), false);
        styleGovernmentIdActionButton(removeGovernmentIdButton, new Color(0xE1, 0x6A, 0x5F), true);
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

    private void styleHeaderActionButton(JButton button, boolean primary) {
        if (button == null) {
            return;
        }
        UITheme.applyHeaderActionButton(button, primary);
    }

    private void styleGovernmentIdActionButton(JButton button, Color accent, boolean destructive) {
        styleModernActionButton(button, accent, !destructive, destructive);
    }

    private void styleModernActionButton(JButton button, Color accent, boolean primary, boolean destructive) {
        if (button == null) {
            return;
        }
        Color fill = destructive
            ? accent
            : primary
                ? accent
                : (UITheme.isLightMode() ? new Color(0xE3, 0xEE, 0xFF) : new Color(0x1B, 0x30, 0x4D));
        Color hover = destructive
            ? accent.brighter()
            : primary
                ? accent.brighter()
                : (UITheme.isLightMode() ? new Color(0xD4, 0xE4, 0xFF) : new Color(0x23, 0x3D, 0x62));
        Color disabledFill = primary
            ? (UITheme.isLightMode() ? new Color(0xD9, 0xE4, 0xF7) : new Color(0x2A, 0x3A, 0x52))
            : (UITheme.isLightMode() ? new Color(0xEC, 0xF1, 0xF8) : new Color(0x20, 0x2A, 0x3A));
        Color text = destructive || primary
            ? Color.WHITE
            : (UITheme.isLightMode() ? new Color(0x12, 0x47, 0x9B) : new Color(0xE8, 0xF1, 0xFF));
        Color disabledText = UITheme.isLightMode() ? new Color(0x88, 0x99, 0xB5) : new Color(0x93, 0xA4, 0xBF);

        button.putClientProperty("gov.fill", fill);
        button.putClientProperty("gov.hover", hover);
        button.putClientProperty("gov.disabledFill", disabledFill);
        button.putClientProperty("gov.border", accent);
        button.putClientProperty("gov.text", text);
        button.putClientProperty("gov.disabledText", disabledText);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setForeground(text);
        button.setFont(UITheme.createFont(Font.BOLD, 12.5f));
        button.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                AbstractButton paintedButton = (AbstractButton) c;
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color currentFill = !paintedButton.isEnabled()
                    ? (Color) paintedButton.getClientProperty("gov.disabledFill")
                    : paintedButton.getModel().isRollover()
                    ? (Color) paintedButton.getClientProperty("gov.hover")
                    : (Color) paintedButton.getClientProperty("gov.fill");
                Color currentBorder = (Color) paintedButton.getClientProperty("gov.border");
                Color currentText = paintedButton.isEnabled()
                    ? (Color) paintedButton.getClientProperty("gov.text")
                    : (Color) paintedButton.getClientProperty("gov.disabledText");

                g2.setColor(currentFill);
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 20, 20);
                g2.setColor(currentBorder);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 20, 20);

                g2.setColor(currentText);
                g2.setFont(paintedButton.getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String textValue = paintedButton.getText();
                int x = (c.getWidth() - fm.stringWidth(textValue)) / 2;
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(textValue, x, y);
                g2.dispose();
            }
        });
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

        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        searchField.setPreferredSize(new Dimension(180, 30));
        searchField.putClientProperty("JTextField.placeholderText", "Search borrower name...");

        JButton addBtn = new JButton("+ New borrower");
        styleHeaderActionButton(addBtn, true);
        addBtn.addActionListener(e -> {
            clearInlineBorrowerForm();
            toggleInlineBorrowerForm(true);
        });

        styleHeaderActionButton(editButton, false);
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

        styleHeaderActionButton(deleteButton, false);
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

        JPanel idField = createGovernmentIdField();

        inlineFormHelperLabel.setForeground(UITheme.TEXT_MUTED);
        updateInlineFormState();

        UITheme.applySecondaryButton(cancelBorrowerButton);
        cancelBorrowerButton.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        cancelBorrowerButton.addActionListener(e -> toggleInlineBorrowerForm(false));

        UITheme.applyPrimaryButton(saveBorrowerButton);
        saveBorrowerButton.setBorder(new RoundedButtonBorder(18, UITheme.ACCENT, false));
        saveBorrowerButton.addActionListener(e -> handleInlineBorrowerSave());

        uploadGovernmentIdButton.addActionListener(e -> selectGovernmentIdPhoto());
        viewGovernmentIdButton.addActionListener(e -> showGovernmentIdPreviewDialog());
        removeGovernmentIdButton.addActionListener(e -> clearGovernmentIdSelection());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelBorrowerButton);
        actions.add(saveBorrowerButton);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(fields, BorderLayout.NORTH);
        content.add(idField, BorderLayout.CENTER);

        formShell.add(content, BorderLayout.CENTER);
        formShell.add(inlineFormHelperLabel, BorderLayout.NORTH);
        formShell.add(actions, BorderLayout.SOUTH);

        inlineFormPanel.removeAll();
        inlineFormPanel.add(formShell, BorderLayout.CENTER);
        return inlineFormPanel;
    }

    private JPanel createGovernmentIdField() {
        RoundedPanel card = new RoundedPanel(16, UITheme.CARD_2);
        card.setBorderColor(UITheme.BORDER);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Government ID", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        title.setFont(UITheme.createFont(Font.BOLD, 13f));

        JLabel helper = new JLabel("Required for every new borrower record.", SwingConstants.LEFT);
        helper.setForeground(UITheme.TEXT_MUTED);
        helper.setFont(UITheme.createFont(Font.PLAIN, 12f));

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(helper);
        left.add(Box.createVerticalStrut(10));
        left.add(governmentIdFileLabel);
        left.add(Box.createVerticalStrut(10));
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.add(uploadGovernmentIdButton);
        actionRow.add(viewGovernmentIdButton);
        actionRow.add(removeGovernmentIdButton);
        left.add(actionRow);

        governmentIdPreviewLabel.setPreferredSize(new Dimension(220, 130));

        card.add(left, BorderLayout.CENTER);
        card.add(governmentIdPreviewLabel, BorderLayout.EAST);
        return card;
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
                    safe(borrower.getLinkedAccountEmail()).isBlank() ? "NOT LINKED" : "LINKED",
                    safe(borrower.getAddress()),
                    Integer.toString(activeLoans),
                    money(balance)
                });
            }
        }

        totalBorrowersValue.setText(Integer.toString(totalBorrowers));
        activeBorrowersValue.setText(Integer.toString(activeLoanCount));
        dueSoonValue.setText(Integer.toString(dueSoonBorrowers));
        overdueValue.setText(Integer.toString(overdueBorrowers));

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{
                "No borrower records yet.",
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
                if (column == 6) {
                    label.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (column == 3 || column == 5) {
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (column == 3) {
                    String status = value == null ? "" : value.toString().trim();
                    if ("LINKED".equalsIgnoreCase(status)) {
                        label.setForeground(GREEN);
                    } else if ("NOT LINKED".equalsIgnoreCase(status)) {
                        label.setForeground(ORANGE);
                    }
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
        pendingGovernmentIdSource = null;
        removeExistingGovernmentId = false;
        newNameField.setText("");
        newGmailField.setText("");
        newAddressField.setText("");
        refreshGovernmentIdPreview("");
        updateInlineFormState();
    }

    private void updateInlineFormState() {
        boolean editing = editingBorrower != null;
        inlineFormHelperLabel.setText(editing
            ? "Update the selected borrower below. Government ID stays required and you can replace the uploaded photo anytime."
            : "Borrower records are stored inline here. Fill in the details, upload a government ID, and save to add a new borrower.");
        saveBorrowerButton.setText(editing ? "Save Changes" : "Save Borrower");
    }

    private void startInlineEdit(Borrower borrower) {
        if (borrower == null) {
            return;
        }
        editingBorrower = borrower;
        pendingGovernmentIdSource = null;
        removeExistingGovernmentId = false;
        newNameField.setText(safe(borrower.getFullName()));
        newGmailField.setText(safe(borrower.getGmail()));
        newAddressField.setText(safe(borrower.getAddress()));
        refreshGovernmentIdPreview(borrower.getGovernmentIdPhotoPath());
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

        String currentGovernmentIdPath = editingBorrower == null || removeExistingGovernmentId
            ? ""
            : safe(editingBorrower.getGovernmentIdPhotoPath());
        boolean missingGovernmentId = editingBorrower == null
            ? pendingGovernmentIdSource == null
            : pendingGovernmentIdSource == null && currentGovernmentIdPath.isEmpty();
        if (missingGovernmentId) {
            JOptionPane.showMessageDialog(
                this,
                "Please upload a government ID photo before saving this borrower.",
                "Borrowers",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            if (editingBorrower != null) {
                editingBorrower.setFullName(name);
                editingBorrower.setGmail(gmail);
                editingBorrower.setAddress(address);
                String previousPhotoPath = editingBorrower.getGovernmentIdPhotoPath();
                editingBorrower.setGovernmentIdPhotoPath(
                    storeGovernmentIdPhoto(editingBorrower.getId(), pendingGovernmentIdSource, previousPhotoPath)
                );
            } else {
                Borrower borrower = new Borrower(generateBorrowerId(), name, gmail, address);
                borrower.setGmail(gmail);
                borrower.setGovernmentIdPhotoPath(storeGovernmentIdPhoto(borrower.getId(), pendingGovernmentIdSource, ""));
                borrowers.add(borrower);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to save the government ID photo right now. Please try again.",
                "Borrowers",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        saveAction.run();
        toggleInlineBorrowerForm(false);
        refreshTable();
    }

    private void selectGovernmentIdPhoto() {
        Path selectedPhoto = showGovernmentIdPickerDialog();
        if (selectedPhoto == null) {
            return;
        }
        pendingGovernmentIdSource = selectedPhoto;
        removeExistingGovernmentId = false;
        refreshGovernmentIdPreview(pendingGovernmentIdSource.toString());
    }

    private Path showGovernmentIdPickerDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = owner instanceof Frame frame
            ? new JDialog(frame, "Select Government ID", true)
            : owner instanceof Dialog ownerDialog
                ? new JDialog(ownerDialog, "Select Government ID", true)
                : new JDialog((Frame) null, "Select Government ID", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        Path home = Paths.get(System.getProperty("user.home", "."));
        Path pictures = home.resolve("Pictures");
        Path downloads = home.resolve("Downloads");
        Path documents = home.resolve("Documents");
        Path desktop = home.resolve("Desktop");

        Path[] currentDirectory = new Path[] {
            pendingGovernmentIdSource != null && Files.exists(pendingGovernmentIdSource.getParent())
                ? pendingGovernmentIdSource.getParent()
                : Files.exists(pictures) ? pictures : home
        };
        Path[] selectedImage = new Path[] { null };
        JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        gridPanel.setOpaque(false);
        JPanel recentStrip = new JPanel();
        recentStrip.setOpaque(false);
        recentStrip.setLayout(new BoxLayout(recentStrip, BoxLayout.X_AXIS));

        JLabel currentFolderLabel = new JLabel("", SwingConstants.LEFT);
        currentFolderLabel.setForeground(UITheme.TEXT);
        currentFolderLabel.setFont(UITheme.createFont(Font.BOLD, 13f));

        JLabel pickerHelperLabel = new JLabel("Browse folders, tap a thumbnail, or drag and drop an image here.", SwingConstants.LEFT);
        pickerHelperLabel.setForeground(UITheme.TEXT_MUTED);
        pickerHelperLabel.setFont(UITheme.createFont(Font.PLAIN, 12f));

        JLabel pickerPreview = new JLabel("Preview", SwingConstants.CENTER);
        pickerPreview.setOpaque(true);
        pickerPreview.setBackground(UITheme.CARD_2);
        pickerPreview.setForeground(UITheme.TEXT_MUTED);
        pickerPreview.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, UITheme.BORDER, false),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        pickerPreview.setPreferredSize(new Dimension(290, 220));

        JLabel pickerMetaLabel = new JLabel("No image selected", SwingConstants.LEFT);
        pickerMetaLabel.setForeground(UITheme.TEXT_MUTED);
        pickerMetaLabel.setFont(UITheme.createFont(Font.PLAIN, 12f));

        JButton chooseButton = new JButton("Use This Photo");
        styleModernActionButton(chooseButton, UITheme.ACCENT, true, false);
        chooseButton.setEnabled(false);

        JButton cancelButton = new JButton("Cancel");
        styleModernActionButton(cancelButton, new Color(0x6E, 0x87, 0xB7), false, false);

        JButton backButton = new JButton("Up One Folder");
        styleModernActionButton(backButton, new Color(0x6E, 0x87, 0xB7), false, false);

        Runnable[] refreshDirectory = new Runnable[1];
        Runnable[] refreshPreview = new Runnable[1];
        Runnable[] refreshRecentStrip = new Runnable[1];

        refreshPreview[0] = () -> {
            Path selectedPath = selectedImage[0];
            pickerPreview.setIcon(null);
            pickerPreview.setText("Preview");
            pickerMetaLabel.setText("No image selected");
            chooseButton.setEnabled(false);

            if (selectedPath == null || !Files.exists(selectedPath) || Files.isDirectory(selectedPath)) {
                return;
            }

            ImageIcon icon = new ImageIcon(selectedPath.toString());
            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                pickerPreview.setText("Preview unavailable");
                pickerMetaLabel.setText(selectedPath.getFileName() == null ? selectedPath.toString() : selectedPath.getFileName().toString());
                return;
            }

            Image scaled = icon.getImage().getScaledInstance(290, 220, Image.SCALE_SMOOTH);
            pickerPreview.setText("");
            pickerPreview.setIcon(new ImageIcon(scaled));
            pickerMetaLabel.setText(selectedPath.getFileName() + "  |  " + readableFileSize(selectedPath));
            chooseButton.setEnabled(true);
        };

        refreshRecentStrip[0] = () -> {
            recentStrip.removeAll();
            ArrayList<Path> recentFiles = recentGovernmentIdPhotos();
            if (pendingGovernmentIdSource != null && Files.exists(pendingGovernmentIdSource)
                && recentFiles.stream().noneMatch(path -> path.toAbsolutePath().normalize().equals(pendingGovernmentIdSource.toAbsolutePath().normalize()))) {
                recentFiles.add(0, pendingGovernmentIdSource);
            }

            if (recentFiles.isEmpty()) {
                JLabel empty = new JLabel("No recent uploads yet", SwingConstants.LEFT);
                empty.setForeground(UITheme.TEXT_MUTED);
                recentStrip.add(empty);
            } else {
                boolean first = true;
                for (Path recentFile : recentFiles) {
                    if (!first) {
                        recentStrip.add(Box.createHorizontalStrut(10));
                    }
                    recentStrip.add(createPickerTile(
                        recentFile,
                        false,
                        currentDirectory,
                        selectedImage,
                        refreshDirectory,
                        refreshPreview,
                        dialog,
                        true
                    ));
                    first = false;
                }
            }
            recentStrip.revalidate();
            recentStrip.repaint();
        };

        refreshDirectory[0] = () -> {
            gridPanel.removeAll();
            currentFolderLabel.setText(currentDirectory[0].toAbsolutePath().normalize().toString());
            List<Path> entries = listPickerEntries(currentDirectory[0]);
            if (entries.isEmpty()) {
                JLabel empty = new JLabel("No folders or supported images found here.", SwingConstants.LEFT);
                empty.setForeground(UITheme.TEXT_MUTED);
                gridPanel.add(empty);
            } else {
                for (Path entry : entries) {
                    gridPanel.add(createPickerTile(
                        entry,
                        Files.isDirectory(entry),
                        currentDirectory,
                        selectedImage,
                        refreshDirectory,
                        refreshPreview,
                        dialog,
                        false
                    ));
                }
            }
            selectedImage[0] = null;
            refreshPreview[0].run();
            gridPanel.revalidate();
            gridPanel.repaint();
        };

        backButton.addActionListener(e -> {
            Path parent = currentDirectory[0] == null ? null : currentDirectory[0].getParent();
            if (parent == null || !Files.exists(parent)) {
                return;
            }
            currentDirectory[0] = parent;
            refreshDirectory[0].run();
        });

        chooseButton.addActionListener(e -> dialog.dispose());
        cancelButton.addActionListener(e -> {
            selectedImage[0] = null;
            dialog.dispose();
        });

        JPanel shortcuts = new JPanel(new GridLayout(5, 1, 0, 8));
        shortcuts.setOpaque(false);
        shortcuts.add(createPickerShortcutButton("Pictures", pictures, currentDirectory, refreshDirectory));
        shortcuts.add(createPickerShortcutButton("Downloads", downloads, currentDirectory, refreshDirectory));
        shortcuts.add(createPickerShortcutButton("Documents", documents, currentDirectory, refreshDirectory));
        shortcuts.add(createPickerShortcutButton("Desktop", desktop, currentDirectory, refreshDirectory));
        shortcuts.add(createPickerShortcutButton("Home", home, currentDirectory, refreshDirectory));

        RoundedPanel shortcutsCard = new RoundedPanel(18, UITheme.CARD_2);
        shortcutsCard.setBorderColor(UITheme.BORDER);
        shortcutsCard.setBorderWidth(1);
        shortcutsCard.setLayout(new BorderLayout());
        shortcutsCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        shortcutsCard.add(shortcuts, BorderLayout.NORTH);

        JScrollPane recentScroll = new JScrollPane(recentStrip);
        recentScroll.setBorder(BorderFactory.createEmptyBorder());
        recentScroll.getViewport().setBackground(UITheme.PANEL_BG);
        recentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        recentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        recentScroll.setPreferredSize(new Dimension(0, 112));

        JScrollPane gridScroll = new JScrollPane(gridPanel);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        gridScroll.getViewport().setBackground(UITheme.PANEL_BG);
        gridScroll.getVerticalScrollBar().setUnitIncrement(18);

        RoundedPanel listCard = new RoundedPanel(18, UITheme.PANEL_BG);
        listCard.setBorderColor(UITheme.BORDER);
        listCard.setBorderWidth(1);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        RoundedPanel recentCard = new RoundedPanel(18, UITheme.CARD_2);
        recentCard.setBorderColor(UITheme.BORDER);
        recentCard.setBorderWidth(1);
        recentCard.setLayout(new BorderLayout(0, 10));
        recentCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel recentTitle = new JLabel("Recent Uploads", SwingConstants.LEFT);
        recentTitle.setForeground(UITheme.TEXT);
        recentTitle.setFont(UITheme.createFont(Font.BOLD, 13f));
        recentCard.add(recentTitle, BorderLayout.NORTH);
        recentCard.add(recentScroll, BorderLayout.CENTER);

        JPanel listTop = new JPanel(new BorderLayout(0, 6));
        listTop.setOpaque(false);
        listTop.add(currentFolderLabel, BorderLayout.NORTH);
        listTop.add(pickerHelperLabel, BorderLayout.CENTER);

        JPanel listActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        listActions.setOpaque(false);
        listActions.add(backButton);
        listTop.add(listActions, BorderLayout.SOUTH);

        listCard.add(listTop, BorderLayout.NORTH);
        listCard.add(gridScroll, BorderLayout.CENTER);

        RoundedPanel previewCard = new RoundedPanel(18, UITheme.CARD_2);
        previewCard.setBorderColor(UITheme.BORDER);
        previewCard.setBorderWidth(1);
        previewCard.setLayout(new BorderLayout(0, 12));
        previewCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel previewTitle = new JLabel("Image Preview", SwingConstants.LEFT);
        previewTitle.setForeground(UITheme.TEXT);
        previewTitle.setFont(UITheme.createFont(Font.BOLD, 13f));

        JPanel previewBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        previewBottom.setOpaque(false);
        previewBottom.add(pickerMetaLabel);

        previewCard.add(previewTitle, BorderLayout.NORTH);
        previewCard.add(pickerPreview, BorderLayout.CENTER);
        previewCard.add(previewBottom, BorderLayout.SOUTH);

        JPanel browserStack = new JPanel(new BorderLayout(0, 12));
        browserStack.setOpaque(false);
        browserStack.add(recentCard, BorderLayout.NORTH);
        browserStack.add(listCard, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout(12, 0));
        main.setOpaque(false);
        main.add(shortcutsCard, BorderLayout.WEST);
        main.add(browserStack, BorderLayout.CENTER);
        main.add(previewCard, BorderLayout.EAST);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(cancelButton);
        footer.add(chooseButton);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UITheme.PANEL_BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.add(main, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        installPickerDropTarget(root, currentDirectory, selectedImage, refreshDirectory, refreshPreview, refreshRecentStrip);
        installPickerDropTarget(gridPanel, currentDirectory, selectedImage, refreshDirectory, refreshPreview, refreshRecentStrip);

        dialog.setContentPane(root);
        dialog.setSize(980, 560);
        dialog.setLocationRelativeTo(this);
        refreshRecentStrip[0].run();
        refreshDirectory[0].run();
        dialog.setVisible(true);
        return selectedImage[0];
    }

    private JButton createPickerShortcutButton(String label, Path directory, Path[] currentDirectory, Runnable[] refreshDirectory) {
        JButton button = new JButton(label);
        UITheme.applySecondaryButton(button);
        button.setBorder(new RoundedButtonBorder(18, UITheme.BORDER, false));
        button.setEnabled(directory != null && Files.exists(directory));
        button.addActionListener(e -> {
            if (directory == null || !Files.exists(directory)) {
                return;
            }
            currentDirectory[0] = directory;
            refreshDirectory[0].run();
        });
        return button;
    }

    private JPanel createPickerTile(
        Path path,
        boolean directory,
        Path[] currentDirectory,
        Path[] selectedImage,
        Runnable[] refreshDirectory,
        Runnable[] refreshPreview,
        JDialog dialog,
        boolean compact
    ) {
        int tileWidth = compact ? 110 : 136;
        int previewWidth = compact ? 86 : 112;
        int previewHeight = compact ? 62 : 84;

        RoundedPanel tile = new RoundedPanel(16, UITheme.CARD_2);
        tile.setBorderColor(UITheme.BORDER);
        tile.setBorderWidth(1);
        tile.setLayout(new BorderLayout(0, 8));
        tile.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tile.setPreferredSize(new Dimension(tileWidth, compact ? 100 : 124));

        JLabel imageLabel = new JLabel(directory ? "Folder" : "Image", SwingConstants.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(UITheme.PANEL_BG);
        imageLabel.setForeground(UITheme.TEXT_MUTED);
        imageLabel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(14, UITheme.BORDER, false),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        imageLabel.setPreferredSize(new Dimension(previewWidth, previewHeight));

        if (directory) {
            imageLabel.setText("\u25A3");
            imageLabel.setFont(UITheme.createFont(Font.BOLD, compact ? 18f : 22f));
        } else {
            ImageIcon thumbnail = createScaledImageIcon(path, previewWidth, previewHeight);
            if (thumbnail != null) {
                imageLabel.setText("");
                imageLabel.setIcon(thumbnail);
            }
        }

        JLabel nameLabel = new JLabel(path.getFileName() == null ? path.toString() : path.getFileName().toString(), SwingConstants.CENTER);
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setFont(UITheme.createFont(Font.PLAIN, compact ? 11f : 12f));

        Runnable handleClick = () -> {
            if (directory) {
                currentDirectory[0] = path;
                refreshDirectory[0].run();
            } else {
                selectedImage[0] = path;
                refreshPreview[0].run();
                if (compact) {
                    dialog.dispose();
                }
            }
        };

        java.awt.event.MouseAdapter clickAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleClick.run();
            }
        };
        tile.addMouseListener(clickAdapter);
        imageLabel.addMouseListener(clickAdapter);
        nameLabel.addMouseListener(clickAdapter);

        tile.add(imageLabel, BorderLayout.CENTER);
        tile.add(nameLabel, BorderLayout.SOUTH);
        return tile;
    }

    private ImageIcon createScaledImageIcon(Path path, int width, int height) {
        if (path == null || !Files.exists(path) || Files.isDirectory(path)) {
            return null;
        }
        ImageIcon icon = new ImageIcon(path.toString());
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void installPickerDropTarget(
        Component target,
        Path[] currentDirectory,
        Path[] selectedImage,
        Runnable[] refreshDirectory,
        Runnable[] refreshPreview,
        Runnable[] refreshRecentStrip
    ) {
        if (!(target instanceof javax.swing.JComponent component)) {
            return;
        }
        component.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<java.io.File> files = (List<java.io.File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    for (java.io.File file : files) {
                        Path path = file.toPath();
                        if (Files.isDirectory(path)) {
                            currentDirectory[0] = path;
                            refreshDirectory[0].run();
                            return true;
                        }
                        if (isSupportedImageFile(path)) {
                            currentDirectory[0] = path.getParent() == null ? currentDirectory[0] : path.getParent();
                            refreshDirectory[0].run();
                            selectedImage[0] = path;
                            refreshPreview[0].run();
                            refreshRecentStrip[0].run();
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                }
                return false;
            }
        });
    }

    private ArrayList<Path> recentGovernmentIdPhotos() {
        ArrayList<Path> recentFiles = new ArrayList<>();
        if (!Files.exists(ID_PHOTO_DIR) || !Files.isDirectory(ID_PHOTO_DIR)) {
            return recentFiles;
        }
        try (var stream = Files.list(ID_PHOTO_DIR)) {
            stream
                .filter(this::isSupportedImageFile)
                .sorted(Comparator.comparingLong(this::lastModifiedSafe).reversed())
                .limit(8)
                .forEach(recentFiles::add);
        } catch (IOException ignored) {
        }
        return recentFiles;
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private List<Path> listPickerEntries(Path directory) {
        ArrayList<Path> entries = new ArrayList<>();
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return entries;
        }
        try (var stream = Files.list(directory)) {
            stream
                .filter(path -> Files.isDirectory(path) || isSupportedImageFile(path))
                .sorted(Comparator
                    .comparing((Path path) -> !Files.isDirectory(path))
                    .thenComparing(path -> safe(path.getFileName() == null ? "" : path.getFileName().toString()).toLowerCase()))
                .forEach(entries::add);
        } catch (IOException ignored) {
        }
        return entries;
    }

    private boolean isSupportedImageFile(Path path) {
        if (path == null || Files.isDirectory(path)) {
            return false;
        }
        String fileName = safe(path.getFileName() == null ? "" : path.getFileName().toString()).toLowerCase();
        for (String extension : IMAGE_EXTENSIONS) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    private String readableFileSize(Path path) {
        try {
            long size = Files.size(path);
            if (size < 1024) {
                return size + " B";
            }
            if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            }
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } catch (IOException ex) {
            return "Unknown size";
        }
    }

    private void refreshGovernmentIdPreview(String photoPath) {
        String normalizedPath = safe(photoPath).trim();
        governmentIdPreviewLabel.setIcon(null);
        governmentIdPreviewLabel.setText("Government ID preview");

        if (normalizedPath.isEmpty()) {
            governmentIdFileLabel.setText("No government ID uploaded");
            return;
        }

        Path file = Paths.get(normalizedPath);
        governmentIdFileLabel.setText(file.getFileName() == null ? normalizedPath : file.getFileName().toString());
        if (!Files.exists(file)) {
            governmentIdPreviewLabel.setText("Preview unavailable");
            return;
        }

        ImageIcon icon = new ImageIcon(normalizedPath);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            governmentIdPreviewLabel.setText("Preview unavailable");
            return;
        }

        Image scaled = icon.getImage().getScaledInstance(220, 130, Image.SCALE_SMOOTH);
        governmentIdPreviewLabel.setText("");
        governmentIdPreviewLabel.setIcon(new ImageIcon(scaled));
    }

    private void clearGovernmentIdSelection() {
        pendingGovernmentIdSource = null;
        removeExistingGovernmentId = editingBorrower != null && !safe(editingBorrower.getGovernmentIdPhotoPath()).isEmpty();
        refreshGovernmentIdPreview("");
    }

    private void showGovernmentIdPreviewDialog() {
        String photoPath = pendingGovernmentIdSource != null
            ? pendingGovernmentIdSource.toString()
            : editingBorrower == null || removeExistingGovernmentId ? "" : safe(editingBorrower.getGovernmentIdPhotoPath());
        if (photoPath.isEmpty() || !Files.exists(Paths.get(photoPath))) {
            JOptionPane.showMessageDialog(
                this,
                "No government ID image is available to preview.",
                "Borrowers",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        ImageIcon icon = new ImageIcon(photoPath);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to open the government ID preview.",
                "Borrowers",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = Math.max(420, (int) Math.round(screenSize.width * 0.68));
        int maxHeight = Math.max(320, (int) Math.round(screenSize.height * 0.68));
        double widthScale = (double) maxWidth / Math.max(1, icon.getIconWidth());
        double heightScale = (double) maxHeight / Math.max(1, icon.getIconHeight());
        double scale = Math.min(1.0, Math.min(widthScale, heightScale));
        int targetWidth = Math.max(280, (int) Math.round(icon.getIconWidth() * scale));
        int targetHeight = Math.max(180, (int) Math.round(icon.getIconHeight() * scale));
        Image scaled = icon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        JLabel preview = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(UITheme.PANEL_BG);
        preview.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(preview);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.PANEL_BG);
        scrollPane.setPreferredSize(new Dimension(
            Math.min(maxWidth + 24, targetWidth + 40),
            Math.min(maxHeight + 24, targetHeight + 40)
        ));

        JOptionPane.showMessageDialog(this, scrollPane, "Government ID Preview", JOptionPane.PLAIN_MESSAGE);
    }

    private String storeGovernmentIdPhoto(String borrowerId, Path sourcePath, String existingPath) throws IOException {
        if (sourcePath == null) {
            return safe(existingPath);
        }
        Files.createDirectories(ID_PHOTO_DIR);
        String extension = extractPhotoExtension(sourcePath.getFileName() == null ? "" : sourcePath.getFileName().toString());
        Path targetPath = ID_PHOTO_DIR.resolve(borrowerId + extension);
        Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        deleteGovernmentIdPhoto(existingPath, targetPath.toString());
        return targetPath.toString();
    }

    private String extractPhotoExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return ".png";
        }
        return fileName.substring(dotIndex).toLowerCase();
    }

    private void deleteGovernmentIdPhoto(String existingPath, String keepPath) {
        if (safe(existingPath).isEmpty()) {
            return;
        }
        Path existingFile = Paths.get(existingPath).toAbsolutePath().normalize();
        Path managedDirectory = ID_PHOTO_DIR.toAbsolutePath().normalize();
        if (!existingFile.startsWith(managedDirectory) || !Files.exists(existingFile)) {
            return;
        }
        if (!safe(keepPath).isEmpty() && existingFile.equals(Paths.get(keepPath).toAbsolutePath().normalize())) {
            return;
        }
        try {
            Files.deleteIfExists(existingFile);
        } catch (IOException ignored) {
        }
    }

    private void deleteGovernmentIdPhoto(String existingPath) {
        deleteGovernmentIdPhoto(existingPath, "");
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

        if (loans != null) {
            ArrayList<String> loanIdsToRemove = new ArrayList<>();
            loans.removeIf(loan -> {
                boolean matches = loan != null && borrowerId.equalsIgnoreCase(safe(loan.getBorrowerId()));
                if (matches) {
                    loanIdsToRemove.add(safe(loan.getId()));
                }
                return matches;
            });
            if (payments != null && !loanIdsToRemove.isEmpty()) {
                payments.removeIf(payment -> payment != null && loanIdsToRemove.contains(safe(payment.getLoanId())));
            }
        }
        borrowers.remove(existing);
        deleteGovernmentIdPhoto(existing.getGovernmentIdPhotoPath());
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
        return UITheme.formatCurrency(amount);
    }

}
