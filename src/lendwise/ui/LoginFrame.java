package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicButtonUI;
import lendwise.services.PasswordResetService;
import lendwise.utils.AccountStore;

public class LoginFrame extends JFrame {
    private static final String VIEW_SIGN_IN = "sign_in";
    private static final String VIEW_CREATE = "create";
    private static final Dimension SIGN_IN_CARD_SIZE = new Dimension(500, 605);
    private static final Dimension CREATE_CARD_SIZE = new Dimension(530, 820);

    private static final Color PAGE_BG = new Color(0x0F, 0x17, 0x2A);
    private static final Color CARD_BG = new Color(0x11, 0x1B, 0x3A);
    private static final Color FIELD_BG = new Color(0x1A, 0x28, 0x48);
    private static final Color BORDER = new Color(0x2E, 0x5C, 0xB8);
    private static final Color TEXT = new Color(0xE8, 0xEC, 0xF4);
    private static final Color MUTED = new Color(0x8C, 0x97, 0xAA);
    private static final Color PRIMARY_BLUE = new Color(0x2F, 0x6E, 0xF0);
    private static final Color ACCENT_GREEN = new Color(0x10, 0xB9, 0x81);
    private static final Color HERO_CARD = new Color(0x131E35);
    private static final Color HERO_MUTED = new Color(0x7E, 0x8D, 0xA8);
    private static final Color HERO_BLUE = new Color(0x4F, 0x86, 0xF7);
    private static final Color HERO_GREEN = new Color(0x2E, 0xB8, 0x5B);
    private static final Color HERO_ORANGE = new Color(0xE7, 0x7A, 0x1B);
    private static final Color HERO_RED = new Color(0xCF, 0x6C, 0x74);
    private static final String HERO_IMAGE_PATH = "C:\\Users\\user\\Pictures\\Screenshots\\Screenshot 2026-04-01 041738.png";
    private static final Color ROLE_BUTTON_FILL = new Color(0x162645);
    private static final Color ROLE_BUTTON_HOVER = new Color(0x1D, 0x31, 0x57);
    private static final Color ROLE_BUTTON_ACTIVE = new Color(0x2F, 0x6E, 0xF0);
    private static final Font UI_FONT = UITheme.createFont(Font.PLAIN, 15f);
    private static final Font UI_FONT_MEDIUM = UITheme.createFont(Font.BOLD, 15f);
    private static final Font UI_FONT_SMALL = UITheme.createFont(Font.PLAIN, 12.5f);
    private static final Font UI_FONT_TITLE = UITheme.createFont(Font.BOLD, 30f);
    private static final Font UI_FONT_HERO_TITLE = UITheme.createFont(Font.BOLD, 42f);
    private static final Font UI_FONT_HERO_SECTION = UITheme.createFont(Font.BOLD, 11.5f);
    private static final Font UI_FONT_HERO_BODY = UITheme.createFont(Font.PLAIN, 19f);
    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewPanel = new JPanel(viewLayout);
    private final RoundedPanel cardPanel = new RoundedPanel(34, CARD_BG);
    private final AccountStore accountStore = new AccountStore();
    private final PasswordResetService passwordResetService = new PasswordResetService(accountStore);
    private final JLabel brandBadge = new JLabel("LW  SMART LENDING PORTAL", SwingConstants.CENTER);
    private final JPanel tabRow = new JPanel(new GridLayout(1, 2, 10, 0));

    private final JButton signInTabButton = new JButton("Sign in");
    private final JButton createAccountTabButton = new JButton("Create Account");

    private final JTextField signInEmailField = new JTextField(22);
    private final JPasswordField signInPasswordField = new JPasswordField(22);
    private final JComboBox<String> signInRoleCombo = new JComboBox<>(new String[]{"Lender", "Borrower"});
    private final JCheckBox signInShowPasswordCheck = new JCheckBox("Show password");
    private final JButton signInButton = new JButton("SIGN IN");
    private final JButton forgotPasswordButton = new JButton("Forgot password?");
    private final JToggleButton signInLenderButton = new JToggleButton("Lender");
    private final JToggleButton signInBorrowerButton = new JToggleButton("Borrower");

    private final JTextField createNameField = new JTextField(22);
    private final JTextField createEmailField = new JTextField(22);
    private final JPasswordField createPasswordField = new JPasswordField(22);
    private final JPasswordField createConfirmPasswordField = new JPasswordField(22);
    private final JComboBox<String> createRoleCombo = new JComboBox<>(new String[]{"Lender", "Borrower"});
    private final JCheckBox createShowPasswordCheck = new JCheckBox("Show passwords");
    private final JButton createAccountButton = new JButton("CREATE ACCOUNT  >");
    private final JToggleButton createLenderButton = new JToggleButton("Lender");
    private final JToggleButton createBorrowerButton = new JToggleButton("Borrower");

    private boolean signInPasswordVisible;
    private boolean createPasswordVisible;
    private boolean signInLoading;
    private int signInSpinnerAngle;
    private Timer signInSpinnerTimer;

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
        JPanel root = new JPanel(new BorderLayout(56, 0));
        root.setBackground(PAGE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));

        JPanel heroPanel = buildHeroPanel();

        JPanel rightShell = new JPanel(new GridBagLayout());
        rightShell.setOpaque(false);

        cardPanel.setBorderColor(CARD_BG);
        cardPanel.setBorderWidth(0);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(SIGN_IN_CARD_SIZE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        brandBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandBadge.setForeground(MUTED);
        brandBadge.setFont(UI_FONT_MEDIUM.deriveFont(12.5f));
        brandBadge.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(18, BORDER, false),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

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

        cardPanel.add(brandBadge);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(tabRow);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(viewPanel);

        rightShell.add(cardPanel);
        root.add(heroPanel, BorderLayout.CENTER);
        root.add(rightShell, BorderLayout.EAST);
        return root;
    }

    private JPanel buildHeroPanel() {
        RoundedPanel hero = new RoundedPanel(36, new Color(0x0F, 0x18, 0x2D));
        hero.setLayout(new BorderLayout(0, 22));
        hero.setBorder(BorderFactory.createEmptyBorder(6, 22, 22, 22));
        hero.setPreferredSize(new Dimension(720, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel badge = new GradientBadgePanel();
        badge.setPreferredSize(new Dimension(170, 42));
        badge.setLayout(new BorderLayout());
        JLabel badgeLabel = new JLabel("LENDWISE", SwingConstants.CENTER);
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setFont(UI_FONT_MEDIUM.deriveFont(13.5f));
        badge.add(badgeLabel, BorderLayout.CENTER);

        top.add(badge, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setPreferredSize(new Dimension(300, 0));

        JLabel eyebrow = new JLabel("POLISHED LENDING WORKSPACE");
        eyebrow.setForeground(HERO_MUTED);
        eyebrow.setFont(UI_FONT_HERO_SECTION);

        JLabel headline = new JLabel("<html>Run lending operations from one polished, high-clarity workspace.</html>");
        headline.setForeground(TEXT);
        headline.setFont(UI_FONT_HERO_TITLE);

        JLabel supporting = new JLabel(
            "<html>Monitor daily collections, active loans, due alerts, and borrower records in one refined dashboard view.</html>"
        );
        supporting.setForeground(HERO_MUTED);
        supporting.setFont(UI_FONT_HERO_BODY);

        textBlock.add(Box.createVerticalGlue());
        textBlock.add(eyebrow);
        textBlock.add(Box.createVerticalStrut(12));
        textBlock.add(headline);
        textBlock.add(Box.createVerticalStrut(14));
        textBlock.add(supporting);
        textBlock.add(Box.createVerticalStrut(18));

        content.add(textBlock, BorderLayout.WEST);
        content.add(buildHeroImagePanel(), BorderLayout.CENTER);

        hero.add(top, BorderLayout.NORTH);
        hero.add(content, BorderLayout.CENTER);
        return hero;
    }

    private JPanel buildHeroImagePanel() {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        RoundedImagePanel imageLabel = new RoundedImagePanel(loadScaledImage(HERO_IMAGE_PATH, 400, 575), 28);
        imageLabel.setPreferredSize(new Dimension(400, 575));
        imagePanel.add(imageLabel, BorderLayout.WEST);
        return imagePanel;
    }

    private ImageIcon loadScaledImage(String path, int width, int height) {
        ImageIcon source = new ImageIcon(path);
        if (source.getIconWidth() <= 0 || source.getIconHeight() <= 0) {
            return null;
        }

        double scale = Math.min((double) width / source.getIconWidth(), (double) height / source.getIconHeight());
        int scaledWidth = Math.max(1, (int) Math.round(source.getIconWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(source.getIconHeight() * scale));
        Image scaled = source.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static final class RoundedImagePanel extends JPanel {
        private final ImageIcon image;
        private final int radius;

        private RoundedImagePanel(ImageIcon image, int radius) {
            this.image = image;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int x = Math.max(0, (getWidth() - image.getIconWidth()) / 2);
            int y = 0;
            Shape clip = new RoundRectangle2D.Float(x, y, image.getIconWidth(), image.getIconHeight(), radius, radius);
            g2.setClip(clip);
            g2.drawImage(image.getImage(), x, y, image.getIconWidth(), image.getIconHeight(), null);
            g2.dispose();
        }
    }

    private JPanel buildSignInView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = createTitle("Sign in to LendWise");
        JLabel subtitle = createSubtitle("Choose your portal first: Lender or Borrower.");

        styleInput(signInEmailField);
        stylePasswordInput(signInPasswordField);
        syncRoleButtonsWithCombo(signInRoleCombo, signInLenderButton, signInBorrowerButton);
        signInShowPasswordCheck.setFont(UI_FONT_SMALL);
        signInShowPasswordCheck.setOpaque(false);
        signInShowPasswordCheck.setForeground(MUTED);
        signInShowPasswordCheck.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        signInShowPasswordCheck.setMargin(new java.awt.Insets(0, 0, 0, 0));
        signInShowPasswordCheck.addActionListener(e -> {
            signInPasswordVisible = signInShowPasswordCheck.isSelected();
            signInPasswordField.setEchoChar(signInPasswordVisible ? (char) 0 : '\u2022');
        });

        styleGradientButton(signInButton, new Dimension(210, 38));
        signInButton.addActionListener(e -> handleLogin());
        styleTextLinkButton(forgotPasswordButton);
        forgotPasswordButton.addActionListener(e -> showForgotPasswordFlow());

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(14));
        content.add(createFieldLabel("Portal"));
        content.add(Box.createVerticalStrut(8));
        content.add(createRoleToggleGroup(signInRoleCombo, signInLenderButton, signInBorrowerButton));
        content.add(Box.createVerticalStrut(10));
        content.add(createFieldLabel("Email address"));
        content.add(Box.createVerticalStrut(8));
        content.add(wrapInputWithRoundedBorder(signInEmailField));
        content.add(Box.createVerticalStrut(10));
        content.add(createFieldLabel("Password"));
        content.add(Box.createVerticalStrut(8));
        content.add(wrapPasswordWithRoundedBorder(signInPasswordField));
        content.add(Box.createVerticalStrut(2));
        content.add(wrapCentered(signInShowPasswordCheck));
        content.add(Box.createVerticalStrut(38));
        content.add(wrapCentered(signInButton));
        content.add(Box.createVerticalStrut(10));
        content.add(wrapCentered(forgotPasswordButton));
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildCreateView() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = createTitle("Create Your Account");
        JLabel subtitle = createSubtitle("Create either a Lender account or a Borrower account.");

        styleInput(createNameField);
        styleInput(createEmailField);
        stylePasswordInput(createPasswordField);
        stylePasswordInput(createConfirmPasswordField);
        syncRoleButtonsWithCombo(createRoleCombo, createLenderButton, createBorrowerButton);

        createShowPasswordCheck.setFont(UI_FONT_SMALL);
        createShowPasswordCheck.setOpaque(false);
        createShowPasswordCheck.setForeground(MUTED);
        createShowPasswordCheck.addActionListener(e -> {
            createPasswordVisible = createShowPasswordCheck.isSelected();
            char echo = createPasswordVisible ? (char) 0 : '\u2022';
            createPasswordField.setEchoChar(echo);
            createConfirmPasswordField.setEchoChar(echo);
        });

        styleGradientButton(createAccountButton, new Dimension(210, 38));
        createAccountButton.addActionListener(e -> handleCreateAccount());

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(14));
        panel.add(createFieldLabel("Account type"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createRoleToggleGroup(createRoleCombo, createLenderButton, createBorrowerButton));
        panel.add(Box.createVerticalStrut(10));
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
        panel.add(Box.createVerticalStrut(12));
        panel.add(wrapCentered(createAccountButton));

        return panel;
    }

    private void switchView(String view) {
        boolean createView = VIEW_CREATE.equals(view);
        if (createView) {
            styleGhostButton(signInTabButton, new Dimension(0, 34));
            styleGradientButton(createAccountTabButton, new Dimension(0, 34));
            cardPanel.setPreferredSize(CREATE_CARD_SIZE);
            cardPanel.setFillColor(CARD_BG);
            cardPanel.setBorderColor(CARD_BG);
            brandBadge.setVisible(false);
            tabRow.setVisible(true);
            createNameField.requestFocusInWindow();
        } else {
            styleGradientButton(signInTabButton, new Dimension(0, 34));
            styleGhostButton(createAccountTabButton, new Dimension(0, 34));
            cardPanel.setPreferredSize(SIGN_IN_CARD_SIZE);
            cardPanel.setFillColor(CARD_BG);
            cardPanel.setBorderColor(CARD_BG);
            brandBadge.setVisible(false);
            tabRow.setVisible(true);
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
        title.setFont(UI_FONT_TITLE);
        return title;
    }

    private JLabel createSubtitle(String text) {
        JLabel subtitle = new JLabel(text, SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(MUTED);
        subtitle.setFont(UI_FONT.deriveFont(14f));
        return subtitle;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(MUTED);
        label.setMaximumSize(new Dimension(440, 18));
        label.setFont(UI_FONT_MEDIUM.deriveFont(13.5f));
        return label;
    }

    private JPanel createInputWrapper(Component field) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.setMinimumSize(new Dimension(440, 44));
        wrapper.setMaximumSize(new Dimension(440, 44));
        wrapper.setPreferredSize(new Dimension(440, 44));

        RoundedPanel rounded = new RoundedPanel(20, FIELD_BG);
        rounded.setBorderColor(BORDER);
        rounded.setBorderWidth(1);
        rounded.setLayout(new java.awt.BorderLayout());
        rounded.setMinimumSize(new Dimension(440, 44));
        rounded.setMaximumSize(new Dimension(440, 44));
        rounded.setPreferredSize(new Dimension(440, 44));
        rounded.add(field, java.awt.BorderLayout.CENTER);
        wrapper.add(rounded, java.awt.BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel wrapCentered(Component component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setOpaque(false);
        panel.add(component);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private JPanel wrapInputWithRoundedBorder(JTextField field) {
        return createInputWrapper(field);
    }

    private JPanel wrapPasswordWithRoundedBorder(JPasswordField field) {
        return createInputWrapper(field);
    }

    private JPanel createRoleToggleGroup(JComboBox<String> comboBox, JToggleButton lenderButton, JToggleButton borrowerButton) {
        styleRoleToggle(lenderButton);
        styleRoleToggle(borrowerButton);

        ButtonGroup group = new ButtonGroup();
        group.add(lenderButton);
        group.add(borrowerButton);

        lenderButton.addActionListener(e -> {
            comboBox.setSelectedItem("Lender");
            refreshRoleToggleStyles(lenderButton, borrowerButton);
        });
        borrowerButton.addActionListener(e -> {
            comboBox.setSelectedItem("Borrower");
            refreshRoleToggleStyles(lenderButton, borrowerButton);
        });

        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMinimumSize(new Dimension(440, 44));
        row.setMaximumSize(new Dimension(440, 44));
        row.setPreferredSize(new Dimension(440, 44));
        row.add(lenderButton);
        row.add(borrowerButton);

        refreshRoleToggleStyles(lenderButton, borrowerButton);
        return row;
    }

    private void syncRoleButtonsWithCombo(JComboBox<String> comboBox, JToggleButton lenderButton, JToggleButton borrowerButton) {
        String selected = comboBox.getSelectedItem() == null ? "Lender" : comboBox.getSelectedItem().toString();
        lenderButton.setSelected("Lender".equalsIgnoreCase(selected));
        borrowerButton.setSelected("Borrower".equalsIgnoreCase(selected));
        refreshRoleToggleStyles(lenderButton, borrowerButton);
    }

    private void styleRoleToggle(JToggleButton button) {
        button.setFont(UI_FONT_MEDIUM);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(0, 44));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                JToggleButton toggle = (JToggleButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill = toggle.isSelected() ? ROLE_BUTTON_ACTIVE : ROLE_BUTTON_FILL;
                if (toggle.getModel().isRollover() && !toggle.isSelected()) {
                    fill = ROLE_BUTTON_HOVER;
                }

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 18, 18);
                g2.setColor(toggle.isSelected() ? ROLE_BUTTON_ACTIVE.brighter() : BORDER);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 18, 18);

                g2.setColor(toggle.isSelected() ? Color.WHITE : (toggle.getModel().isRollover() ? TEXT : MUTED));
                g2.setFont(toggle.getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String text = toggle.getText();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
    }

    private void refreshRoleToggleStyles(JToggleButton lenderButton, JToggleButton borrowerButton) {
        applyRoleToggleState(lenderButton, lenderButton.isSelected());
        applyRoleToggleState(borrowerButton, borrowerButton.isSelected());
    }

    private void applyRoleToggleState(JToggleButton button, boolean selected) {
        button.repaint();
    }

    private void styleInput(JTextField field) {
        field.setFont(UI_FONT);
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(0x2A, 0x67, 0xF0));
        field.setSelectedTextColor(Color.WHITE);
        field.setMinimumSize(new Dimension(0, 44));
        field.setPreferredSize(new Dimension(440, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        field.setMargin(new java.awt.Insets(0, 0, 0, 0));
        field.setHorizontalAlignment(JTextField.LEFT);
    }

    private void stylePasswordInput(JPasswordField field) {
        field.setFont(UI_FONT);
        field.setEchoChar('\u2022');
        field.setOpaque(false);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(0x2A, 0x67, 0xF0));
        field.setSelectedTextColor(Color.WHITE);
        field.setMinimumSize(new Dimension(0, 44));
        field.setPreferredSize(new Dimension(440, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        field.setMargin(new java.awt.Insets(0, 0, 0, 0));
        field.setHorizontalAlignment(JTextField.LEFT);
    }

    private void styleGradientButton(JButton button, Dimension size) {
        button.setFont(UI_FONT_MEDIUM);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_BLUE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 24, 24);
                g2.setColor(Color.WHITE);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String text = c instanceof javax.swing.JButton ? ((javax.swing.JButton) c).getText() : "";
                boolean loading = Boolean.TRUE.equals(c.getClientProperty("loading"));
                int spinnerSize = 12;
                int gap = loading ? 10 : 0;
                int totalWidth = fm.stringWidth(text) + (loading ? spinnerSize + gap : 0);
                int textX = (c.getWidth() - totalWidth) / 2 + (loading ? spinnerSize + gap : 0);
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                if (loading) {
                    int spinnerX = textX - gap - spinnerSize;
                    int spinnerY = (c.getHeight() - spinnerSize) / 2;
                    g2.setStroke(new java.awt.BasicStroke(2.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    g2.drawArc(spinnerX, spinnerY, spinnerSize, spinnerSize, -(Integer) c.getClientProperty("spinnerAngle"), 300);
                }
                g2.drawString(text, textX, y);
            }
        });
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
    }

    private static final class GradientBadgePanel extends JPanel {
        private GradientBadgePanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, PRIMARY_BLUE, getWidth(), 0, ACCENT_GREEN));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void styleGhostButton(JButton button, Dimension size) {
        button.setFont(UI_FONT_MEDIUM);
        button.setFocusPainted(false);
        button.setForeground(MUTED);
        button.setBackground(FIELD_BG);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 24, 24);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 24, 24);
                g2.setColor(MUTED);
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

    private void styleTextLinkButton(JButton button) {
        button.setFont(UI_FONT_SMALL);
        button.setForeground(PRIMARY_BLUE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    private void handleCreateAccount() {
        String fullName = createNameField.getText().trim();
        String email = createEmailField.getText().trim();
        String password = new String(createPasswordField.getPassword()).trim();
        String confirmPassword = new String(createConfirmPasswordField.getPassword()).trim();
        String role = resolveRole((String) createRoleCombo.getSelectedItem());

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
        if (AccountStore.ROLE_BORROWER.equals(role) && !accountStore.canCreateBorrowerAccount(email)) {
            JOptionPane.showMessageDialog(
                this,
                "A borrower profile with that Gmail must exist in the admin panel before this borrower account can be created.",
                "Create Account",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (accountStore.accountExists(email)) {
            JOptionPane.showMessageDialog(this, "That account already exists. You can sign in now.", "Create Account", JOptionPane.INFORMATION_MESSAGE);
            switchView(VIEW_SIGN_IN);
            signInEmailField.setText(email);
            return;
        }
        if (!accountStore.createAccount(fullName, email, password, role)) {
            JOptionPane.showMessageDialog(this, "Account could not be created.", "Create Account", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String borrowerNote = AccountStore.ROLE_BORROWER.equals(role)
            ? "\nUse the same email as the borrower record so the account sees the correct loan data."
            : "";
        JOptionPane.showMessageDialog(this, "Account created successfully." + borrowerNote, "Create Account", JOptionPane.INFORMATION_MESSAGE);
        signInEmailField.setText(email);
        signInPasswordField.setText("");
        signInRoleCombo.setSelectedItem(AccountStore.ROLE_BORROWER.equals(role) ? "Borrower" : "Lender");
        createNameField.setText("");
        createEmailField.setText("");
        createPasswordField.setText("");
        createConfirmPasswordField.setText("");
        createRoleCombo.setSelectedIndex(0);
        switchView(VIEW_SIGN_IN);
    }

    private void handleLogin() {
        if (signInLoading) {
            return;
        }

        String email = signInEmailField.getText().trim();
        String password = new String(signInPasswordField.getPassword()).trim();
        String role = resolveRole((String) signInRoleCombo.getSelectedItem());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your email and password.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setSignInLoading(true);

        SwingWorker<AccountStore.AccountProfile, Void> worker = new SwingWorker<>() {
            @Override
            protected AccountStore.AccountProfile doInBackground() throws Exception {
                AccountStore.AccountProfile account = accountStore.authenticate(email, password, role);
                Thread.sleep(500L);
                return account;
            }

            @Override
            protected void done() {
                try {
                    AccountStore.AccountProfile account = get();
                    if (account == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this, "Invalid email, password, or portal type.", "Login", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (AccountStore.ROLE_BORROWER.equals(account.getRole())) {
                        accountStore.ensureBorrowerAccountLink(account.getEmail());
                        BorrowerDashboardFrame dashboard = new BorrowerDashboardFrame(account.getDisplayName(), account.getEmail());
                        dashboard.setVisible(true);
                    } else {
                        DashboardFrame dashboard = new DashboardFrame(account.getDisplayName(), account.getEmail());
                        dashboard.setVisible(true);
                    }
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this, "Could not sign in right now. Please try again.", "Login", JOptionPane.WARNING_MESSAGE);
                } finally {
                    setSignInLoading(false);
                }
            }
        };
        worker.execute();
    }

    private void setSignInLoading(boolean loading) {
        signInLoading = loading;
        signInButton.putClientProperty("loading", loading);
        signInButton.putClientProperty("spinnerAngle", signInSpinnerAngle);
        signInButton.setText(loading ? "SIGNING IN" : "SIGN IN");
        signInButton.setEnabled(!loading);
        signInEmailField.setEnabled(!loading);
        signInPasswordField.setEnabled(!loading);
        signInRoleCombo.setEnabled(!loading);
        signInShowPasswordCheck.setEnabled(!loading);
        forgotPasswordButton.setEnabled(!loading);
        signInLenderButton.setEnabled(!loading);
        signInBorrowerButton.setEnabled(!loading);

        if (loading) {
            if (signInSpinnerTimer == null) {
                signInSpinnerTimer = new Timer(90, e -> {
                    signInSpinnerAngle = (signInSpinnerAngle + 18) % 360;
                    signInButton.putClientProperty("spinnerAngle", signInSpinnerAngle);
                    signInButton.repaint();
                });
            }
            signInSpinnerTimer.start();
        } else {
            signInSpinnerAngle = 0;
            if (signInSpinnerTimer != null) {
                signInSpinnerTimer.stop();
            }
            signInButton.putClientProperty("spinnerAngle", signInSpinnerAngle);
            signInButton.repaint();
        }
    }

    private void showForgotPasswordFlow() {
        JTextField emailField = new JTextField(signInEmailField.getText().trim(), 24);
        emailField.setFont(UI_FONT);

        JPanel emailPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        emailPanel.add(new JLabel("Enter the email address for your account:"));
        emailPanel.add(emailField);

        int emailChoice = JOptionPane.showConfirmDialog(
            this,
            emailPanel,
            "Forgot Password",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (emailChoice != JOptionPane.OK_OPTION) {
            return;
        }

        String email = emailField.getText().trim();
        PasswordResetService.ResetResult requestResult = passwordResetService.requestOtp(email);
        if (!requestResult.success()) {
            String message = requestResult.configPath() == null
                ? requestResult.message()
                : requestResult.message() + "\nConfigure mail here: " + requestResult.configPath();
            JOptionPane.showMessageDialog(this, message, "Forgot Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        signInEmailField.setText(email);
        showOtpResetDialog(email, requestResult.message());
    }

    private void showOtpResetDialog(String email, String statusMessage) {
        while (true) {
            JTextField otpField = new JTextField(12);
            JPasswordField newPasswordField = new JPasswordField(18);
            JPasswordField confirmPasswordField = new JPasswordField(18);
            otpField.setFont(UI_FONT);
            newPasswordField.setFont(UI_FONT);
            confirmPasswordField.setFont(UI_FONT);

            JPanel resetPanel = new JPanel(new GridLayout(0, 1, 0, 8));
            resetPanel.add(new JLabel("<html><body style='width:260px'>" + statusMessage + "</body></html>"));
            resetPanel.add(new JLabel("OTP code"));
            resetPanel.add(otpField);
            resetPanel.add(new JLabel("New password"));
            resetPanel.add(newPasswordField);
            resetPanel.add(new JLabel("Confirm new password"));
            resetPanel.add(confirmPasswordField);

            Object[] options = {"Reset Password", "Resend OTP", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                this,
                resetPanel,
                "Reset Password",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
            );

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (choice == 1) {
                PasswordResetService.ResetResult resendResult = passwordResetService.requestOtp(email);
                String message = resendResult.configPath() == null
                    ? resendResult.message()
                    : resendResult.message() + "\nConfigure mail here: " + resendResult.configPath();
                JOptionPane.showMessageDialog(
                    this,
                    message,
                    "Reset Password",
                    resendResult.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
                if (resendResult.success()) {
                    statusMessage = resendResult.message();
                }
                continue;
            }

            String newPassword = new String(newPasswordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Reset Password", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            PasswordResetService.ResetResult resetResult = passwordResetService.resetPassword(
                email,
                otpField.getText().trim(),
                newPassword
            );
            JOptionPane.showMessageDialog(
                this,
                resetResult.message(),
                "Reset Password",
                resetResult.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
            );
            if (resetResult.success()) {
                signInEmailField.setText(email);
                signInPasswordField.setText("");
                return;
            }
            statusMessage = resetResult.message();
        }
    }

    private String resolveRole(String selectedLabel) {
        return "Borrower".equalsIgnoreCase(selectedLabel) ? AccountStore.ROLE_BORROWER : AccountStore.ROLE_LENDER;
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
