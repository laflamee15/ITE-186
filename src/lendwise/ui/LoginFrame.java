package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class LoginFrame extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public LoginFrame() {
        setTitle("LendWise - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(480, 340));
        setLocationRelativeTo(null);

        trySetLookAndFeel();
        UITheme.installTableDefaults();

        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        root.setBackground(UITheme.BG);

        JLabel title = new JLabel("LendWise", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(32f));
        title.setForeground(UITheme.TEXT);

        RoundedPanel card = new RoundedPanel(24, UITheme.CARD);
        card.setLayout(new BorderLayout());
        UITheme.applyCardPadding(card);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        usernameLabel.setForeground(UITheme.TEXT_MUTED);
        passwordLabel.setForeground(UITheme.TEXT_MUTED);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        UITheme.applyTextField(usernameField);
        passwordField.setBackground(UITheme.CARD_2);
        passwordField.setForeground(UITheme.TEXT);
        passwordField.setCaretColor(UITheme.TEXT);
        passwordField.setBorder(usernameField.getBorder());

        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit");
        UITheme.applyPrimaryButton(loginButton);
        UITheme.applySecondaryButton(exitButton);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; form.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(passwordField, gbc);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(loginButton);
        buttons.add(exitButton);

        loginButton.addActionListener(e -> handleLogin());
        exitButton.addActionListener(e -> dispose());

        card.add(form, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);

        root.add(title, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username and password.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DashboardFrame dashboard = new DashboardFrame(username);
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
        } catch (Exception ignored) {}
    }
}
