package lendwise.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.Border;

/** Rounded border for buttons (pill / soft rectangle). */
public class RoundedButtonBorder implements Border {
    private final int radius;
    private final Color color;
    private final boolean fill;

    public RoundedButtonBorder(int radius, Color color, boolean fill) {
        this.radius = radius;
        this.color = color;
        this.fill = fill;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(10, 18, 10, 18);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        int arc = radius * 2;
        if (fill) {
            g2.fillRoundRect(x, y, w - 1, h - 1, arc, arc);
        } else {
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        }
        g2.dispose();
    }
}
