package lendwise.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public final class UITheme {
    public enum Mode {
        DARK,
        LIGHT
    }

    private static Mode mode = Mode.DARK;

    public static Color BG;
    public static Color CARD;
    public static Color CARD_2;
    public static Color PANEL_BG;
    public static Color BORDER;
    public static Color ACCENT;
    public static Color TEXT;
    public static Color TEXT_MUTED;

    static {
        applyMode(mode);
    }

    private UITheme() {}

    public static Mode getMode() {
        return mode;
    }

    public static boolean isLightMode() {
        return mode == Mode.LIGHT;
    }

    public static void toggleMode() {
        setMode(mode == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    public static void setMode(Mode nextMode) {
        mode = nextMode == null ? Mode.DARK : nextMode;
        applyMode(mode);
        installTableDefaults();
    }

    private static void applyMode(Mode currentMode) {
        if (currentMode == Mode.LIGHT) {
            BG = new Color(0xF2, 0xF5, 0xFA);
            CARD = new Color(0xF6, 0xF8, 0xFC);
            CARD_2 = new Color(0xE6, 0xEC, 0xF5);
            PANEL_BG = new Color(0xFB, 0xFC, 0xFE);
            BORDER = new Color(0xD9, 0xE2, 0xEC);
            ACCENT = new Color(0x35, 0x6A, 0xE6);
            TEXT = new Color(0x0F, 0x17, 0x2A);
            TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
        } else {
            BG = new Color(0x0D, 0x11, 0x18);
            CARD = new Color(0x16, 0x1B, 0x26);
            CARD_2 = new Color(0x1F, 0x26, 0x36);
            PANEL_BG = new Color(0x13, 0x19, 0x29);
            BORDER = new Color(0x2D, 0x37, 0x48);
            ACCENT = new Color(0x25, 0x63, 0xEB);
            TEXT = new Color(0xF0, 0xF2, 0xF5);
            TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
        }
    }

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

    public static void installTableDefaults() {
        UIManager.put("Table.background", PANEL_BG);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("TableHeader.background", tableHeaderBackground());
        UIManager.put("TableHeader.foreground", tableHeaderForeground());
    }

    public static void applyTable(JTable table) {
        if (table == null) return;
        table.setBackground(PANEL_BG);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(CARD_2);
        table.setSelectionForeground(TEXT);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, createCellRenderer());
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(tableHeaderBackground());
            header.setForeground(tableHeaderForeground());
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
                l.setBackground(tableHeaderBackground());
                l.setForeground(tableHeaderForeground());
                l.setOpaque(true);
                l.setHorizontalAlignment(JLabel.CENTER);
                return l;
            }
        };
    }

    public static Color tableHeaderBackground() {
        return isLightMode() ? ACCENT : new Color(0x1B, 0x2A, 0x44);
    }

    public static Color tableHeaderForeground() {
        return isLightMode() ? Color.WHITE : TEXT;
    }

    public static Color tableHeaderDivider() {
        return isLightMode() ? new Color(0xC7, 0xD2, 0xFE) : new Color(0x2F, 0x4A, 0x72);
    }

    private static TableCellRenderer createCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                JComponent c = (JComponent) super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                c.setOpaque(true);
                c.setBackground(selected ? CARD_2 : PANEL_BG);
                c.setForeground(TEXT);
                return c;
            }
        };
    }

    public static void applyScrollPane(JScrollPane scroll) {
        if (scroll == null) return;
        scroll.getViewport().setBackground(PANEL_BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
    }

    public static void applyCardPadding(JComponent c) {
        if (c == null) return;
        c.setBorder(new EmptyBorder(16, 16, 16, 16));
    }

    public static Color metricCardFill(Color accent) {
        if (accent == null) {
            return PANEL_BG;
        }
        if (!isLightMode()) {
            return blend(accent, BG, 0.78f);
        }
        return blend(accent, Color.WHITE, 0.94f);
    }

    public static Color metricPrimaryText() {
        return isLightMode() ? Color.WHITE : TEXT;
    }

    public static Color metricSecondaryText() {
        return isLightMode() ? new Color(0xF5, 0xF9, 0xFF) : new Color(0xE2, 0xE8, 0xF0);
    }

    public static Color metricPillBorder() {
        return isLightMode() ? new Color(0xFF, 0xFF, 0xFF, 215) : new Color(0xFF, 0xFF, 0xFF, 130);
    }

    public static Color metricPillFill(Color accent) {
        if (accent == null) {
            return PANEL_BG;
        }
        if (!isLightMode()) {
            return blend(accent, BG, 0.56f);
        }
        return blend(accent, Color.WHITE, 0.82f);
    }

    private static Color blend(Color first, Color second, float ratioFirst) {
        float ratio = Math.max(0f, Math.min(1f, ratioFirst));
        float inverse = 1f - ratio;
        int red = Math.round((first.getRed() * ratio) + (second.getRed() * inverse));
        int green = Math.round((first.getGreen() * ratio) + (second.getGreen() * inverse));
        int blue = Math.round((first.getBlue() * ratio) + (second.getBlue() * inverse));
        return new Color(red, green, blue);
    }
}
