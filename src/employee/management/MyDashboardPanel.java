package employee.management;

import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;

public class MyDashboardPanel extends JPanel {
    private MainFrame parent;
    private String empId;

    public MyDashboardPanel(MainFrame parent) {
        this.parent = parent;
        this.empId  = SessionManager.empId;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        buildUI();
    }

    private void buildUI() {
        // Topbar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_PANEL);
        top.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,Theme.BORDER),
            new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("  My Dashboard");
        title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);

        // Live clock
        JLabel clock = new JLabel();
        clock.setFont(Theme.FONT_SMALL); clock.setForeground(Theme.TEXT_MUT);
        javax.swing.Timer t = new javax.swing.Timer(1000, e ->
            clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  |  hh:mm:ss a"))));
        t.start();
        clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  |  hh:mm:ss a")));

        top.add(title, BorderLayout.WEST);
        top.add(clock, BorderLayout.EAST);

        // Scroll content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_DEEP);
        content.setBorder(new EmptyBorder(16,16,16,16));

        // Load employee data
        String name="—", desig="—", email="—", phone="—", sal="0", photo="", joined="—";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM employee WHERE empId=?");
            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                name   = nvl(rs.getString("name"),"—");
                desig  = nvl(rs.getString("designation"),"—");
                email  = nvl(rs.getString("email"),"—");
                phone  = nvl(rs.getString("phone"),"—");
                sal    = nvl(rs.getString("salary"),"0");
                photo  = nvl(rs.getString("photo"),"");
                joined = rs.getTimestamp("created_at")!=null
                    ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString() : "—";
            }
            rs.close(); ps.close();
        } catch(SQLException ignored){}

        // ── Hero card ─────────────────────────────────────────────────────────
        JPanel hero = buildHero(name, desig, empId, sal, email, photo, joined);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        content.add(hero);
        content.add(Box.createVerticalStrut(14));

        // ── Stats row ─────────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1,4,10,0));
        statsRow.setBackground(Theme.BG_DEEP);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        int present=0, absent=0, pending=0, approved=0;
        try {
            Connection conn = DBConnection.getConnection();
            ResultSet r1 = conn.createStatement().executeQuery(
                "SELECT status, COUNT(*) c FROM attendance WHERE empId='"+empId+
                "' AND MONTH(date)=MONTH(NOW()) GROUP BY status");
            while(r1.next()){
                String s=r1.getString("status"); int c=r1.getInt("c");
                if("Present".equals(s)||"Late".equals(s)) present+=c;
                else if("Absent".equals(s)) absent+=c;
            }
            r1.close();
            ResultSet r2 = conn.createStatement().executeQuery(
                "SELECT status, COUNT(*) c FROM leaves WHERE empId='"+empId+"' GROUP BY status");
            while(r2.next()){
                String s=r2.getString("status"); int c=r2.getInt("c");
                if("Pending".equals(s)) pending+=c;
                else if("Approved".equals(s)) approved+=c;
            }
            r2.close();
        } catch(SQLException ignored){}

        statsRow.add(statCard("Days Present",  String.valueOf(present), Theme.ACCENT_GRN, "This month"));
        statsRow.add(statCard("Days Absent",   String.valueOf(absent),  Theme.ACCENT_RED, "This month"));
        statsRow.add(statCard("Leave Pending", String.valueOf(pending), Theme.ACCENT_YLW, "Awaiting approval"));
        statsRow.add(statCard("Leave Approved",String.valueOf(approved),Theme.ACCENT_BLU, "This year"));
        content.add(statsRow);
        content.add(Box.createVerticalStrut(14));

        // ── Two columns: Info + Recent attendance ─────────────────────────────
        JPanel cols = new JPanel(new GridLayout(1,2,12,0));
        cols.setBackground(Theme.BG_DEEP);
        cols.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        cols.add(buildInfoCard(name, desig, email, phone, sal, empId, joined));
        cols.add(buildRecentAttendance());
        content.add(cols);
        content.add(Box.createVerticalStrut(14));

        // ── Quick actions ─────────────────────────────────────────────────────
        content.add(buildQuickActions());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHero(String name, String desig, String id, String sal, String email, String photo, String joined) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0,new Color(0x1a,0x2a,0x4a), getWidth(),0, new Color(0x1a,0x14,0x3a));
                g2.setPaint(gp); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(Theme.BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 16));

        // Photo or avatar
        JComponent photoComp;
        if(!photo.isEmpty()){
            File pf=new File(photo);
            if(pf.exists()){
                try{
                    Image img=ImageIO.read(pf).getScaledInstance(72,72,Image.SCALE_SMOOTH);
                    JLabel lbl=new JLabel(new ImageIcon(img));
                    lbl.setBorder(BorderFactory.createLineBorder(Theme.ACCENT_BLU,2));
                    photoComp=lbl;
                }catch(IOException ex){ photoComp=new UI.AvatarPanel(name,0,72); }
            } else photoComp=new UI.AvatarPanel(name,0,72);
        } else photoComp=new UI.AvatarPanel(name,0,72);
        card.add(photoComp);

        // Info
        JPanel info=new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setOpaque(false);
        JLabel nm=new JLabel("Welcome, "+name+"!"); nm.setFont(new Font("Segoe UI",Font.BOLD,16)); nm.setForeground(Theme.TEXT_PRI);
        JLabel dg=new JLabel(desig+" · "+id); dg.setFont(Theme.FONT_MED); dg.setForeground(Theme.ACCENT_BLU);
        JLabel em=new JLabel(email); em.setFont(Theme.FONT_SMALL); em.setForeground(Theme.TEXT_MUT);
        JPanel badges=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2)); badges.setOpaque(false);
        badges.add(UI.BadgeLabel.green("Active"));
        badges.add(UI.BadgeLabel.blue(desig.isEmpty()?"Employee":desig));
        info.add(nm); info.add(Box.createVerticalStrut(2)); info.add(dg); info.add(em); info.add(badges);
        card.add(info);

        // Stats
        JPanel qs=new JPanel(new GridLayout(1,3,16,0)); qs.setOpaque(false);
        qs.add(quickStat("Salary","₹"+fmt(sal),Theme.ACCENT_GRN));
        qs.add(quickStat("Joined",joined,Theme.ACCENT_BLU));
        qs.add(quickStat("Today",LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),Theme.TEXT_SEC));
        card.add(qs);
        return card;
    }

    private JPanel quickStat(String label, String val, Color color){
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false);
        p.setBorder(new EmptyBorder(0,10,0,10));
        JLabel l=new JLabel(label); l.setFont(new Font("Segoe UI",Font.PLAIN,10)); l.setForeground(Theme.TEXT_MUT);
        JLabel v=new JLabel(val);   v.setFont(new Font("Segoe UI",Font.BOLD,12)); v.setForeground(color);
        p.add(l); p.add(Box.createVerticalStrut(2)); p.add(v); return p;
    }

    private JPanel statCard(String label, String val, Color color, String sub){
        JPanel p=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(color); g2.fillRoundRect(0,0,4,getHeight(),4,4);
                g2.setColor(Theme.BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10,16,10,10));
        JLabel l=new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        JLabel v=new JLabel(val);   v.setFont(new Font("Segoe UI",Font.BOLD,20)); v.setForeground(color);
        JLabel s=new JLabel(sub);   s.setFont(new Font("Segoe UI",Font.PLAIN,10)); s.setForeground(Theme.TEXT_MUT);
        p.add(l); p.add(Box.createVerticalStrut(2)); p.add(v); p.add(s); return p;
    }

    private JPanel buildInfoCard(String name, String desig, String email, String phone, String sal, String id, String joined){
        JPanel card=roundCard("My Information");
        JPanel rows=new JPanel(new GridLayout(0,1,0,0)); rows.setBackground(Theme.BG_PANEL);
        String[][] data={{"Employee ID",id},{"Name",name},{"Designation",desig},{"Email",email},{"Phone",phone},{"Salary","₹"+fmt(sal)},{"Joined",joined}};
        for(String[] r:data){
            JPanel row=new JPanel(new BorderLayout(10,0)); row.setBackground(Theme.BG_PANEL);
            row.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
            JLabel k=new JLabel(r[0]); k.setFont(Theme.FONT_SMALL); k.setForeground(Theme.TEXT_MUT); k.setPreferredSize(new Dimension(110,20));
            JLabel v=new JLabel(r[1]); v.setFont(Theme.FONT_MED);   v.setForeground(Theme.TEXT_PRI);
            row.add(k,BorderLayout.WEST); row.add(v,BorderLayout.CENTER); rows.add(row);
        }
        card.add(rows, BorderLayout.CENTER); return card;
    }

    private JPanel buildRecentAttendance(){
        JPanel card=roundCard("Recent Attendance");
        JPanel rows=new JPanel(); rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS)); rows.setBackground(Theme.BG_PANEL);
        try{
            Connection conn=DBConnection.getConnection();
            PreparedStatement ps=conn.prepareStatement(
                "SELECT date,status,checkIn,checkOut FROM attendance WHERE empId=? ORDER BY date DESC LIMIT 7");
            ps.setString(1,empId); ResultSet rs=ps.executeQuery();
            boolean any=false;
            while(rs.next()){
                any=true;
                String date=rs.getString("date"), st=nvl(rs.getString("status"),"Absent");
                String ci=nvl(rs.getString("checkIn"),"—"), co=nvl(rs.getString("checkOut"),"—");
                Color stColor="Present".equals(st)?Theme.ACCENT_GRN:"Absent".equals(st)?Theme.ACCENT_RED:Theme.ACCENT_YLW;
                JPanel row=new JPanel(new BorderLayout(8,0)); row.setBackground(Theme.BG_PANEL);
                row.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
                JLabel d=new JLabel(date); d.setFont(Theme.FONT_SMALL); d.setForeground(Theme.TEXT_SEC); d.setPreferredSize(new Dimension(90,20));
                JLabel s=new JLabel(st);   s.setFont(new Font("Segoe UI",Font.BOLD,11)); s.setForeground(stColor);
                JLabel t=new JLabel(ci.equals("—")?"":ci+" → "+co); t.setFont(Theme.FONT_SMALL); t.setForeground(Theme.TEXT_MUT);
                row.add(d,BorderLayout.WEST); row.add(s,BorderLayout.CENTER); row.add(t,BorderLayout.EAST);
                rows.add(row);
            }
            rs.close(); ps.close();
            if(!any){ JLabel e=new JLabel("No attendance records yet"); e.setFont(Theme.FONT_SMALL); e.setForeground(Theme.TEXT_MUT); rows.add(e); }
        }catch(SQLException ignored){}
        card.add(rows,BorderLayout.CENTER); return card;
    }

    private JPanel buildQuickActions(){
        JPanel card=roundCard("Quick Actions");
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton att=UI.DarkButton.normal("📅 My Attendance");
        att.addActionListener(e->parent.navigateTo(2));
        UI.DarkButton leave=UI.DarkButton.normal("📋 Apply Leave");
        leave.addActionListener(e->parent.navigateTo(3));
        UI.DarkButton chat=UI.DarkButton.normal("✉ Email / Chat");
        chat.addActionListener(e->parent.navigateTo(6));
        UI.DarkButton notif=UI.DarkButton.normal("🔔 Notifications");
        notif.addActionListener(e->parent.navigateTo(7));
        btns.add(att); btns.add(leave); btns.add(chat); btns.add(notif);
        card.add(btns,BorderLayout.CENTER); return card;
    }

    private JPanel roundCard(String title){
        JPanel card=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12,14,12,14));
        JLabel t=new JLabel(title); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.TEXT_PRI);
        t.setBorder(new EmptyBorder(0,0,8,0)); card.add(t,BorderLayout.NORTH);
        return card;
    }

    private String nvl(String s,String d){return(s==null||s.isEmpty())?d:s;}
    private String fmt(String s){try{return String.format("%,d",Long.parseLong(s));}catch(Exception e){return s;}}
}