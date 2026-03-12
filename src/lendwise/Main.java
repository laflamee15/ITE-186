package lendwise;

import javax.swing.SwingUtilities;
import lendwise.ui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}

