package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class DashboardFrame extends JFrame {
    private final ArrayList<Borrower> borrowers = new ArrayList<>();
    private final ArrayList<Loan> loans = new ArrayList<>();
    private final ArrayList<Payment> payments = new ArrayList<>();
    private final LoanCalculator loanCalculator = new LoanCalculator();
    private final JLabel loanStatusLabel = new JLabel("Loan Status: (select a loan)", SwingConstants.LEFT);

    public DashboardFrame(String username) {
        setTitle("LendWise - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        seedSampleData();
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent(username));
    }

    private JPanel buildContent(String username) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(UITheme.BG);

        JLabel header = new JLabel("Welcome, " + username + "  |  LendWise", SwingConstants.LEFT);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.setFont(header.getFont().deriveFont(18f));
        header.setForeground(UITheme.TEXT);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBackground(UITheme.BG);
        tabs.setForeground(UITheme.TEXT);

        BorrowerPanel borrowerPanel = new BorrowerPanel(borrowers);
        LoanPanel loanPanel = new LoanPanel(loans, loanCalculator);
        PaymentPanel paymentPanel = new PaymentPanel(payments, loans, loanCalculator);

        loanPanel.setSelectionListener(selected -> {
            if (selected == null) {
                loanStatusLabel.setText("Loan Status: (select a loan)");
                return;
            }
            String status = selected.getStatus() == null ? "" : selected.getStatus().trim();
            if (status.isEmpty()) status = "(unknown)";
            loanStatusLabel.setText("Loan Status: " + status + "  |  Loan ID: " + safe(selected.getId()));
        });

        RoundedPanel borrowerCard = new RoundedPanel(20, UITheme.CARD);
        borrowerCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(borrowerCard);
        borrowerCard.add(borrowerPanel, BorderLayout.CENTER);

        RoundedPanel loanCard = new RoundedPanel(20, UITheme.CARD);
        loanCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(loanCard);
        loanCard.add(loanPanel, BorderLayout.CENTER);

        RoundedPanel paymentCard = new RoundedPanel(20, UITheme.CARD);
        paymentCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(paymentCard);
        paymentCard.add(paymentPanel, BorderLayout.CENTER);

        tabs.addTab("Borrowers", borrowerCard);
        tabs.addTab("Loans", loanCard);
        tabs.addTab("Payments", paymentCard);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 8, 8, 8),
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER)
        ));
        statusBar.setBackground(UITheme.CARD);
        statusBar.add(loanStatusLabel, BorderLayout.WEST);
        loanStatusLabel.setForeground(UITheme.TEXT_MUTED);

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        return root;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(UITheme.CARD);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));

        JMenu file = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        file.setForeground(UITheme.TEXT);
        exit.setForeground(UITheme.TEXT);
        exit.addActionListener(e -> dispose());
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        help.setForeground(UITheme.TEXT);
        about.setForeground(UITheme.TEXT);
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "LendWise - Lending Management System\nDark theme, modern UI.",
            "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private void seedSampleData() {
        borrowers.clear();
        loans.clear();
        payments.clear();
        borrowers.add(new Borrower("B-001", "Juan Dela Cruz", "0917-000-0001", "Sample Address 1"));
        borrowers.add(new Borrower("B-002", "Maria Santos", "0917-000-0002", "Sample Address 2"));
        loans.add(new Loan("L-1001", "B-001", 10000.00, 12.0, 6, LocalDate.now().minusDays(10), "ACTIVE"));
        loans.add(new Loan("L-1002", "B-002", 5000.00, 10.0, 3, LocalDate.now().minusDays(40), "PENDING"));
        payments.add(new Payment("P-9001", "L-1001", 1000.00, LocalDate.now().minusDays(2), "CASH"));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
