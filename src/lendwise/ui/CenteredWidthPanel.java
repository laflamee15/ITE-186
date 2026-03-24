package lendwise.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Centers a single child component and constrains its width to a max value.
 * This matches the common "content column" layout (like modern dashboards).
 */
public class CenteredWidthPanel extends JPanel {
    private final int maxWidth;
    private final JComponent content;

    public CenteredWidthPanel(int maxWidth, JComponent content) {
        this.maxWidth = Math.max(320, maxWidth);
        this.content = content;

        setOpaque(false);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);

        add(content, gbc);

        // Keep the centered column width consistent on resize.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePreferredWidth();
            }
        });

        updatePreferredWidth();
    }

    private void updatePreferredWidth() {
        int available = Math.max(0, getWidth());
        int targetW = Math.min(available, maxWidth);

        Dimension pref = content.getPreferredSize();
        int targetH = pref == null ? 0 : pref.height;
        content.setPreferredSize(new Dimension(targetW, targetH));
        content.revalidate();
    }
}

