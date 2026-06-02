package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class DashboardPanel extends JPanel {
    private MainFrame parent;
    private JLabel totalEmpVal, presentVal, onLeaveVal, birthdayVal;

    public DashboardPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        buildUI();
        loadStats();
    }

    private void buildUI() {
        // ── Topbar ────────────────────────────────────────────────────────────
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Theme.BG_PANEL);
        topbar.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0, Theme.BORDER),
            new EmptyBorder(10,20,10,20)
        ));
        JLabel title = new JLabel("  Dashboard");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRI);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBar.setBackground(Theme.BG_PANEL);
        UI.DarkButton addBtn = UI.DarkButton.primary("+ Add Employee");
        addBtn.addActionListener(e -> parent.navigateTo(1));
        rightBar.add(addBtn);

        topbar.add(title, BorderLayout.WEST);
        topbar.add(rightBar, BorderLayout.EAST);

        // ── Scroll content ────────────────────────────────────────────────────
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_DEEP);
        content.setBorder(new EmptyBorder(16,16,16,16));

        // Stat cards
        JPanel statGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        statGrid.setBackground(Theme.BG_DEEP);
        statGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        totalEmpVal = new JLabel("...");
        presentVal  = new JLabel("...");
        onLeaveVal  = new JLabel("...");
        birthdayVal = new JLabel("...");

        statGrid.add(makeStatCard("Total Employees",  totalEmpVal, "Loading...",    Theme.ACCENT_GRN));
        statGrid.add(makeStatCard("Present Today",    presentVal,  "87% attendance",Theme.ACCENT_GRN));
        statGrid.add(makeStatCard("On Leave",         onLeaveVal,  "Pending approval",Theme.ACCENT_YLW));
        statGrid.add(makeStatCard("Birthdays Today",  birthdayVal, "Celebration time!",Theme.ACCENT_BLU));

        content.add(statGrid);
        content.add(Box.createVerticalStrut(14));

        // Two-column: recent employees + announcements
        JPanel grid2 = new JPanel(new GridLayout(1, 2, 12, 0));
        grid2.setBackground(Theme.BG_DEEP);

        grid2.add(buildRecentEmployees());
        grid2.add(buildAnnouncements());

        content.add(grid2);
        content.add(Box.createVerticalStrut(14));

        // Quick actions row
        content.add(buildQuickActions());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);
        scroll.getVerticalScrollBar().setBackground(Theme.BG_DEEP);

        add(topbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel makeStatCard(String label, JLabel valLabel, String sub, Color subColor) {
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
        card.setBorder(new EmptyBorder(12,14,12,14));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUT);

        valLabel.setFont(Theme.FONT_STAT);
        valLabel.setForeground(Theme.TEXT_PRI);

        JLabel s = new JLabel(sub);
        s.setFont(Theme.FONT_SMALL);
        s.setForeground(subColor);

        card.add(lbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(s);
        return card;
    }

    private JPanel buildRecentEmployees() {
        JPanel card = UI.card("Recent Employees");
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.BG_PANEL);

        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT empId, name, designation FROM employee ORDER BY created_at DESC LIMIT 5";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int idx = 0;
            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String id = rs.getString("empId");
                String name = rs.getString("name");
                String desig = rs.getString("designation");
                if (desig == null) desig = "—";
                list.add(buildEmpRow(name, desig, idx++));
                list.add(Box.createVerticalStrut(2));
            }
            if (!hasRows) {
                JLabel empty = new JLabel("No employees yet. Add one!");
                empty.setFont(Theme.FONT_MED);
                empty.setForeground(Theme.TEXT_MUT);
                empty.setBorder(new EmptyBorder(12,0,12,0));
                list.add(empty);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            JLabel err = new JLabel("Could not load: " + e.getMessage());
            err.setFont(Theme.FONT_SMALL);
            err.setForeground(Theme.ACCENT_RED);
            list.add(err);
        }

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildEmpRow(String name, String desig, int idx) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.BG_PANEL);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        UI.AvatarPanel av = new UI.AvatarPanel(name, idx, 34);
        JPanel info = new JPanel(new GridLayout(2,1,0,1));
        info.setBackground(Theme.BG_PANEL);
        JLabel nm = new JLabel(name);
        nm.setFont(Theme.FONT_MED);
        nm.setForeground(Theme.TEXT_PRI);
        JLabel dg = new JLabel(desig);
        dg.setFont(Theme.FONT_SMALL);
        dg.setForeground(Theme.TEXT_MUT);
        info.add(nm); info.add(dg);

        UI.BadgeLabel badge = UI.BadgeLabel.green("Active");

        row.add(av, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    private JPanel buildAnnouncements() {
        JPanel card = UI.card("Announcements");
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.BG_PANEL);

        list.add(buildAnnounce("Holiday Notice", "Office closed on 15th Aug for Independence Day", Theme.ACCENT_BLU));
        list.add(Box.createVerticalStrut(8));
        list.add(buildAnnounce("Payroll Processed", "May 2026 salaries credited successfully", Theme.ACCENT_GRN));
        list.add(Box.createVerticalStrut(8));
        list.add(buildAnnounce("Policy Update", "New WFH policy effective from June 2026", Theme.ACCENT_YLW));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAnnounce(String title, String body, Color accent) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(accent);
                g2.fillRect(0,0,3,getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8,12,8,10));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(accent);
        JLabel b = new JLabel(body);
        b.setFont(Theme.FONT_SMALL);
        b.setForeground(Theme.TEXT_MUT);
        p.add(t); p.add(Box.createVerticalStrut(2)); p.add(b);
        return p;
    }

    private JPanel buildQuickActions() {
        JPanel card = UI.card("Quick Actions");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setBackground(Theme.BG_PANEL);

        UI.DarkButton b1 = UI.DarkButton.primary("Add Employee");
        UI.DarkButton b2 = UI.DarkButton.normal("View All");
        UI.DarkButton b3 = UI.DarkButton.normal("Search");
        UI.DarkButton b4 = UI.DarkButton.normal("Reports");
        UI.DarkButton b5 = UI.DarkButton.normal("Email / Chat");

        b1.addActionListener(e -> parent.navigateTo(1));
        b2.addActionListener(e -> parent.navigateTo(1));
        b3.addActionListener(e -> parent.navigateTo(2));
        b4.addActionListener(e -> parent.navigateTo(3));
        b5.addActionListener(e -> parent.navigateTo(4));

        btns.add(b1); btns.add(b2); btns.add(b3); btns.add(b4); btns.add(b5);
        card.add(btns, BorderLayout.CENTER);
        return card;
    }

    private void loadStats() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override protected int[] doInBackground() throws Exception {
                Connection conn = DBConnection.getConnection();
                int total = 0;
                ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM employee");
                if (rs.next()) total = rs.getInt(1);
                rs.close();
                return new int[]{total, (int)(total * 0.87), (int)(total * 0.07), 2};
            }
            @Override protected void done() {
                try {
                    int[] vals = get();
                    totalEmpVal.setText(String.valueOf(vals[0]));
                    presentVal.setText(String.valueOf(vals[1]));
                    onLeaveVal.setText(String.valueOf(vals[2]));
                    birthdayVal.setText(String.valueOf(vals[3]));
                } catch (Exception ex) {
                    totalEmpVal.setText("—");
                }
            }
        };
        worker.execute();
    }
}
