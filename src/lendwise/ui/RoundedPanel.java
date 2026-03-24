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
    private Color borderColor;
    private int borderWidth;

    public RoundedPanel(int radius, Color fillColor) {
        this.radius = radius;
        this.fillColor = fillColor;
        setOpaque(false);
    }

    public RoundedPanel() {
        this(20, UITheme.CARD);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(0, borderWidth);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fillColor != null ? fillColor : UITheme.CARD);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        if (borderColor != null && borderWidth > 0) {
            g2.setColor(borderColor);
            for (int i = 0; i < borderWidth; i++) {
                g2.drawRoundRect(i, i, getWidth() - 1 - (i * 2), getHeight() - 1 - (i * 2), radius, radius);
            }
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
