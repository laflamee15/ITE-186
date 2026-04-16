package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;
import lendwise.utils.AppDataStore;

public class BorrowerDashboardFrame extends JFrame {
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
    private final String username;
    private final String accountEmail;
    private String currentView = "ACCOUNT";

    public BorrowerDashboardFrame(String username, String accountEmail) {
        this.username = username == null || username.trim().isEmpty() ? "borrower" : username.trim();
        this.accountEmail = accountEmail == null ? "" : accountEmail.trim();
        this.appDataStore = new AppDataStore(this.accountEmail);
        setTitle("LendWise - Borrower Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        UITheme.installTableDefaults();
        appDataStore.loadBorrowerViewInto(this.accountEmail, borrowers, loans, payments);
        setContentPane(buildContent());
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

        JPanel headerRow = buildHeaderRow();
        UserPanel accountPanel = new UserPanel(
            borrowers, loans, payments, loanCalculator, username, accountEmail, UserPanel.ViewMode.ACCOUNT
        );
        UserPanel loansPanel = new UserPanel(
            borrowers, loans, payments, loanCalculator, username, accountEmail, UserPanel.ViewMode.LOANS
        );
        UserPanel paymentsPanel = new UserPanel(
            borrowers, loans, payments, loanCalculator, username, accountEmail, UserPanel.ViewMode.PAYMENTS
        );
        UserPanel timelinePanel = new UserPanel(
            borrowers, loans, payments, loanCalculator, username, accountEmail, UserPanel.ViewMode.TIMELINE
        );
        accountPanel.refresh();
        loansPanel.refresh();
        paymentsPanel.refresh();
        timelinePanel.refresh();

        RoundedPanel accountCard = createUserCard(accountPanel);
        RoundedPanel loansCard = createUserCard(loansPanel);
        RoundedPanel paymentsCard = createUserCard(paymentsPanel);
        RoundedPanel timelineCard = createUserCard(timelinePanel);

        CardLayout contentLayout = new CardLayout();
        JPanel contentStack = new JPanel(contentLayout);
        contentStack.setOpaque(false);
        contentStack.add(new CenteredWidthPanel(1280, accountCard), "ACCOUNT");
        contentStack.add(new CenteredWidthPanel(1280, loansCard), "LOANS");
        contentStack.add(new CenteredWidthPanel(1280, paymentsCard), "PAYMENTS");
        contentStack.add(new CenteredWidthPanel(1280, timelineCard), "TIMELINE");

        JPanel contentArea = new JPanel(new BorderLayout(0, 10));
        contentArea.setOpaque(false);
        contentArea.add(headerRow, BorderLayout.NORTH);
        contentArea.add(contentStack, BorderLayout.CENTER);

        JButton accountButton = createSidebarButton("My Account");
        JButton loansButton = createSidebarButton("Loan Overview");
        JButton paymentsButton = createSidebarButton("Payment History");
        JButton timelineButton = createSidebarButton("Account Timeline");

        accountButton.addActionListener(e -> {
            currentView = "ACCOUNT";
            accountPanel.refresh();
            contentLayout.show(contentStack, "ACCOUNT");
            setSelectedSidebarButton(accountButton, loansButton, paymentsButton, timelineButton);
        });
        loansButton.addActionListener(e -> {
            currentView = "LOANS";
            loansPanel.refresh();
            contentLayout.show(contentStack, "LOANS");
            setSelectedSidebarButton(loansButton, accountButton, paymentsButton, timelineButton);
        });
        paymentsButton.addActionListener(e -> {
            currentView = "PAYMENTS";
            paymentsPanel.refresh();
            contentLayout.show(contentStack, "PAYMENTS");
            setSelectedSidebarButton(paymentsButton, accountButton, loansButton, timelineButton);
        });
        timelineButton.addActionListener(e -> {
            currentView = "TIMELINE";
            timelinePanel.refresh();
            contentLayout.show(contentStack, "TIMELINE");
            setSelectedSidebarButton(timelineButton, accountButton, loansButton, paymentsButton);
        });

        switch (currentView) {
            case "LOANS" -> {
                contentLayout.show(contentStack, "LOANS");
                setSelectedSidebarButton(loansButton, accountButton, paymentsButton, timelineButton);
            }
            case "PAYMENTS" -> {
                contentLayout.show(contentStack, "PAYMENTS");
                setSelectedSidebarButton(paymentsButton, accountButton, loansButton, timelineButton);
            }
            case "TIMELINE" -> {
                contentLayout.show(contentStack, "TIMELINE");
                setSelectedSidebarButton(timelineButton, accountButton, loansButton, paymentsButton);
            }
            default -> {
                currentView = "ACCOUNT";
                contentLayout.show(contentStack, "ACCOUNT");
                setSelectedSidebarButton(accountButton, loansButton, paymentsButton, timelineButton);
            }
        }

        shell.add(buildSidebar(accountButton, loansButton, paymentsButton, timelineButton), BorderLayout.WEST);
        shell.add(contentArea, BorderLayout.CENTER);

        root.add(shell, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeaderRow() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(buildWelcomeHeader(), BorderLayout.WEST);

        JButton themeToggleButton = new JButton(UITheme.isLightMode() ? "Dark Mode" : "Light Mode");
        UITheme.applyHeaderActionButton(themeToggleButton, false);
        themeToggleButton.addActionListener(e -> {
            UITheme.toggleMode();
            setContentPane(buildContent());
            revalidate();
            repaint();
        });
        header.add(themeToggleButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildWelcomeHeader() {
        JPanel header = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome,");
        Font baseFont = welcomeLabel.getFont();
        if (baseFont != null) {
            welcomeLabel.setFont(baseFont.deriveFont(Font.BOLD, 18f));
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
        Font userFont = userLabel.getFont();
        if (userFont != null) {
            userLabel.setFont(userFont.deriveFont(Font.BOLD, 16f));
        }
        userLabel.setForeground(BRAND_BLUE);
        userBadge.add(userLabel, BorderLayout.CENTER);

        JLabel brandLabel = new JLabel("| Borrower Portal");
        brandLabel.setForeground(UITheme.TEXT_MUTED);

        header.add(welcomeLabel);
        header.add(userBadge);
        header.add(brandLabel);
        return header;
    }

    private RoundedPanel createUserCard(UserPanel panel) {
        RoundedPanel userCard = new RoundedPanel(40, UITheme.CARD);
        userCard.setBorderColor(UITheme.BORDER);
        userCard.setBorderWidth(1);
        userCard.setLayout(new BorderLayout());
        UITheme.applyCardPadding(userCard);
        userCard.add(panel, BorderLayout.CENTER);
        return userCard;
    }

    private JPanel buildSidebar(JButton accountButton, JButton loansButton, JButton paymentsButton, JButton timelineButton) {
        RoundedPanel sidebar = new RoundedPanel(30, sidebarBackground());
        sidebar.setBorderColor(UITheme.BORDER);
        sidebar.setBorderWidth(1);
        sidebar.setLayout(new BorderLayout(0, 20));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JPanel top = new JPanel(new BorderLayout(0, 24));
        top.setOpaque(false);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel brandTitle = new JLabel("LendWise");
        brandTitle.setForeground(sidebarText());
        Font brandFont = brandTitle.getFont();
        if (brandFont != null) {
            brandTitle.setFont(brandFont.deriveFont(Font.BOLD, 24f));
        }

        JLabel brandSubtitle = new JLabel("BORROWER PORTAL");
        brandSubtitle.setForeground(sidebarMuted());
        brandText.add(brandTitle);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(brandSubtitle);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.add(createSidebarSectionLabel("YOUR VIEW"));
        sections.add(Box.createVerticalStrut(10));
        sections.add(accountButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(loansButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(paymentsButton);
        sections.add(Box.createVerticalStrut(10));
        sections.add(timelineButton);

        top.add(brandText, BorderLayout.NORTH);
        top.add(sections, BorderLayout.CENTER);

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
        Font font = label.getFont();
        if (font != null) {
            label.setFont(font.deriveFont(Font.BOLD, 13f));
        }
        return label;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        button.setForeground(Color.WHITE);
        button.setBackground(sidebarButtonNormal());
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setUI(new RoundedSidebarButtonUI(18));
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, BRAND_BLUE, false),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return button;
    }

    private void setSelectedSidebarButton(JButton selected, JButton... others) {
        selected.setBackground(sidebarActive());
        for (JButton other : others) {
            if (other != null) {
                other.setBackground(sidebarButtonNormal());
            }
        }
    }

    private JButton createSignOutButton() {
        JButton button = new JButton("Sign out");
        button.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
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
