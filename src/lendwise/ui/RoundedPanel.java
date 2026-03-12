package lendwise.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/** Panel with rounded corners (card style). */
public class RoundedPanel extends JPanel {
    private final int radius;
    private Color fillColor;

    public RoundedPanel(int radius, Color fillColor) {
        this.radius = radius;
        this.fillColor = fillColor;
        setOpaque(false);
    }

    public RoundedPanel() {
        this(20, UITheme.CARD);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fillColor != null ? fillColor : UITheme.CARD);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}
