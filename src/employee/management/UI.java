package employee.management;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class UI {

    // ── Dark-themed JButton ──────────────────────────────────────────────────
    public static class DarkButton extends JButton {
        private Color normalBg, hoverBg, textColor, borderColor;

        public DarkButton(String text, Color bg, Color hover, Color fg, Color border) {
            super(text);
            this.normalBg  = bg;
            this.hoverBg   = hover;
            this.textColor = fg;
            this.borderColor = border;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(Theme.FONT_MED);
            setForeground(fg);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { repaint(); }
                @Override public void mouseExited(MouseEvent e)  { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            g2.setColor(hover ? hoverBg : normalBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }

        public static DarkButton primary(String text) {
            return new DarkButton(text, Theme.BTN_BLU_BG, new Color(0x25,0x45,0x70),
                    Theme.ACCENT_BLU, new Color(0x2a,0x4a,0x6f));
        }
        public static DarkButton danger(String text) {
            return new DarkButton(text, Theme.BTN_RED_BG, new Color(0x3a,0x10,0x10),
                    Theme.ACCENT_RED, new Color(0x3d,0x15,0x15));
        }
        public static DarkButton success(String text) {
            return new DarkButton(text, Theme.BTN_GRN_BG, new Color(0x0d,0x2a,0x20),
                    Theme.ACCENT_GRN, new Color(0x0f,0x30,0x20));
        }
        public static DarkButton normal(String text) {
            return new DarkButton(text, Theme.BG_CARD, Theme.BG_HOVER,
                    Theme.TEXT_SEC, Theme.BORDER);
        }
    }

    // ── Dark-themed JTextField ───────────────────────────────────────────────
    public static class DarkField extends JTextField {
        public DarkField(String placeholder) {
            super();
            setFont(Theme.FONT_MED);
            setForeground(Theme.TEXT_PRI);
            setBackground(Theme.BG_INPUT);
            setCaretColor(Theme.TEXT_PRI);
            setBorder(new CompoundBorder(
                new RoundBorder(Theme.BORDER, 8),
                new EmptyBorder(6, 10, 6, 10)
            ));
            setOpaque(false);
            putClientProperty("placeholder", placeholder);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D pg = (Graphics2D) g.create();
                pg.setColor(Theme.TEXT_MUT);
                pg.setFont(getFont());
                FontMetrics fm = pg.getFontMetrics();
                String ph = (String) getClientProperty("placeholder");
                if (ph != null) {
                    pg.drawString(ph, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
                pg.dispose();
            }
        }
    }

    // ── Dark-themed JPasswordField ───────────────────────────────────────────
    public static class DarkPassField extends JPasswordField {
        public DarkPassField(String placeholder) {
            super();
            setFont(Theme.FONT_MED);
            setForeground(Theme.TEXT_PRI);
            setBackground(Theme.BG_INPUT);
            setCaretColor(Theme.TEXT_PRI);
            setBorder(new CompoundBorder(
                new RoundBorder(Theme.BORDER, 8),
                new EmptyBorder(6, 10, 6, 10)
            ));
            setOpaque(false);
            putClientProperty("placeholder", placeholder);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Dark-themed JComboBox ────────────────────────────────────────────────
    public static class DarkCombo extends JComboBox<String> {
        public DarkCombo(String[] items) {
            super(items);
            setFont(Theme.FONT_MED);
            setForeground(Theme.TEXT_SEC);
            setBackground(Theme.BG_PANEL);
            setBorder(new CompoundBorder(new RoundBorder(Theme.BORDER, 8), new EmptyBorder(4,8,4,8)));
            setOpaque(true);
            ((JComponent)getRenderer()).setOpaque(true);
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int idx, boolean sel, boolean focus) {
                    super.getListCellRendererComponent(list, value, idx, sel, focus);
                    setBackground(sel ? Theme.BG_HOVER : Theme.BG_PANEL);
                    setForeground(Theme.TEXT_SEC);
                    setBorder(new EmptyBorder(4, 10, 4, 10));
                    return this;
                }
            });
        }
    }

    // ── Rounded border ───────────────────────────────────────────────────────
    public static class RoundBorder extends AbstractBorder {
        private Color color;
        private int radius;
        public RoundBorder(Color c, int r) { color = c; radius = r; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
    }

    // ── Avatar circle ────────────────────────────────────────────────────────
    public static class AvatarPanel extends JPanel {
        private String initials;
        private Color bg, fg;
        private int size;
        public AvatarPanel(String name, int idx, int size) {
            this.size = size;
            Color[] pair = Theme.AVATAR_COLORS[idx % Theme.AVATAR_COLORS.length];
            bg = pair[0]; fg = pair[1];
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            initials = sb.length() > 2 ? sb.substring(0,2) : sb.toString();
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillOval(0, 0, size, size);
            g2.setColor(fg);
            g2.setFont(new Font("Segoe UI", Font.BOLD, size/3));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (size - fm.stringWidth(initials)) / 2;
            int ty = (size + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(initials, tx, ty);
            g2.dispose();
        }
    }

    // ── Badge label ──────────────────────────────────────────────────────────
    public static class BadgeLabel extends JLabel {
        private Color bg, fg, border;
        public BadgeLabel(String text, Color bg, Color fg, Color border) {
            super(text);
            this.bg = bg; this.fg = fg; this.border = border;
            setForeground(fg);
            setFont(Theme.FONT_SMALL);
            setOpaque(false);
            setHorizontalAlignment(CENTER);
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 16, d.height + 6);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }

        public static BadgeLabel green(String text) {
            return new BadgeLabel(text, Theme.BTN_GRN_BG, Theme.ACCENT_GRN, new Color(0x0f,0x30,0x20));
        }
        public static BadgeLabel yellow(String text) {
            return new BadgeLabel(text, new Color(0x2d,0x20,0x06), Theme.ACCENT_YLW, new Color(0x3d,0x2a,0x08));
        }
        public static BadgeLabel blue(String text) {
            return new BadgeLabel(text, new Color(0x0e,0x1f,0x3d), Theme.ACCENT_BLU, new Color(0x15,0x30,0x55));
        }
        public static BadgeLabel red(String text) {
            return new BadgeLabel(text, Theme.BTN_RED_BG, Theme.ACCENT_RED, new Color(0x3d,0x15,0x15));
        }
    }

    // ── Stat card ────────────────────────────────────────────────────────────
    public static class StatCard extends JPanel {
        public StatCard(String label, String value, String sub, Color subColor) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.BG_PANEL);
            setBorder(new CompoundBorder(new RoundBorder(Theme.BORDER, 10), new EmptyBorder(12,14,12,14)));
            JLabel lbl = new JLabel(label);
            lbl.setFont(Theme.FONT_SMALL);
            lbl.setForeground(Theme.TEXT_MUT);
            JLabel val = new JLabel(value);
            val.setFont(Theme.FONT_STAT);
            val.setForeground(Theme.TEXT_PRI);
            JLabel s = new JLabel(sub);
            s.setFont(Theme.FONT_SMALL);
            s.setForeground(subColor);
            add(lbl); add(Box.createVerticalStrut(4));
            add(val); add(Box.createVerticalStrut(3));
            add(s);
        }
    }

    // ── Section card wrapper ─────────────────────────────────────────────────
    public static JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(new CompoundBorder(new RoundBorder(Theme.BORDER, 10), new EmptyBorder(14,14,14,14)));
        if (title != null) {
            JLabel t = new JLabel(title);
            t.setFont(Theme.FONT_TITLE);
            t.setForeground(Theme.TEXT_PRI);
            t.setBorder(new EmptyBorder(0,0,10,0));
            p.add(t, BorderLayout.NORTH);
        }
        return p;
    }

    // ── Separator line ───────────────────────────────────────────────────────
    public static JPanel separator() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BORDER);
        p.setPreferredSize(new Dimension(1, 1));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return p;
    }

    // ── Toast notification ───────────────────────────────────────────────────
    public static void showToast(JFrame parent, String message, boolean error) {
        JWindow toast = new JWindow(parent);
        toast.setBackground(new Color(0,0,0,0));
        JLabel lbl = new JLabel("  " + message + "  ");
        lbl.setFont(Theme.FONT_MED);
        lbl.setForeground(error ? Theme.ACCENT_RED : Theme.ACCENT_GRN);
        lbl.setOpaque(false);
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(error ? Theme.BTN_RED_BG : Theme.BTN_GRN_BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(error ? new Color(0x3d,0x15,0x15) : new Color(0x0f,0x30,0x20));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8,14,8,14));
        panel.add(lbl);
        toast.add(panel);
        toast.pack();
        // Position bottom-right of parent
        Point loc = parent.getLocationOnScreen();
        Dimension sz = parent.getSize();
        toast.setLocation(loc.x + sz.width - toast.getWidth() - 20,
                          loc.y + sz.height - toast.getHeight() - 20);
        toast.setVisible(true);
        new javax.swing.Timer(2500, e -> toast.dispose()).start();
    }
}