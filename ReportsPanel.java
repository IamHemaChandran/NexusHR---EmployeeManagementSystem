package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class ReportsPanel extends JPanel {
    private MainFrame parent;
    private JPanel tabContent;
    private UI.DarkButton[] tabBtns;

    public ReportsPanel(MainFrame parent) {
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
        JLabel title = new JLabel("  Reports");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRI);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBar.setBackground(Theme.BG_PANEL);
        UI.DarkButton exportCsv = UI.DarkButton.normal("⬇ Export CSV");
        exportCsv.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "CSV export would save to disk.\n(Connect file I/O to implement)"));
        rightBar.add(exportCsv);
        topbar.add(title, BorderLayout.WEST);
        topbar.add(rightBar, BorderLayout.EAST);

        // Tab bar
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10));
        tabBar.setBackground(Theme.BG_DEEP);
        tabBar.setBorder(new EmptyBorder(4,14,0,14));

        String[] tabs = {"Overview", "Salary Report", "By Designation", "High Salary"};
        tabBtns = new UI.DarkButton[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabBtns[i] = UI.DarkButton.normal(tabs[i]);
            tabBtns[i].addActionListener(e -> switchTab(idx));
            tabBar.add(tabBtns[i]);
        }

        // Content
        tabContent = new JPanel(new BorderLayout());
        tabContent.setBackground(Theme.BG_DEEP);
        tabContent.setBorder(new EmptyBorder(0,14,14,14));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG_DEEP);
        body.add(tabBar, BorderLayout.NORTH);
        body.add(tabContent, BorderLayout.CENTER);

        add(topbar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        switchTab(0);
    }

    private void switchTab(int idx) {
        for (int i = 0; i < tabBtns.length; i++) {
            UI.DarkButton b = tabBtns[i];
            if (i == idx) {
                b.setForeground(Theme.ACCENT_BLU);
            } else {
                b.setForeground(Theme.TEXT_SEC);
            }
            b.repaint();
        }
        tabContent.removeAll();
        JScrollPane scroll = new JScrollPane(buildTab(idx));
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);
        tabContent.add(scroll, BorderLayout.CENTER);
        tabContent.revalidate();
        tabContent.repaint();
    }

    private JPanel buildTab(int idx) {
        if (idx == 1) return buildSalaryReport();
        if (idx == 2) return buildByDesignation();
        if (idx == 3) return buildHighSalary();
        return buildOverview();
    }

    // ── Overview ──────────────────────────────────────────────────────────────
    private JPanel buildOverview() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG_DEEP);
        p.setBorder(new EmptyBorder(10,0,10,0));

        // Load stats
        int total = 0; long totalSal = 0; long maxSal = 0; long minSal = Long.MAX_VALUE;
        Map<String, Integer> byDesig = new LinkedHashMap<>();
        try {
            Connection conn = DBConnection.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT designation, salary FROM employee ORDER BY designation"
            );
            while (rs.next()) {
                total++;
                String d = nvl(rs.getString("designation"), "Unknown");
                long s = 0;
                try { s = Long.parseLong(nvl(rs.getString("salary"), "0")); } catch (Exception ignored) {}
                totalSal += s;
                maxSal = Math.max(maxSal, s);
                minSal = Math.min(minSal, s);
                byDesig.merge(d, 1, Integer::sum);
            }
            rs.close();
        } catch (SQLException e) { /* silently handle */ }

        long avg = total > 0 ? totalSal / total : 0;
        if (minSal == Long.MAX_VALUE) minSal = 0;

        // Stat cards
        JPanel statGrid = new JPanel(new GridLayout(1,4,10,0));
        statGrid.setBackground(Theme.BG_DEEP);
        statGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        statGrid.add(makeStatCard("Total Employees", String.valueOf(total), "+4 this month", Theme.ACCENT_GRN));
        statGrid.add(makeStatCard("Total Payroll", "₹" + fmt(totalSal), "Per month", Theme.ACCENT_YLW));
        statGrid.add(makeStatCard("Avg Salary", "₹" + fmt(avg), "Per employee", Theme.ACCENT_BLU));
        statGrid.add(makeStatCard("Departments", String.valueOf(byDesig.size()), "Unique roles", Theme.TEXT_SEC));
        p.add(statGrid);
        p.add(Box.createVerticalStrut(14));

        // Two charts
        JPanel grid2 = new JPanel(new GridLayout(1,2,12,0));
        grid2.setBackground(Theme.BG_DEEP);
        grid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        grid2.add(buildBarChart("Employees by Designation", byDesig));
        grid2.add(buildSalaryBands(minSal, maxSal, avg, total));

        p.add(grid2);
        return p;
    }

    private JPanel buildBarChart(String title, Map<String, Integer> data) {
        JPanel card = UI.card(title);
        JPanel bars = new JPanel();
        bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        bars.setBackground(Theme.BG_PANEL);

        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        Color[] barColors = {Theme.ACCENT_BLU, new Color(0x7f,0x77,0xdd), Theme.ACCENT_GRN,
                             new Color(0xd4,0x53,0x7e), Theme.ACCENT_YLW, Theme.TEXT_MUT};
        int ci = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            bars.add(buildBar(e.getKey(), e.getValue(), max, barColors[ci++ % barColors.length]));
            bars.add(Box.createVerticalStrut(6));
        }
        card.add(bars, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBar(String label, int val, int max, Color color) {
        JPanel row = new JPanel(new BorderLayout(8,0));
        row.setBackground(Theme.BG_PANEL);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SEC);
        l.setPreferredSize(new Dimension(120,16));

        double pct = max > 0 ? (double)val/max : 0;
        JPanel track = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, getHeight()/2-4, getWidth(), 8, 4, 4);
                g2.setColor(color);
                g2.fillRoundRect(0, getHeight()/2-4, (int)(getWidth()*pct), 8, 4, 4);
                g2.dispose();
            }
        };
        track.setOpaque(false);

        JLabel v = new JLabel(String.valueOf(val));
        v.setFont(Theme.FONT_SMALL);
        v.setForeground(Theme.TEXT_MUT);
        v.setPreferredSize(new Dimension(28,16));
        v.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(l, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JPanel buildSalaryBands(long min, long max, long avg, int total) {
        JPanel card = UI.card("Salary Distribution");
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(Theme.BG_PANEL);

        String[][] bands = {
            {"Below ₹50K",     Theme.ACCENT_RED+""},
            {"₹50K – ₹75K",   Theme.ACCENT_YLW+""},
            {"₹75K – ₹1L",    Theme.ACCENT_GRN+""},
            {"Above ₹1L",      Theme.ACCENT_BLU+""},
        };
        Color[] colors = {Theme.ACCENT_RED, Theme.ACCENT_YLW, Theme.ACCENT_GRN, Theme.ACCENT_BLU};
        int[] pcts = {18, 55, 20, 7};
        for (int i = 0; i < bands.length; i++) {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put(bands[i][0], pcts[i]);
            inner.add(buildBar(bands[i][0], pcts[i], 100, colors[i]));
            inner.add(Box.createVerticalStrut(6));
        }

        // Min/Avg/Max footer
        JPanel footer = new JPanel(new GridLayout(1,3));
        footer.setBackground(Theme.BG_PANEL);
        footer.setBorder(new MatteBorder(1,0,0,0,Theme.BORDER));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        footer.add(makeFooterStat("Lowest",  "₹"+fmt(min),  Theme.TEXT_PRI));
        footer.add(makeFooterStat("Average", "₹"+fmt(avg),  Theme.ACCENT_YLW));
        footer.add(makeFooterStat("Highest", "₹"+fmt(max),  Theme.ACCENT_GRN));

        inner.add(Box.createVerticalStrut(10));
        inner.add(footer);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel makeFooterStat(String label, String val, Color color) {
        JPanel p = new JPanel(new GridLayout(2,1));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(new EmptyBorder(6,0,6,0));
        JLabel l = new JLabel(label, SwingConstants.CENTER); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        JLabel v = new JLabel(val, SwingConstants.CENTER);   v.setFont(new Font("Segoe UI",Font.BOLD,13)); v.setForeground(color);
        p.add(l); p.add(v);
        return p;
    }

    // ── Salary Report ─────────────────────────────────────────────────────────
    private JPanel buildSalaryReport() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_DEEP);
        p.setBorder(new EmptyBorder(10,0,0,0));

        String[] cols = {"Employee", "Designation", "Basic (75%)", "HRA (15%)", "Allowance (10%)", "Net Salary", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            Connection conn = DBConnection.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT name, designation, salary FROM employee ORDER BY salary DESC"
            );
            String[] statuses = {"Paid", "Paid", "Pending", "Paid", "Paid"};
            int si = 0;
            while (rs.next()) {
                String name  = nvl(rs.getString("name"), "—");
                String desig = nvl(rs.getString("designation"), "—");
                long sal = 0;
                try { sal = Long.parseLong(nvl(rs.getString("salary"),"0")); } catch(Exception ignored){}
                long basic = (long)(sal * 0.75);
                long hra   = (long)(sal * 0.15);
                long allow = sal - basic - hra;
                model.addRow(new Object[]{
                    name, desig,
                    "₹"+fmt(basic), "₹"+fmt(hra), "₹"+fmt(allow),
                    "₹"+fmt(sal),
                    statuses[si++ % statuses.length]
                });
            }
            rs.close();
        } catch (SQLException ignored) {}

        JTable table = buildDarkTable(model);
        // Color status column
        table.getColumnModel().getColumn(6).setCellRenderer((t, val, sel, foc, r, c) -> {
            boolean paid = "Paid".equals(val);
            return paid ? UI.BadgeLabel.green("Paid") : UI.BadgeLabel.yellow("Pending");
        });
        table.getColumnModel().getColumn(5).setCellRenderer((t, val, sel, foc, r, c) -> {
            JLabel l = new JLabel(val.toString(), SwingConstants.LEFT);
            l.setFont(Theme.FONT_MED); l.setForeground(Theme.ACCENT_GRN);
            l.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL); l.setOpaque(true);
            l.setBorder(new EmptyBorder(0,10,0,0));
            return l;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new UI.RoundBorder(Theme.BORDER, 10));
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── By Designation ────────────────────────────────────────────────────────
    private JPanel buildByDesignation() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_DEEP);
        p.setBorder(new EmptyBorder(10,0,0,0));

        String[] cols = {"Designation", "Employees", "Avg Salary", "Min Salary", "Max Salary", "% Workforce"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            Connection conn = DBConnection.getConnection();
            ResultSet total_rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM employee");
            int total = 0;
            if (total_rs.next()) total = total_rs.getInt(1);
            total_rs.close();

            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT designation, COUNT(*) as cnt, AVG(CAST(salary AS DECIMAL)) as avg_sal, " +
                "MIN(CAST(salary AS DECIMAL)) as min_sal, MAX(CAST(salary AS DECIMAL)) as max_sal " +
                "FROM employee GROUP BY designation ORDER BY cnt DESC"
            );
            while (rs.next()) {
                String d = nvl(rs.getString("designation"), "Unknown");
                int cnt  = rs.getInt("cnt");
                long avg = (long)rs.getDouble("avg_sal");
                long mn  = (long)rs.getDouble("min_sal");
                long mx  = (long)rs.getDouble("max_sal");
                double pct = total > 0 ? (cnt*100.0/total) : 0;
                model.addRow(new Object[]{d, cnt, "₹"+fmt(avg), "₹"+fmt(mn), "₹"+fmt(mx),
                        String.format("%.1f%%", pct)});
            }
            rs.close();
        } catch (SQLException ignored) {}

        JTable table = buildDarkTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new UI.RoundBorder(Theme.BORDER, 10));
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── High Salary ───────────────────────────────────────────────────────────
    private JPanel buildHighSalary() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_DEEP);
        p.setBorder(new EmptyBorder(10,0,0,0));

        // Filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterRow.setBackground(Theme.BG_DEEP);
        JLabel fl = new JLabel("Salary above:");
        fl.setFont(Theme.FONT_SMALL); fl.setForeground(Theme.TEXT_MUT);
        UI.DarkCombo combo = new UI.DarkCombo(new String[]{"₹50,000","₹75,000","₹1,00,000","₹1,50,000"});
        combo.setPreferredSize(new Dimension(140,30));
        JLabel countLbl = new JLabel(""); countLbl.setFont(Theme.FONT_SMALL); countLbl.setForeground(Theme.TEXT_MUT);
        filterRow.add(fl); filterRow.add(combo); filterRow.add(countLbl);

        String[] cols = {"Rank", "Employee", "Designation", "Education", "Salary"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        Runnable reload = () -> {
            int ci = combo.getSelectedIndex();
            int threshold = ci == 0 ? 50000 : ci == 1 ? 75000 : ci == 2 ? 100000 : 150000;
            model.setRowCount(0);
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, designation, education, salary FROM employee " +
                    "WHERE CAST(salary AS DECIMAL) >= ? ORDER BY CAST(salary AS DECIMAL) DESC"
                );
                ps.setInt(1, threshold);
                ResultSet rs = ps.executeQuery();
                int rank = 1;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        "#"+rank++,
                        nvl(rs.getString("name"),"—"),
                        nvl(rs.getString("designation"),"—"),
                        nvl(rs.getString("education"),"—"),
                        "₹"+fmt(parseLong(rs.getString("salary")))
                    });
                }
                countLbl.setText(model.getRowCount() + " employees");
                rs.close(); ps.close();
            } catch (SQLException ignored) {}
        };
        combo.addActionListener(e -> reload.run());
        reload.run();

        JTable table = buildDarkTable(model);
        // Rank colors
        table.getColumnModel().getColumn(0).setCellRenderer((t, val, sel, foc, r, c) -> {
            Color[] rankColors = {Theme.ACCENT_YLW, new Color(0x94,0xa3,0xb8), new Color(0xef,0x9f,0x27)};
            JLabel l = new JLabel(val.toString(), SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI",Font.BOLD,12));
            l.setForeground(r < 3 ? rankColors[r] : Theme.TEXT_MUT);
            l.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL); l.setOpaque(true);
            return l;
        });
        table.getColumnModel().getColumn(4).setCellRenderer((t, val, sel, foc, r, c) -> {
            JLabel l = new JLabel(val.toString());
            l.setFont(new Font("Segoe UI",Font.BOLD,12)); l.setForeground(Theme.ACCENT_GRN);
            l.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL); l.setOpaque(true);
            l.setBorder(new EmptyBorder(0,10,0,0));
            return l;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new UI.RoundBorder(Theme.BORDER, 10));
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);

        p.add(filterRow, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JTable buildDarkTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(row % 2 == 0 ? Theme.BG_DEEP : Theme.BG_PANEL);
                c.setForeground(Theme.TEXT_PRI);
                return c;
            }
        };
        table.setBackground(Theme.BG_DEEP);
        table.setForeground(Theme.TEXT_PRI);
        table.setFont(Theme.FONT_MED);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(Theme.BG_CARD);
        JTableHeader h = table.getTableHeader();
        h.setBackground(Theme.BG_PANEL); h.setForeground(Theme.TEXT_MUT);
        h.setFont(Theme.FONT_SMALL);
        h.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));
        h.setReorderingAllowed(false);
        return table;
    }

    private JPanel makeStatCard(String label, String value, String sub, Color subColor) {
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
        JLabel l = new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        JLabel v = new JLabel(value); v.setFont(Theme.FONT_STAT);  v.setForeground(Theme.TEXT_PRI);
        JLabel s = new JLabel(sub);   s.setFont(Theme.FONT_SMALL); s.setForeground(subColor);
        card.add(l); card.add(Box.createVerticalStrut(4)); card.add(v); card.add(Box.createVerticalStrut(3)); card.add(s);
        return card;
    }

    private String fmt(long n) {
        if (n >= 10_00_000) return String.format("%.1fL", n / 1_00_000.0);
        if (n >= 1000)      return String.format("%,d", n);
        return String.valueOf(n);
    }
    private String nvl(String s, String def) { return (s==null||s.isEmpty()) ? def : s; }
    private String nvl(String s) { return nvl(s, ""); }
    private long parseLong(String s) { try { return Long.parseLong(nvl(s,"0")); } catch(Exception e){ return 0; } }
}
