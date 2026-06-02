package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SearchPanel extends JPanel {
    private MainFrame parent;
    private UI.DarkField searchField;
    private UI.DarkCombo searchBy;
    private JPanel resultsPanel;
    private JLabel statusLabel;

    public SearchPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        buildUI();
    }

    private void buildUI() {
        // Topbar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Theme.BG_PANEL);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER), new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("  Search Employees");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRI);
        topbar.add(title, BorderLayout.WEST);

        // Search controls
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 14));
        searchBox.setBackground(Theme.BG_DEEP);
        searchBox.setBorder(new EmptyBorder(4,14,4,14));

        searchBy = new UI.DarkCombo(new String[]{
            "All Fields", "Employee ID", "Name", "Designation", "Phone", "Email"
        });
        searchBy.setPreferredSize(new Dimension(160, 34));

        searchField = new UI.DarkField("Enter search term...");
        searchField.setPreferredSize(new Dimension(360, 34));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSearch();
            }
        });

        UI.DarkButton searchBtn = UI.DarkButton.primary("Search");
        searchBtn.setPreferredSize(new Dimension(90, 34));
        searchBtn.addActionListener(e -> doSearch());

        UI.DarkButton clearBtn = UI.DarkButton.normal("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            resultsPanel.removeAll();
            statusLabel.setText("Enter a search query above");
            resultsPanel.revalidate();
            resultsPanel.repaint();
        });

        statusLabel = new JLabel("Enter a search query above");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUT);

        searchBox.add(searchBy);
        searchBox.add(searchField);
        searchBox.add(searchBtn);
        searchBox.add(clearBtn);
        searchBox.add(Box.createHorizontalStrut(10));
        searchBox.add(statusLabel);

        // Results area
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 12, 12));
        resultsPanel.setBackground(Theme.BG_DEEP);

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(new EmptyBorder(0,14,14,14));
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG_DEEP);
        body.add(searchBox, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);

        add(topbar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private void doSearch() {
        String query = searchField.getText().trim();
        String field = (String) searchBy.getSelectedItem();

        if (query.isEmpty()) {
            statusLabel.setText("Please enter a search term");
            return;
        }

        resultsPanel.removeAll();

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps;
            String like = "%" + query + "%";

            if ("All Fields".equals(field)) {
                ps = conn.prepareStatement(
                    "SELECT * FROM employee WHERE empId LIKE ? OR name LIKE ? OR designation LIKE ? OR phone LIKE ? OR email LIKE ?"
                );
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
                ps.setString(4, like); ps.setString(5, like);
            } else {
                String col;
                if      ("Employee ID".equals(field))  col = "empId";
                else if ("Name".equals(field))         col = "name";
                else if ("Designation".equals(field))  col = "designation";
                else if ("Phone".equals(field))        col = "phone";
                else if ("Email".equals(field))        col = "email";
                else                                   col = "name";
                ps = conn.prepareStatement("SELECT * FROM employee WHERE " + col + " LIKE ?");
                ps.setString(1, like);
            }

            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                resultsPanel.add(buildResultCard(rs, count));
                count++;
            }
            rs.close(); ps.close();

            if (count == 0) {
                JLabel empty = new JLabel("No results found for \"" + query + "\"");
                empty.setFont(Theme.FONT_MED);
                empty.setForeground(Theme.TEXT_MUT);
                empty.setBorder(new EmptyBorder(40,20,0,0));
                resultsPanel.add(empty);
                statusLabel.setText("0 results");
            } else {
                statusLabel.setText(count + " result" + (count > 1 ? "s" : "") + " found");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel buildResultCard(ResultSet rs, int idx) throws SQLException {
        String empId = rs.getString("empId");
        String name  = nvl(rs.getString("name"));
        String desig = nvl(rs.getString("designation"));
        String phone = nvl(rs.getString("phone"));
        String email = nvl(rs.getString("email"));
        String sal   = nvl(rs.getString("salary"));
        String edu   = nvl(rs.getString("education"));
        String dob   = nvl(rs.getString("dob"));

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(240, 260));
        card.setBorder(new EmptyBorder(16,16,16,16));

        // Avatar + name
        UI.AvatarPanel av = new UI.AvatarPanel(name, idx, 48);
        av.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nm = new JLabel(name, SwingConstants.CENTER);
        nm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nm.setForeground(Theme.TEXT_PRI);
        nm.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dg = new JLabel(desig, SwingConstants.CENTER);
        dg.setFont(Theme.FONT_SMALL);
        dg.setForeground(Theme.TEXT_MUT);
        dg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        badgeRow.setBackground(new Color(0,0,0,0));
        badgeRow.setOpaque(false);
        badgeRow.add(UI.BadgeLabel.green("Active"));
        badgeRow.add(UI.BadgeLabel.blue(empId));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setBackground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Info rows
        JPanel info = new JPanel(new GridLayout(0, 1, 0, 4));
        info.setBackground(new Color(0,0,0,0));
        info.setOpaque(false);
        addInfo(info, "📞", phone);
        addInfo(info, "✉", email);
        addInfo(info, "₹", sal.isEmpty() ? "—" : "₹" + sal);
        addInfo(info, "🎓", edu);

        // Action buttons
        JPanel acts = new JPanel(new GridLayout(1,2,6,0));
        acts.setBackground(new Color(0,0,0,0));
        acts.setOpaque(false);
        acts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        UI.DarkButton editBtn = UI.DarkButton.primary("Edit");
        UI.DarkButton delBtn  = UI.DarkButton.danger("Delete");
        editBtn.addActionListener(e -> openEditFromSearch(empId));
        delBtn.addActionListener(e -> deleteFromSearch(empId, name, card));
        acts.add(editBtn); acts.add(delBtn);

        card.add(av);
        card.add(Box.createVerticalStrut(8));
        card.add(nm);
        card.add(Box.createVerticalStrut(2));
        card.add(dg);
        card.add(Box.createVerticalStrut(6));
        card.add(badgeRow);
        card.add(Box.createVerticalStrut(10));
        card.add(sep);
        card.add(Box.createVerticalStrut(10));
        card.add(info);
        card.add(Box.createVerticalGlue());
        card.add(acts);
        return card;
    }

    private void addInfo(JPanel panel, String icon, String text) {
        JLabel l = new JLabel(icon + "  " + (text.isEmpty() ? "—" : text));
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SEC);
        panel.add(l);
    }

    private void openEditFromSearch(String empId) {
        parent.navigateTo(1);
        // A small delay to let the panel load, then trigger edit
        SwingUtilities.invokeLater(() -> {
            UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),
                "Use Employee panel to edit " + empId, false);
        });
    }

    private void deleteFromSearch(String empId, String name, JPanel card) {
        int res = JOptionPane.showConfirmDialog(this,
            "Delete " + name + "?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM employee WHERE empId=?");
                ps.setString(1, empId); ps.executeUpdate(); ps.close();
                resultsPanel.remove(card);
                resultsPanel.revalidate(); resultsPanel.repaint();
                UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this), "Deleted " + name, true);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    // FlowLayout that wraps nicely
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container c) {
            Dimension d = layoutSize(c, true);
            Insets i = c.getInsets();
            d.width  += i.left + i.right;
            d.height += i.top  + i.bottom;
            return d;
        }
        private Dimension layoutSize(Container c, boolean preferred) {
            int width = c.getWidth();
            if (width == 0) width = Integer.MAX_VALUE;
            int rowWidth = 0, rowHeight = 0, totalHeight = getVgap();
            int nmembers = c.getComponentCount();
            for (int i = 0; i < nmembers; i++) {
                Component m = c.getComponent(i);
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width + getHgap() > width) {
                    totalHeight += rowHeight + getVgap();
                    rowWidth = 0; rowHeight = 0;
                }
                rowWidth += d.width + getHgap();
                rowHeight = Math.max(rowHeight, d.height);
            }
            totalHeight += rowHeight + getVgap();
            return new Dimension(width, totalHeight);
        }
    }
}
