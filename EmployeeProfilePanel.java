package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.sql.*;
import javax.imageio.*;

public class EmployeeProfilePanel extends JPanel {
    private MainFrame parent;
    private String empId;

    public EmployeeProfilePanel(MainFrame parent, String empId) {
        this.parent = parent;
        this.empId  = empId;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        loadAndBuild();
    }

    private void loadAndBuild() {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM employee WHERE empId=?");
            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) buildUI(rs);
            else showEmpty();
            rs.close(); ps.close();
        } catch (SQLException e) {
            JLabel err = new JLabel("Error loading profile: " + e.getMessage());
            err.setForeground(Theme.ACCENT_RED); err.setFont(Theme.FONT_MED);
            add(err, BorderLayout.CENTER);
        }
    }

    private void buildUI(ResultSet rs) throws SQLException {
        String name  = nvl(rs.getString("name"),  "Unknown");
        String desig = nvl(rs.getString("designation"), "—");
        String email = nvl(rs.getString("email"),  "—");
        String phone = nvl(rs.getString("phone"),  "—");
        String dept  = nvl(rs.getString("education"), "—");
        String sal   = nvl(rs.getString("salary"), "0");
        String addr  = nvl(rs.getString("address"), "—");
        String dob   = nvl(rs.getString("dob"),    "—");
        String aadh  = nvl(rs.getString("aadhaar"), "—");
        String fname = nvl(rs.getString("fname"),  "—");
        String photo = nvl(rs.getString("photo"),  "");
        String joined= rs.getTimestamp("created_at")!=null
            ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString() : "—";

        // ── Topbar ────────────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_PANEL);
        top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER), new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("  Employee Profile");
        title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        right.setBackground(Theme.BG_PANEL);
        UI.DarkButton backBtn = UI.DarkButton.normal("← Back");
        backBtn.addActionListener(e -> parent.navigateTo(1));
        right.add(backBtn);
        top.add(title, BorderLayout.WEST); top.add(right, BorderLayout.EAST);

        // ── Scroll content ────────────────────────────────────────────────────
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_DEEP);
        content.setBorder(new EmptyBorder(16,16,16,16));

        // Hero card
        JPanel hero = buildHeroCard(name, desig, empId, email, phone, sal, photo, joined);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        content.add(hero);
        content.add(Box.createVerticalStrut(14));

        // Two-column details
        JPanel cols = new JPanel(new GridLayout(1,2,12,0));
        cols.setBackground(Theme.BG_DEEP);
        cols.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        cols.add(buildInfoCard("Personal Information", new String[][]{
            {"Employee ID",  empId},
            {"Full Name",    name},
            {"Father's Name",fname},
            {"Date of Birth",dob},
            {"Aadhaar",      aadh},
            {"Education",    dept},
        }));
        cols.add(buildInfoCard("Job & Contact", new String[][]{
            {"Designation",  desig},
            {"Salary",       "₹"+fmt(sal)},
            {"Email",        email},
            {"Phone",        phone},
            {"Address",      addr},
            {"Joined",       joined},
        }));
        content.add(cols);
        content.add(Box.createVerticalStrut(14));

        // Attendance summary
        content.add(buildAttendanceSummary());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeroCard(String name, String desig, String id, String email,
                                  String phone, String sal, String photo, String joined) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient background
                GradientPaint gp = new GradientPaint(0,0,Theme.BTN_BLU_BG,getWidth(),0,new Color(0x1a,0x14,0x3a));
                g2.setPaint(gp); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(Theme.BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));

        // Avatar or photo
        if (!photo.isEmpty()) {
            File pf = new File(photo);
            if (pf.exists()) {
                try {
                    BufferedImage img = ImageIO.read(pf);
                    Image scaled = img.getScaledInstance(70,70,Image.SCALE_SMOOTH);
                    JLabel imgLbl = new JLabel(new ImageIcon(scaled));
                    imgLbl.setBorder(BorderFactory.createLineBorder(Theme.ACCENT_BLU, 2));
                    card.add(imgLbl);
                } catch (IOException ignored) {
                    card.add(new UI.AvatarPanel(name,0,70));
                }
            } else card.add(new UI.AvatarPanel(name,0,70));
        } else card.add(new UI.AvatarPanel(name,0,70));

        // Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nm = new JLabel(name); nm.setFont(new Font("Segoe UI",Font.BOLD,18)); nm.setForeground(Theme.TEXT_PRI);
        JLabel dg = new JLabel(desig); dg.setFont(Theme.FONT_MED); dg.setForeground(Theme.ACCENT_BLU);
        JLabel eid = new JLabel(id);   eid.setFont(Theme.FONT_SMALL); eid.setForeground(Theme.TEXT_MUT);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4)); badges.setOpaque(false);
        badges.add(UI.BadgeLabel.green("Active"));
        badges.add(UI.BadgeLabel.blue(desig.isEmpty()?"Employee":desig));

        info.add(nm); info.add(Box.createVerticalStrut(2));
        info.add(dg); info.add(eid); info.add(badges);
        card.add(info);

        // Quick stats
        JPanel stats = new JPanel(new GridLayout(1,3,16,0));
        stats.setOpaque(false);
        stats.add(quickStat("Salary","₹"+fmt(sal),Theme.ACCENT_GRN));
        stats.add(quickStat("Email",email.length()>18?email.substring(0,16)+"..":email,Theme.ACCENT_BLU));
        stats.add(quickStat("Joined",joined,Theme.TEXT_SEC));
        card.add(stats);

        return card;
    }

    private JPanel quickStat(String label, String val, Color color) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false); p.setBorder(new EmptyBorder(0,10,0,10));
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI",Font.PLAIN,10)); l.setForeground(Theme.TEXT_MUT);
        JLabel v = new JLabel(val);   v.setFont(new Font("Segoe UI",Font.BOLD,12)); v.setForeground(color);
        p.add(l); p.add(Box.createVerticalStrut(2)); p.add(v);
        return p;
    }

    private JPanel buildInfoCard(String title, String[][] rows) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14,16,14,16));

        JLabel t = new JLabel(title); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.TEXT_PRI);
        card.add(t); card.add(Box.createVerticalStrut(10));

        for (String[] row : rows) {
            JPanel r = new JPanel(new BorderLayout(10,0));
            r.setOpaque(false); r.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            JLabel k = new JLabel(row[0]); k.setFont(Theme.FONT_SMALL); k.setForeground(Theme.TEXT_MUT); k.setPreferredSize(new Dimension(120,20));
            JLabel v = new JLabel(row[1]); v.setFont(Theme.FONT_MED);   v.setForeground(Theme.TEXT_PRI);
            r.add(k, BorderLayout.WEST); r.add(v, BorderLayout.CENTER);
            card.add(r); card.add(Box.createVerticalStrut(4));
        }
        return card;
    }

    private JPanel buildAttendanceSummary() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14,16,14,16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel t = new JLabel("Attendance This Month"); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.TEXT_PRI);
        card.add(t); card.add(Box.createVerticalStrut(10));

        int present=0, absent=0, late=0, leave=0;
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT status, COUNT(*) as cnt FROM attendance WHERE empId=? AND MONTH(date)=MONTH(NOW()) GROUP BY status");
            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String st = rs.getString("status"); int cnt = rs.getInt("cnt");
                if ("Present".equals(st)) present=cnt;
                else if ("Absent".equals(st)) absent=cnt;
                else if ("Late".equals(st)) late=cnt;
                else if ("Leave".equals(st)) leave=cnt;
            }
            rs.close(); ps.close();
        } catch (SQLException ignored) {}

        JPanel stats = new JPanel(new GridLayout(1,4,12,0)); stats.setOpaque(false);
        stats.add(attStat("Present", present, Theme.ACCENT_GRN));
        stats.add(attStat("Absent",  absent,  Theme.ACCENT_RED));
        stats.add(attStat("Late",    late,    Theme.ACCENT_YLW));
        stats.add(attStat("On Leave",leave,   Theme.ACCENT_BLU));
        card.add(stats);
        return card;
    }

    private JPanel attStat(String label, int val, Color color) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel v = new JLabel(String.valueOf(val)); v.setFont(new Font("Segoe UI",Font.BOLD,22)); v.setForeground(color);
        JLabel l = new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        p.add(v); p.add(l);
        return p;
    }

    private void showEmpty() {
        JLabel l = new JLabel("Employee not found: " + empId, SwingConstants.CENTER);
        l.setFont(Theme.FONT_MED); l.setForeground(Theme.TEXT_MUT);
        add(l, BorderLayout.CENTER);
    }

    private String nvl(String s, String d) { return (s==null||s.isEmpty())?d:s; }
    private String fmt(String s) { try { return String.format("%,d",Long.parseLong(s)); } catch(Exception e){ return s; } }
}
