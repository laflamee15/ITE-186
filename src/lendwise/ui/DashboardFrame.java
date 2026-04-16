package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.GmailReminderService;
import lendwise.services.LoanCalculator;
import lendwise.services.OverdueReminderService;
import lendwise.utils.AppDataStore;

public class DashboardFrame extends JFrame {
    private static final Color SIDEBAR_BG_DARK = new Color(0x10, 0x16, 0x24);
    private static final Color SIDEBAR_BG_LIGHT = new Color(0xE7, 0xEE, 0xFA);
    private static final Color SIDEBAR_ACTIVE_DARK = new Color(0x1F, 0x3A, 0x5F);
    private static final Color SIDEBAR_ACTIVE_LIGHT = new Color(0x35, 0x6A, 0xE6);
    private static final Color SIDEBAR_BUTTON_LIGHT = new Color(0x35, 0x6A, 0xE6);
    private static final Color SIDEBAR_BUTTON_DARK = new Color(0x2A, 0x34, 0x47);
    private static final Color BRAND_BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color SIDEBAR_MUTED_DARK = new Color(0x7B, 0x85, 0x98);
    private static final Color SIDEBAR_MUTED_LIGHT = new Color(0x4E, 0x63, 0x85);
    private static final Color SIDEBAR_TEXT_DARK = new Color(0xF0, 0xF2, 0xF5);
    private static final Color SIDEBAR_TEXT_LIGHT = new Color(0x12, 0x2B, 0x45);

    private final ArrayList<Borrower> borrowers = new ArrayList<>();
    private final ArrayList<Loan> loans = new ArrayList<>();
    private final ArrayList<Payment> payments = new ArrayList<>();
    private final LoanCalculator loanCalculator = new LoanCalculator();
    private final AppDataStore appDataStore;
    private final GmailReminderService gmailReminderService = new GmailReminderService();
    private final OverdueReminderService overdueReminderService = new OverdueReminderService();
    private final String username;
    private final String accountEmail;
    private String currentView = "Dashboard";
    private boolean overdueReminderPromptShown;
    private boolean autoReminderCheckInProgress;
    private final Set<String> remindedLoanKeysThisSession = new LinkedHashSet<>();

    public DashboardFrame(String username, String accountEmail) {
        this.username = username == null || username.trim().isEmpty() ? "user" : username.trim();
        this.accountEmail = accountEmail == null ? "" : accountEmail.trim();
        this.appDataStore = new AppDataStore(this.accountEmail);
        setTitle("LendWise - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        UITheme.installTableDefaults();
        appDataStore.loadInto(borrowers, loans, payments);
        setContentPane(buildContent());
        javax.swing.SwingUtilities.invokeLater(this::promptForOverdueReminders);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.setBackground(UITheme.BG);

        RoundedPanel shell = new RoundedPanel(48, UITheme.CARD);
        shell.setBorderColor(UITheme.BORDER);
        shell.setBorderWidth(1);
        shell.setLayout(new BorderLayout(10, 10));
        shell.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = buildWelcomeHeader();

        JButton themeToggleButton = new JButton(UITheme.isLightMode() ? "Dark Mode" : "Light Mode");
        UITheme.applyHeaderActionButton(themeToggleButton, false);
        themeToggleButton.addActionListener(e -> {
            UITheme.toggleMode();
            setContentPane(buildContent());
            revalidate();
            repaint();
        });

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(themeToggleButton);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(headerActions, BorderLayout.EAST);

        final Runnable[] borrowerSaveRef = new Runnable[1];
        final Runnable[] loanSaveRef = new Runnable[1];
        final Runnable[] paymentSaveRef = new Runnable[1];
        final Runnable[] transactionSaveRef = new Runnable[1];
        Runnable borrowerSaveAction = () -> {
            if (borrowerSaveRef[0] != null) {
                borrowerSaveRef[0].run();
            }
        };
        Runnable loanSaveAction = () -> {
            if (loanSaveRef[0] != null) {
                loanSaveRef[0].run();
            }
        };
        Runnable paymentSaveAction = () -> {
            if (paymentSaveRef[0] != null) {
                paymentSaveRef[0].run();
            }
        };
        Runnable transactionSaveAction = () -> {
            if (transactionSaveRef[0] != null) {
                transactionSaveRef[0].run();
            }
        };

        BorrowerPanel borrowerPanel = new BorrowerPanel(borrowers, loans, payments, loanCalculator, username, borrowerSaveAction);
        LoanPanel loanPanel = new LoanPanel(
            borrowers,
            loans,
            payments,
            loanCalculator,
            username,
            loanSaveAction,
            this::sendOverdueReminders
        );
        PaymentPanel paymentPanel = new PaymentPanel(borrowers, payments, loans, loanCalculator, paymentSaveAction);
        DashboardPanel dashboardPanel = new DashboardPanel(borrowers, loans, payments, loanCalculator, username);
        AdminTransactionHistoryPanel transactionHistoryPanel = new AdminTransactionHistoryPanel(
            borrowers,
            loans,
            payments,
            loanCalculator,
            transactionSaveAction
        );
        Runnable refreshAll = () -> {
            appDataStore.save(borrowers, loans, payments);
            borrowerPanel.refreshTable();
            loanPanel.refreshTable();
            paymentPanel.refreshTable();
            dashboardPanel.refresh();
            transactionHistoryPanel.refresh();
            refreshOverdueReminderPromptState();
        };
        borrowerSaveRef[0] = refreshAll;
        loanSaveRef[0] = () -> {
            refreshAll.run();
            processAutomaticReminders(false, true);
        };
        paymentSaveRef[0] = () -> {
            refreshAll.run();
            processAutomaticReminders(false, true);
        };
        transactionSaveRef[0] = refreshAll;
        borrowerPanel.refreshTable();
        loanPanel.refreshTable();
        paymentPanel.refreshTable();
        dashboardPanel.refresh();
        transactionHistoryPanel.refresh();

        RoundedPanel borrowerCard = new RoundedPanel(40, UITheme.CARD);
        borrowerCard.setBorderColor(UITheme.BORDER);
        borrowerCard.setBorderWidth(1);
        borrowerCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(borrowerCard);
        borrowerCard.add(borrowerPanel, BorderLayout.CENTER);

        RoundedPanel dashboardCard = new RoundedPanel(40, UITheme.CARD);
        dashboardCard.setBorderColor(UITheme.BORDER);
        dashboardCard.setBorderWidth(1);
        dashboardCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(dashboardCard);
        dashboardCard.add(dashboardPanel, BorderLayout.CENTER);

        RoundedPanel loanCard = new RoundedPanel(40, UITheme.CARD);
        loanCard.setBorderColor(UITheme.BORDER);
        loanCard.setBorderWidth(1);
        loanCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(loanCard);
        loanCard.add(loanPanel, BorderLayout.CENTER);

        RoundedPanel paymentCard = new RoundedPanel(40, UITheme.CARD);
        paymentCard.setBorderColor(UITheme.BORDER);
        paymentCard.setBorderWidth(1);
        paymentCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(paymentCard);
        paymentCard.add(paymentPanel, BorderLayout.CENTER);

        RoundedPanel transactionHistoryCard = new RoundedPanel(40, UITheme.CARD);
        transactionHistoryCard.setBorderColor(UITheme.BORDER);
        transactionHistoryCard.setBorderWidth(1);
        transactionHistoryCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(transactionHistoryCard);
        transactionHistoryCard.add(transactionHistoryPanel, BorderLayout.CENTER);

        CardLayout contentLayout = new CardLayout();
        JPanel contentStack = new JPanel(contentLayout);
        contentStack.setOpaque(false);
        int maxContentWidth = 1280;
        contentStack.add(new CenteredWidthPanel(maxContentWidth, dashboardCard), "Dashboard");
        contentStack.add(new CenteredWidthPanel(maxContentWidth, borrowerCard), "Borrower");
        contentStack.add(new CenteredWidthPanel(maxContentWidth, loanCard), "Loans");
        contentStack.add(new CenteredWidthPanel(maxContentWidth, paymentCard), "Payments");
        contentStack.add(new CenteredWidthPanel(maxContentWidth, transactionHistoryCard), "Transactions");

        JButton dashboardButton = createSidebarButton("Dashboard", null);
        JButton borrowerButton = createSidebarButton("Borrowers", null);
        JButton loansButton = createSidebarButton("Loans", null);
        JButton paymentsButton = createSidebarButton("Payments", null);
        JButton transactionsButton = createSidebarButton("Transaction History", null);

        dashboardButton.addActionListener(e -> {
            currentView = "Dashboard";
            contentLayout.show(contentStack, "Dashboard");
            dashboardPanel.refresh();
            setSelectedSidebarButton(dashboardButton, borrowerButton, loansButton, paymentsButton, transactionsButton);
        });
        borrowerButton.addActionListener(e -> {
            currentView = "Borrower";
            contentLayout.show(contentStack, "Borrower");
            setSelectedSidebarButton(borrowerButton, dashboardButton, loansButton, paymentsButton, transactionsButton);
        });
        loansButton.addActionListener(e -> {
            currentView = "Loans";
            contentLayout.show(contentStack, "Loans");
            setSelectedSidebarButton(loansButton, dashboardButton, borrowerButton, paymentsButton, transactionsButton);
        });
        paymentsButton.addActionListener(e -> {
            currentView = "Payments";
            contentLayout.show(contentStack, "Payments");
            setSelectedSidebarButton(paymentsButton, dashboardButton, borrowerButton, loansButton, transactionsButton);
        });
        transactionsButton.addActionListener(e -> {
            currentView = "Transactions";
            contentLayout.show(contentStack, "Transactions");
            transactionHistoryPanel.refresh();
            setSelectedSidebarButton(transactionsButton, dashboardButton, borrowerButton, loansButton, paymentsButton);
        });

        JPanel sidebar = buildSidebar(dashboardButton, borrowerButton, loansButton, paymentsButton, transactionsButton);
        switch (currentView) {
            case "Borrower" -> {
                contentLayout.show(contentStack, "Borrower");
                setSelectedSidebarButton(borrowerButton, dashboardButton, loansButton, paymentsButton, transactionsButton);
            }
            case "Loans" -> {
                contentLayout.show(contentStack, "Loans");
                setSelectedSidebarButton(loansButton, dashboardButton, borrowerButton, paymentsButton, transactionsButton);
            }
            case "Payments" -> {
                contentLayout.show(contentStack, "Payments");
                setSelectedSidebarButton(paymentsButton, dashboardButton, borrowerButton, loansButton, transactionsButton);
            }
            case "Transactions" -> {
                contentLayout.show(contentStack, "Transactions");
                transactionHistoryPanel.refresh();
                setSelectedSidebarButton(transactionsButton, dashboardButton, borrowerButton, loansButton, paymentsButton);
            }
            default -> {
                currentView = "Dashboard";
                contentLayout.show(contentStack, "Dashboard");
                dashboardPanel.refresh();
                setSelectedSidebarButton(dashboardButton, borrowerButton, loansButton, paymentsButton, transactionsButton);
            }
        }

        JPanel contentArea = new JPanel(new BorderLayout(0, 10));
        contentArea.setOpaque(false);
        contentArea.add(headerRow, BorderLayout.NORTH);
        contentArea.add(contentStack, BorderLayout.CENTER);

        shell.add(sidebar, BorderLayout.WEST);
        shell.add(contentArea, BorderLayout.CENTER);

        root.add(shell, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildWelcomeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome,");
        Font welcomeFont = welcomeLabel.getFont();
        if (welcomeFont != null) {
            welcomeLabel.setFont(welcomeFont.deriveFont(Font.BOLD, 18f));
        }
        welcomeLabel.setForeground(UITheme.TEXT);

        RoundedPanel userBadge = new RoundedPanel(24, UITheme.isLightMode()
            ? new Color(0xE8, 0xF1, 0xFF)
            : new Color(0x1C, 0x24, 0x34));
        userBadge.setLayout(new BorderLayout());
        userBadge.setBorderColor(UITheme.isLightMode() ? new Color(0xC7, 0xDA, 0xFF) : new Color(0x2E, 0x3F, 0x5C));
        userBadge.setBorderWidth(1);
        userBadge.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));

        JLabel userLabel = new JLabel(username);
        userLabel.setFont(UITheme.UI_FONT_MEDIUM.deriveFont(16f));
        userLabel.setForeground(BRAND_BLUE);
        userBadge.add(userLabel, BorderLayout.CENTER);

        JLabel brandLabel = new JLabel("| LendWise");
        brandLabel.setFont(UITheme.UI_FONT.deriveFont(16f));
        brandLabel.setForeground(UITheme.TEXT_MUTED);

        header.add(welcomeLabel);
        header.add(userBadge);
        header.add(brandLabel);
        return header;
    }

    private JPanel buildSidebar(
            JButton dashboardButton,
            JButton borrowerButton,
            JButton loansButton,
            JButton paymentsButton,
            JButton transactionsButton
    ) {
        RoundedPanel sidebar = new RoundedPanel(30, sidebarBackground());
        sidebar.setBorderColor(UITheme.BORDER);
        sidebar.setBorderWidth(1);
        sidebar.setLayout(new BorderLayout(0, 20));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JPanel brand = new JPanel(new BorderLayout());
        brand.setOpaque(false);

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setOpaque(false);
        JLabel brandTitle = new JLabel("LendWise");
        brandTitle.setForeground(sidebarText());
        Font brandTitleFont = brandTitle.getFont();
        if (brandTitleFont != null) {
            brandTitle.setFont(brandTitleFont.deriveFont(Font.BOLD, 24f));
        }
        JLabel brandSubtitle = new JLabel("LENDING MANAGEMENT");
        brandSubtitle.setForeground(sidebarMuted());
        Font brandSubtitleFont = brandSubtitle.getFont();
        if (brandSubtitleFont != null) {
            brandSubtitle.setFont(brandSubtitleFont.deriveFont(12f));
        }

        brandText.add(brandTitle);
        brandText.add(brandSubtitle);
        brand.add(brandText, BorderLayout.CENTER);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        sections.add(createSidebarSectionLabel("OVERVIEW"));
        sections.add(Box.createVerticalStrut(10));

        dashboardButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        sections.add(dashboardButton);
        sections.add(Box.createVerticalStrut(24));

        sections.add(createSidebarSectionLabel("MANAGEMENT"));
        sections.add(Box.createVerticalStrut(10));

        borrowerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loansButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        transactionsButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        sections.add(borrowerButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(loansButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(paymentsButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(transactionsButton);

        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        nav.add(sections, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(0, 24));
        top.setOpaque(false);
        top.add(brand, BorderLayout.NORTH);
        top.add(nav, BorderLayout.CENTER);

        JButton signOutButton = createSignOutButton();
        signOutButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            dispose();
        });

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(signOutButton, BorderLayout.SOUTH);
        return sidebar;
    }

    private JLabel createSidebarSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(sidebarMuted());
        label.setFont(UITheme.metricLabelFont());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton createSidebarButton(String labelText, String badgeText) {
        JButton button = new JButton();
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(SIDEBAR_TEXT_DARK);
        button.setBackground(sidebarButtonNormal());
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setUI(new RoundedSidebarButtonUI(18));
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, sidebarButtonNormal(), false),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.putClientProperty("navLabel", labelText);
        button.putClientProperty("navBadgeText", badgeText);
        button.setText(buildSidebarButtonText(labelText, badgeText, false));
        return button;
    }

    private JButton createSignOutButton() {
        JButton button = new JButton("Sign out");
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setForeground(Color.WHITE);
        button.setBackground(sidebarButtonNormal());
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setUI(new RoundedSidebarButtonUI(18));
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, UITheme.isLightMode() ? BRAND_BLUE : sidebarButtonNormal(), false),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        button.setPreferredSize(new Dimension(0, 46));
        return button;
    }

    private void setSelectedSidebarButton(JButton selectedButton, JButton... otherButtons) {
        selectedButton.setBackground(sidebarActive());
        selectedButton.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(16, BRAND_BLUE, false),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        updateSidebarButtonColors(selectedButton, true);
        for (JButton button : otherButtons) {
            button.setBackground(sidebarButtonNormal());
            button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedButtonBorder(16, UITheme.isLightMode() ? BRAND_BLUE : sidebarButtonNormal(), false),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
            updateSidebarButtonColors(button, false);
        }
    }

    private void updateSidebarButtonColors(JButton button, boolean selected) {
        String labelText = (String) button.getClientProperty("navLabel");
        String badgeText = (String) button.getClientProperty("navBadgeText");
        button.setText(buildSidebarButtonText(labelText, badgeText, selected));
    }

    private String buildSidebarButtonText(String labelText, String badgeText, boolean selected) {
        String textColor = toHex(Color.WHITE);
        String badgeColor = toHex(selected ? Color.WHITE : new Color(0xC8, 0xD2, 0xE6));
        String badge = badgeText == null || badgeText.isEmpty() ? "" : badgeText;
        return "<html><table width='190' cellpadding='0' cellspacing='0'>"
            + "<tr>"
            + "<td><span style='color:" + textColor + "; font-weight:bold;'>" + safe(labelText) + "</span></td>"
            + "<td align='right'><span style='color:" + badgeColor + ";'>" + safe(badge) + "</span></td>"
            + "</tr>"
            + "</table></html>";
    }

    private Color sidebarButtonNormal() {
        return UITheme.isLightMode() ? SIDEBAR_BUTTON_LIGHT : SIDEBAR_BUTTON_DARK;
    }

    private Color sidebarBackground() {
        return UITheme.isLightMode() ? SIDEBAR_BG_LIGHT : SIDEBAR_BG_DARK;
    }

    private Color sidebarActive() {
        return UITheme.isLightMode() ? SIDEBAR_ACTIVE_LIGHT : SIDEBAR_ACTIVE_DARK;
    }

    private Color sidebarMuted() {
        return UITheme.isLightMode() ? SIDEBAR_MUTED_LIGHT : SIDEBAR_MUTED_DARK;
    }

    private Color sidebarText() {
        return UITheme.isLightMode() ? SIDEBAR_TEXT_LIGHT : SIDEBAR_TEXT_DARK;
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void promptForOverdueReminders() {
        if (overdueReminderPromptShown) {
            return;
        }
        overdueReminderPromptShown = true;
        int overdueBorrowers = overdueReminderService.countOverdueBorrowers(borrowers, loans);
        if (overdueBorrowers <= 0) {
            return;
        }
        processAutomaticReminders(true, false);
    }

    private void refreshOverdueReminderPromptState() {
        if (overdueReminderService.countOverdueBorrowers(borrowers, loans) <= 0) {
            overdueReminderPromptShown = false;
        }
    }

    private void sendOverdueReminders() {
        processAutomaticReminders(true, false);
    }

    private void processAutomaticReminders(boolean interactive, boolean onlyNewThisSession) {
        if (autoReminderCheckInProgress) {
            return;
        }
        autoReminderCheckInProgress = true;
        Set<String> targetLoanKeys = collectTargetOverdueLoanKeys(onlyNewThisSession);
        if (targetLoanKeys.isEmpty()) {
            autoReminderCheckInProgress = false;
            if (interactive) {
                JOptionPane.showMessageDialog(
                    this,
                    "No new overdue reminders needed to be sent.",
                    "Overdue Reminders",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            return;
        }
        GmailReminderService.ReminderResult result =
            gmailReminderService.sendSelectedOverdueReminders(borrowers, loans, targetLoanKeys);
        if (!result.isConfigured()) {
            autoReminderCheckInProgress = false;
            if (!interactive) {
                return;
            }
            JOptionPane.showMessageDialog(
                this,
                "Edit " + gmailReminderService.getMailConfig().getConfigPath()
                    + " and enter your sender Gmail and Gmail App Password first.",
                "Gmail Reminder Setup",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (result.sentCount() > 0) {
            remindedLoanKeysThisSession.addAll(targetLoanKeys);
        } else if (!result.errors().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                result.errors().get(0),
                "Overdue Reminders",
                JOptionPane.WARNING_MESSAGE
            );
        } else if (interactive) {
            JOptionPane.showMessageDialog(
                this,
                "No new overdue reminders needed to be sent.",
                "Overdue Reminders",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
        autoReminderCheckInProgress = false;
    }

    private Set<String> collectTargetOverdueLoanKeys(boolean onlyNewThisSession) {
        Set<String> targetLoanKeys = new LinkedHashSet<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }

            String status = safe(loan.getStatus()).trim().toUpperCase();
            java.time.LocalDate dueDate = null;
            if (loan.getStartDate() != null && loan.getOriginalTermMonths() > 0) {
                dueDate = loan.getStartDate().plusMonths(loan.getOriginalTermMonths());
            }

            boolean overdue = ("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today)))
                && !"PAID".equals(status);
            if (!overdue) {
                continue;
            }

            String loanKey = buildLoanKey(loan.getBorrowerId(), loan.getId());
            if (onlyNewThisSession && remindedLoanKeysThisSession.contains(loanKey)) {
                continue;
            }
            targetLoanKeys.add(loanKey);
        }
        return targetLoanKeys;
    }

    private String buildLoanKey(String borrowerId, String loanId) {
        return safe(borrowerId) + "::" + safe(loanId);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static final class RoundedSidebarButtonUI extends BasicButtonUI {
        private final int radius;

        private RoundedSidebarButtonUI(int radius) {
            this.radius = radius;
        }

        @Override
        public void update(Graphics g, javax.swing.JComponent c) {
            AbstractButton button = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(button.getBackground());
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius * 2, radius * 2);
            g2.dispose();
            paint(g, c);
        }
    }
}
