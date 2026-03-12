package lendwise.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Central dark theme for LendWise. Use these colors and helpers everywhere
 * so the app looks modern and consistent.
 */
public final class UITheme {
    private UITheme() {}

    // Dark background (main window)
    public static final Color BG = new Color(0x0D, 0x11, 0x18);
    // Card / panel background
    public static final Color CARD = new Color(0x16, 0x1B, 0x26);
    // Slightly lighter (headers, alternating rows, inputs)
    public static final Color CARD_2 = new Color(0x1F, 0x26, 0x36);
    // Borders and grid lines
    public static final Color BORDER = new Color(0x2D, 0x37, 0x48);
    // Accent (primary buttons, selection) – green
    public static final Color ACCENT = new Color(0x22, 0xC5, 0x5E);
    // Main text
    public static final Color TEXT = new Color(0xF0, 0xF2, 0xF5);
    // Secondary / muted text
    public static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);

    public static void applyPrimaryButton(JButton btn) {
        if (btn == null) return;
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new RoundedButtonBorder(12, ACCENT, false));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
    }

    public static void applySecondaryButton(JButton btn) {
        if (btn == null) return;
        btn.setBackground(CARD_2);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(new RoundedButtonBorder(12, BORDER, false));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
    }

    public static void applyTextField(JTextField field) {
        if (field == null) return;
        field.setBackground(CARD_2);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    /** Call once at startup so table/header use dark theme (avoids L&F painting white). */
    public static void installTableDefaults() {
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("TableHeader.background", CARD_2);
        UIManager.put("TableHeader.foreground", TEXT);
    }

    public static void applyTable(JTable table) {
        if (table == null) return;
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, createDarkCellRenderer());
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(CARD_2);
            header.setForeground(TEXT);
            header.setOpaque(true);
            header.setDefaultRenderer(createHeaderRenderer());
            Font f = header.getFont();
            if (f != null) header.setFont(f.deriveFont(Font.BOLD));
            header.setReorderingAllowed(false);
        }
    }

    private static TableCellRenderer createHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                l.setBackground(CARD_2);
                l.setForeground(TEXT);
                l.setOpaque(true);
                l.setHorizontalAlignment(JLabel.CENTER);
                return l;
            }
        };
    }

    private static TableCellRenderer createDarkCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                javax.swing.JComponent c = (javax.swing.JComponent) super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                c.setOpaque(true);
                if (selected) {
                    c.setBackground(ACCENT);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground((row % 2 == 0) ? CARD : CARD_2);
                    c.setForeground(TEXT);
                }
                return c;
            }
        };
    }

    public static void applyScrollPane(JScrollPane scroll) {
        if (scroll == null) return;
        scroll.getViewport().setBackground(CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
    }

    public static void applyCardPadding(JComponent c) {
        if (c == null) return;
        c.setBorder(new EmptyBorder(16, 16, 16, 16));
    }
}
