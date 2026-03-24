package lendwise.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import lendwise.utils.AccountStore;

public class LoginFrame extends JFrame {
    private static final String VIEW_SIGN_IN = "sign_in";
    private static final String VIEW_CREATE = "create";
    private static final Dimension SIGN_IN_CARD_SIZE = new Dimension(500, 575);
    private static final Dimension CREATE_CARD_SIZE = new Dimension(530, 760);

    private static final Color PAGE_BG = new Color(0x0F, 0x17, 0x2A);
    private static final Color CARD_BG = new Color(0x11, 0x1B, 0x3A);
    private static final Color FIELD_BG = new Color(0x1A, 0x28, 0x48);
    private static final Color BORDER = new Color(0x2E, 0x5C, 0xB8);
    private static final Color TEXT = new Color(0xE8, 0xEC, 0xF4);
    private static final Color MUTED = new Color(0x8C, 0x97, 0xAA);
    private static final Color PRIMARY_BLUE = new Color(0x2F, 0x6E, 0xF0);
    private static final Color ACCENT_GREEN = new Color(0x10, 0xB9, 0x81);

    private final AccountStore accountStore = new AccountStore();

    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewPanel = new JPanel(viewLayout);
    private final RoundedPanel cardPanel = new RoundedPanel(34, CARD_BG);

    private final JButton signInTabButton = new JButton("Sign in");
    private final JButton createAccountTabButton = new JButton("Create Account");

    private final JTextField signInEmailField = new JTextField(22);
    private final JPasswordField signInPasswordField = new JPasswordField(22);
    private final JCheckBox signInShowPasswordCheck = new JCheckBox("Show password");
    private final JButton signInButton = new JButton("SIGN IN  >");

    private final JTextField createNameField = new JTextField(22);
    private final JTextField createEmailField = new JTextField(22);
    private final JPasswordField createPasswordField = new JPasswordField(22);
    private final JPasswordField createConfirmPasswordField = new JPasswordField(22);
    private final JCheckBox createShowPasswordCheck = new JCheckBox("Show passwords");
    private final JButton createAccountButton = new JButton("CREATE ACCOUNT  >");

    private boolean signInPasswordVisible;
    private boolean createPasswordVisible;

    public LoginFrame() {
        setTitle("LendWise - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        trySetLookAndFeel();
        UITheme.installTableDefaults();
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setContentPane(buildContent());
        switchView(VIEW_SIGN_IN);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                validate();
                repaint();
                signInEmailField.requestFocusInWindow();
            }
        });
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(PAGE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        cardPanel.setBorderColor(BORDER);
        cardPanel.setBorderWidth(1);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(SIGN_IN_CARD_SIZE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(24, 30, 30, 30));

        JLabel badge = new JLabel("LW  SMART LENDING PORTAL", SwingConstants.CENTER);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        badge.setForeground(MUTED);
        badge.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, BORDER, false),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        JPanel tabRow = new JPanel(new GridLayout(1, 2, 10, 0));
        tabRow.setOpaque(false);
        tabRow.setMaximumSize(new Dimension(440, 34));
        tabRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        signInTabButton.addActionListener(e -> switchView(VIEW_SIGN_IN));
        createAccountTabButton.addActionListener(e -> switchView(VIEW_CREATE));
        tabRow.add(signInTabButton);
        tabRow.add(createAccountTabButton);

        viewPanel.setOpaque(false);
        viewPanel.add(buildSignInView(), VIEW_SIGN_IN);
        viewPanel.add(buildCreateView(), VIEW_CREATE);

        cardPanel.add(badge);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(tabRow);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(viewPanel);

        root.add(cardPanel);
        return root;
    }

    private JPanel buildSignInView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = createTitle("Sign in to LendWise");
        JLabel subtitle = createSubtitle("Administrator access only. Session stored in your browser.");

        styleInput(signInEmailField);
        stylePasswordInput(signInPasswordField);
        signInShowPasswordCheck.setOpaque(false);
        signInShowPasswordCheck.setForeground(MUTED);
        signInShowPasswordCheck.addActionListener(e -> {
            signInPasswordVisible = signInShowPasswordCheck.isSelected();
            signInPasswordField.setEchoChar(signInPasswordVisible ? (char) 0 : '\u2022');
        });

        JPanel passwordFieldPanel = wrapCentered(signInPasswordField);
        JPanel checkboxPanel = wrapCentered(signInShowPasswordCheck);

        JLabel helper = new JLabel(
            "<html><div style='text-align:center;'>Don't have an account yet?<br>Create one using the tab above.</div></html>",
            SwingConstants.CENTER
        );
        helper.setAlignmentX(Component.CENTER_ALIGNMENT);
        helper.setForeground(MUTED);
        helper.setMaximumSize(new Dimension(280, 40));

        styleGradientButton(signInButton, new Dimension(150, 38));
        signInButton.addActionListener(e -> handleLogin());

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(6));
        panel.add(createFieldLabel("Email"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(wrapInputWithRoundedBorder(signInEmailField));
        panel.add(Box.createVerticalStrut(2));
        panel.add(createFieldLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(wrapPasswordWithRoundedBorder(signInPasswordField));
        panel.add(Box.createVerticalStrut(0));
        panel.add(checkboxPanel);
        panel.add(Box.createVerticalStrut(1));
        panel.add(wrapCentered(helper));
        panel.add(Box.createVerticalStrut(2));
        panel.add(wrapCentered(signInButton));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildCreateView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = createTitle("Create Your Account");
        JLabel subtitle = createSubtitle("Set up your admin account to manage loans and borrowers.");

        styleInput(createNameField);
        styleInput(createEmailField);
        stylePasswordInput(createPasswordField);
        stylePasswordInput(createConfirmPasswordField);

        createShowPasswordCheck.setOpaque(false);
        createShowPasswordCheck.setForeground(MUTED);
        createShowPasswordCheck.addActionListener(e -> {
            createPasswordVisible = createShowPasswordCheck.isSelected();
            char echo = createPasswordVisible ? (char) 0 : '\u2022';
            createPasswordField.setEchoChar(echo);
            createConfirmPasswordField.setEchoChar(echo);
        });

        JLabel helper = new JLabel(
            "<html><div style='text-align:center;'>Create your admin account<br>and start managing loans today.</div></html>",
            SwingConstants.CENTER
        );
        helper.setAlignmentX(Component.CENTER_ALIGNMENT);
        helper.setForeground(MUTED);
        helper.setMaximumSize(new Dimension(300, 44));

        styleGradientButton(createAccountButton, new Dimension(210, 38));
        createAccountButton.addActionListener(e -> handleCreateAccount());

        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(18));
        panel.add(createFieldLabel("Full name"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(wrapInputWithRoundedBorder(createNameField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createFieldLabel("Email address"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(wrapInputWithRoundedBorder(createEmailField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createFieldLabel("Password"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(wrapPasswordWithRoundedBorder(createPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createFieldLabel("Confirm password"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(wrapPasswordWithRoundedBorder(createConfirmPasswordField));
        panel.add(Box.createVerticalStrut(6));
        panel.add(wrapCentered(createShowPasswordCheck));
        panel.add(Box.createVerticalStrut(18));
        panel.add(wrapCentered(helper));
        panel.add(Box.createVerticalStrut(10));
        panel.add(wrapCentered(createAccountButton));

        return panel;
    }

    private void switchView(String view) {
        boolean createView = VIEW_CREATE.equals(view);
        if (createView) {
            styleGhostButton(signInTabButton, new Dimension(0, 34));
            styleGradientButton(createAccountTabButton, new Dimension(0, 34));
            cardPanel.setPreferredSize(CREATE_CARD_SIZE);
            createNameField.requestFocusInWindow();
        } else {
            styleGradientButton(signInTabButton, new Dimension(0, 34));
            styleGhostButton(createAccountTabButton, new Dimension(0, 34));
            cardPanel.setPreferredSize(SIGN_IN_CARD_SIZE);
            signInEmailField.requestFocusInWindow();
        }
        viewLayout.show(viewPanel, view);
        revalidate();
        repaint();
    }

    private JLabel createTitle(String text) {
        JLabel title = new JLabel(text, SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }
        return title;
    }

    private JLabel createSubtitle(String text) {
        JLabel subtitle = new JLabel(text, SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(MUTED);
        return subtitle;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(MUTED);
        label.setMaximumSize(new Dimension(440, 18));
        return label;
    }

    private JPanel wrapCentered(Component component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setOpaque(false);
        panel.add(component);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private JPanel wrapInputWithRoundedBorder(JTextField field) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        RoundedPanel rounded = new RoundedPanel(20, FIELD_BG);
        rounded.setBorderColor(BORDER);
        rounded.setBorderWidth(1);
        rounded.setLayout(new java.awt.BorderLayout());
        rounded.setMaximumSize(new Dimension(440, 44));
        rounded.setPreferredSize(new Dimension(440, 44));
        rounded.add(field, java.awt.BorderLayout.CENTER);
        wrapper.add(rounded);
        return wrapCentered(wrapper);
    }

    private JPanel wrapPasswordWithRoundedBorder(JPasswordField field) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        RoundedPanel rounded = new RoundedPanel(20, FIELD_BG);
        rounded.setBorderColor(BORDER);
        rounded.setBorderWidth(1);
        rounded.setLayout(new java.awt.BorderLayout());
        rounded.setMaximumSize(new Dimension(440, 44));
        rounded.setPreferredSize(new Dimension(440, 44));
        rounded.add(field, java.awt.BorderLayout.CENTER);
        wrapper.add(rounded);
        return wrapCentered(wrapper);
    }

    private void styleInput(JTextField field) {
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(0x2A, 0x67, 0xF0));
        field.setSelectedTextColor(Color.WHITE);
        field.setMaximumSize(new Dimension(440, 44));
        field.setPreferredSize(new Dimension(440, 44));
        field.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }

    private void stylePasswordInput(JPasswordField field) {
        field.setEchoChar('\u2022');
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(0x2A, 0x67, 0xF0));
        field.setSelectedTextColor(Color.WHITE);
        field.setMaximumSize(new Dimension(440, 44));
        field.setPreferredSize(new Dimension(440, 44));
        field.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }

    private void styleGradientButton(JButton button, Dimension size) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_BLUE);
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.GradientPaint gradient = new java.awt.GradientPaint(
                    0, 0, PRIMARY_BLUE,
                    c.getWidth(), 0, ACCENT_GREEN
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String text = c instanceof javax.swing.JButton ? ((javax.swing.JButton) c).getText() : "";
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, x, y);
            }
        });
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
    }

    private void styleGhostButton(JButton button, Dimension size) {
        button.setFocusPainted(false);
        button.setForeground(MUTED);
        button.setBackground(FIELD_BG);
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 16, 16);
                g2.setColor(MUTED);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String text = c instanceof javax.swing.JButton ? ((javax.swing.JButton) c).getText() : "";
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, x, y);
            }
        });
        button.setBorder(new RoundedButtonBorder(16, BORDER, false));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
    }

    private void handleCreateAccount() {
        String fullName = createNameField.getText().trim();
        String email = createEmailField.getText().trim();
        String password = new String(createPasswordField.getPassword()).trim();
        String confirmPassword = new String(createConfirmPasswordField.getPassword()).trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all create account fields.", "Create Account", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Create Account", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Create Account", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (accountStore.accountExists(email)) {
            JOptionPane.showMessageDialog(this, "That account already exists. You can sign in now.", "Create Account", JOptionPane.INFORMATION_MESSAGE);
            switchView(VIEW_SIGN_IN);
            signInEmailField.setText(email);
            return;
        }
        if (!accountStore.createAccount(fullName, email, password)) {
            JOptionPane.showMessageDialog(this, "Account could not be created.", "Create Account", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Account created successfully.", "Create Account", JOptionPane.INFORMATION_MESSAGE);
        signInEmailField.setText(email);
        signInPasswordField.setText("");
        createNameField.setText("");
        createEmailField.setText("");
        createPasswordField.setText("");
        createConfirmPasswordField.setText("");
        switchView(VIEW_SIGN_IN);
    }

    private void handleLogin() {
        String email = signInEmailField.getText().trim();
        String password = new String(signInPasswordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your email and password.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!accountStore.authenticate(email, password)) {
            JOptionPane.showMessageDialog(this, "Invalid email or password.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String displayName = accountStore.getDisplayName(email);
        DashboardFrame dashboard = new DashboardFrame(displayName);
        dashboard.setVisible(true);
        dispose();
    }

    private void trySetLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (javax.swing.UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
        }
    }
}
