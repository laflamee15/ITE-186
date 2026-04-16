package lendwise.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public final class UITheme {
    private static final String FONT_FAMILY = resolveFontFamily();
    private static final Locale PH_LOCALE = Locale.forLanguageTag("en-PH");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(PH_LOCALE);

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
    public static Font UI_FONT;
    public static Font UI_FONT_MEDIUM;
    public static Font UI_FONT_SMALL;
    public static Font UI_FONT_TITLE;
    public static Font UI_FONT_H1;
    public static Font UI_FONT_H2;
    public static Font UI_FONT_METRIC;
    public static Font UI_FONT_SIDEBAR_TITLE;
    public static Font UI_FONT_SECTION;

    static {
        applyMode(mode);
        installTypography();
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
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
        installTypography();
        installTableDefaults();
    }

    public static void installTypography() {
        UI_FONT = createFont(Font.PLAIN, 15f);
        UI_FONT_MEDIUM = createFont(Font.BOLD, 15f);
        UI_FONT_SMALL = createFont(Font.PLAIN, 12.5f);
        UI_FONT_TITLE = createFont(Font.BOLD, 30f);
        UI_FONT_H1 = createFont(Font.BOLD, 18f);
        UI_FONT_H2 = createFont(Font.BOLD, 23f);
        UI_FONT_METRIC = createFont(Font.BOLD, 34f);
        UI_FONT_SIDEBAR_TITLE = createFont(Font.BOLD, 25f);
        UI_FONT_SECTION = createFont(Font.BOLD, 11.5f);
    }

    public static Font createFont(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size)).deriveFont(style, size);
    }

    public static Font pageTitleFont() {
        return UI_FONT_TITLE;
    }

    public static Font sectionTitleFont() {
        return UI_FONT_H2;
    }

    public static Font headerFont() {
        return UI_FONT_H1;
    }

    public static Font metricValueFont() {
        return UI_FONT_METRIC;
    }

    public static Font metricLabelFont() {
        return UI_FONT_SECTION;
    }

    public static Font sidebarTitleFont() {
        return UI_FONT_SIDEBAR_TITLE;
    }

    public static Font captionFont() {
        return UI_FONT_SMALL;
    }

    public static String formatCurrency(double amount) {
        return "\u20B1" + CURRENCY_FORMAT.format(amount);
    }

    private static String resolveFontFamily() {
        String[] preferredFamilies = {
            "Aptos",
            "Segoe UI Variable Text",
            "Segoe UI Variable Display",
            "Segoe UI Variable",
            "Segoe UI",
            "Inter",
            "Arial"
        };

        String[] availableFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String preferred : preferredFamilies) {
            for (String available : availableFamilies) {
                if (preferred.equalsIgnoreCase(available)) {
                    return available;
                }
            }
        }

        Font base = UIManager.getFont("Label.font");
        if (base != null && base.getFamily() != null && !base.getFamily().isBlank()) {
            return base.getFamily();
        }
        return Font.SANS_SERIF;
    }

    private static void applyMode(Mode currentMode) {
        if (currentMode == Mode.LIGHT) {
            BG = new Color(0xE8, 0xF0, 0xFA);
            CARD = new Color(0xF0, 0xF5, 0xFC);
            CARD_2 = new Color(0xDF, 0xE9, 0xF7);
            PANEL_BG = new Color(0xF5, 0xF9, 0xFE);
            BORDER = new Color(0xC9, 0xD8, 0xEB);
            ACCENT = new Color(0x4A, 0x7E, 0xE8);
            TEXT = new Color(0x14, 0x23, 0x38);
            TEXT_MUTED = new Color(0x5E, 0x72, 0x8F);
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

    public static void applyHeaderActionButton(JButton btn, boolean primary) {
        if (btn == null) return;

        Color fill = primary ? ACCENT : CARD_2;
        Color hover = primary
            ? ACCENT.brighter()
            : (isLightMode() ? new Color(0xD9, 0xE4, 0xF5) : new Color(0x2A, 0x34, 0x47));
        Color border = primary ? ACCENT : BORDER;
        Color text = primary ? Color.WHITE : TEXT;

        btn.putClientProperty("header.fill", fill);
        btn.putClientProperty("header.hover", hover);
        btn.putClientProperty("header.border", border);
        btn.putClientProperty("header.text", text);
        btn.setForeground(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, 0.02f);
        Font actionFont = createFont(primary ? Font.BOLD : Font.PLAIN, primary ? 13f : 12.75f)
            .deriveFont(attributes);
        btn.setFont(actionFont);
        btn.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                AbstractButton button = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                Color currentFill = button.getModel().isRollover()
                    ? (Color) button.getClientProperty("header.hover")
                    : (Color) button.getClientProperty("header.fill");
                Color currentBorder = (Color) button.getClientProperty("header.border");
                Color currentText = (Color) button.getClientProperty("header.text");

                g2.setColor(currentFill);
                g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                g2.setColor(currentBorder);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);

                g2.setColor(currentText);
                g2.setFont(button.getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String textValue = button.getText();
                int x = (c.getWidth() - fm.stringWidth(textValue)) / 2;
                int y = ((c.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(textValue, x, y);
                g2.dispose();
            }
        });
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
