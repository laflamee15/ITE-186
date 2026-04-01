package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Centers a single child component, constrains its width, and allows vertical scrolling
 * when the viewport is too short for the content.
 */
public class CenteredWidthPanel extends JPanel {
    private final int maxWidth;
    private final JComponent content;
    private final JPanel centeredContent = new JPanel(new GridBagLayout());
    private final JScrollPane scrollPane = new JScrollPane();

    public CenteredWidthPanel(int maxWidth, JComponent content) {
        this.maxWidth = Math.max(320, maxWidth);
        this.content = content;

        setOpaque(true);
        setBackground(UITheme.BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));

        centeredContent.setOpaque(true);
        centeredContent.setBackground(UITheme.BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        centeredContent.add(content, gbc);

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UITheme.BG);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(UITheme.BG);
        scrollPane.getVerticalScrollBar().setBackground(UITheme.BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(centeredContent);

        add(scrollPane, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePreferredWidth();
            }
        });

        updatePreferredWidth();
    }

    private void updatePreferredWidth() {
        int available = Math.max(0, scrollPane.getViewport().getWidth());
        if (available <= 0) {
            available = Math.max(0, getWidth());
        }

        int targetW = Math.min(available, maxWidth);
        Dimension pref = content.getPreferredSize();
        int targetH = pref == null ? 0 : pref.height;
        content.setPreferredSize(new Dimension(targetW, targetH));
        content.revalidate();
    }
}
