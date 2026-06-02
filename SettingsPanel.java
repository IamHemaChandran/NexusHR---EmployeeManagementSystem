package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class SettingsPanel extends JPanel {
    private MainFrame parent;

    public SettingsPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        buildUI();
    }

    private void buildUI() {
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Theme.BG_PANEL);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER), new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("  Settings");
        title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);
        topbar.add(title, BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_DEEP);
        content.setBorder(new EmptyBorder(16,16,16,16));

        content.add(buildAppearanceSection());
        content.add(Box.createVerticalStrut(14));
        content.add(buildDbSection());
        content.add(Box.createVerticalStrut(14));
        content.add(buildSmtpSection());
        content.add(Box.createVerticalStrut(14));
        content.add(buildPasswordSection());
        content.add(Box.createVerticalStrut(14));
        content.add(buildAboutSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_DEEP);
        scroll.getViewport().setBackground(Theme.BG_DEEP);

        add(topbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Appearance ────────────────────────────────────────────────────────────
    private JPanel buildAppearanceSection() {
        JPanel card = sectionCard("Appearance");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row.setBackground(Theme.BG_PANEL);

        JLabel lbl = new JLabel("Theme:");
        lbl.setFont(Theme.FONT_MED); lbl.setForeground(Theme.TEXT_SEC);

        UI.DarkButton darkBtn  = UI.DarkButton.normal("☾  Dark Mode");
        UI.DarkButton lightBtn = UI.DarkButton.normal("☀  Light Mode");
        highlightThemeBtn(darkBtn, lightBtn);

        darkBtn.addActionListener(e -> {
            Theme.isDark = true; Theme.refresh();
            highlightThemeBtn(darkBtn, lightBtn);
            refreshWindow();
        });
        lightBtn.addActionListener(e -> {
            Theme.isDark = false; Theme.refresh();
            highlightThemeBtn(darkBtn, lightBtn);
            refreshWindow();
        });

        row.add(lbl); row.add(darkBtn); row.add(lightBtn);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private void highlightThemeBtn(UI.DarkButton dark, UI.DarkButton light) {
        dark.setForeground(Theme.isDark  ? Theme.ACCENT_BLU : Theme.TEXT_SEC);
        light.setForeground(!Theme.isDark ? Theme.ACCENT_BLU : Theme.TEXT_SEC);
    }

    private void refreshWindow() {
        SwingUtilities.invokeLater(() -> {
            SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
        });
    }

    // ── DB Config ─────────────────────────────────────────────────────────────
    private JPanel buildDbSection() {
        JPanel card = sectionCard("Database Connection");

        JPanel form = new JPanel(new GridLayout(0,2,10,10));
        form.setBackground(Theme.BG_PANEL);

        UI.DarkField host = new UI.DarkField("localhost");  host.setText("localhost");
        UI.DarkField port = new UI.DarkField("3306");       port.setText("3306");
        UI.DarkField db   = new UI.DarkField("EmployeeManagement"); db.setText("EmployeeManagement");
        UI.DarkField user = new UI.DarkField("root");       user.setText("root");

        addRow(form,"Host",host); addRow(form,"Port",port);
        addRow(form,"Database",db); addRow(form,"Username",user);

        JLabel statusLbl = new JLabel("Not tested");
        statusLbl.setFont(Theme.FONT_SMALL); statusLbl.setForeground(Theme.TEXT_MUT);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton testBtn = UI.DarkButton.normal("Test Connection");
        testBtn.addActionListener(e -> {
            try {
                Connection conn = DBConnection.getConnection();
                if (!conn.isClosed()) {
                    statusLbl.setForeground(Theme.ACCENT_GRN);
                    statusLbl.setText("✓ Connected to MySQL successfully");
                    UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"DB Connected!",false);
                }
            } catch (SQLException ex) {
                statusLbl.setForeground(Theme.ACCENT_RED);
                statusLbl.setText("✗ " + ex.getMessage());
            }
        });
        btns.add(testBtn); btns.add(statusLbl);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Theme.BG_PANEL);
        inner.add(form, BorderLayout.CENTER);
        inner.add(btns, BorderLayout.SOUTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── SMTP Config ───────────────────────────────────────────────────────────
    private JPanel buildSmtpSection() {
        JPanel card = sectionCard("Email / SMTP Settings");

        JPanel form = new JPanel(new GridLayout(0,2,10,10));
        form.setBackground(Theme.BG_PANEL);

        UI.DarkField smtpHost = new UI.DarkField("smtp.gmail.com");  smtpHost.setText(EmailSender.SMTP_HOST);
        UI.DarkField smtpPort = new UI.DarkField("587");              smtpPort.setText(EmailSender.SMTP_PORT);
        UI.DarkField smtpUser = new UI.DarkField("your@gmail.com");   smtpUser.setText(EmailSender.SMTP_USER);
        UI.DarkPassField smtpPass = new UI.DarkPassField("App Password"); smtpPass.setText(EmailSender.SMTP_PASS);
        UI.DarkField fromName = new UI.DarkField("EMS Pro");          fromName.setText(EmailSender.FROM_NAME);

        addRow(form,"SMTP Host",smtpHost); addRow(form,"Port",smtpPort);
        addRow(form,"Gmail Address",smtpUser); addRow(form,"App Password",smtpPass);
        addRow(form,"Sender Name",fromName);

        JLabel hint = new JLabel("Use a Gmail App Password (not your main password). Enable 2FA first.");
        hint.setFont(new Font("Segoe UI",Font.ITALIC,10)); hint.setForeground(Theme.ACCENT_YLW);

        JLabel statusLbl = new JLabel(""); statusLbl.setFont(Theme.FONT_SMALL); statusLbl.setForeground(Theme.TEXT_MUT);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton saveSmtp = UI.DarkButton.primary("Save & Test");
        saveSmtp.addActionListener(e -> {
            EmailSender.SMTP_HOST = smtpHost.getText().trim();
            EmailSender.SMTP_PORT = smtpPort.getText().trim();
            EmailSender.SMTP_USER = smtpUser.getText().trim();
            EmailSender.SMTP_PASS = new String(smtpPass.getPassword());
            EmailSender.FROM_NAME = fromName.getText().trim();
            EmailSender.saveConfig();
            saveSmtp.setText("Saving..."); saveSmtp.setEnabled(false);
            SwingWorker<Boolean,Void> w = new SwingWorker<>(){
                protected Boolean doInBackground(){ return EmailSender.send(EmailSender.SMTP_USER,"EMS Pro SMTP Test","✓ SMTP is configured correctly!"); }
                protected void done(){
                    try {
                        boolean ok = get();
                        statusLbl.setForeground(ok?Theme.ACCENT_GRN:Theme.ACCENT_YLW);
                        statusLbl.setText(ok?"✓ SMTP working!":"Saved (test failed — check credentials)");
                    } catch(Exception ex){ statusLbl.setText("Error"); }
                    saveSmtp.setText("Save & Test"); saveSmtp.setEnabled(true);
                }
            };
            w.execute();
        });
        btns.add(saveSmtp); btns.add(statusLbl);

        JPanel inner = new JPanel(new BorderLayout(0,6));
        inner.setBackground(Theme.BG_PANEL);
        inner.add(form, BorderLayout.CENTER);
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setBackground(Theme.BG_PANEL);
        bottomRow.add(hint, BorderLayout.NORTH);
        bottomRow.add(btns, BorderLayout.SOUTH);
        inner.add(bottomRow, BorderLayout.SOUTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── Change Password ───────────────────────────────────────────────────────
    private JPanel buildPasswordSection() {
        JPanel card = sectionCard("Change Password");

        JPanel form = new JPanel(new GridLayout(0,2,10,10));
        form.setBackground(Theme.BG_PANEL);

        UI.DarkField userField      = new UI.DarkField("Username");
        UI.DarkPassField curPass    = new UI.DarkPassField("Current password");
        UI.DarkPassField newPass    = new UI.DarkPassField("New password");
        UI.DarkPassField conPass    = new UI.DarkPassField("Confirm new password");

        addRow(form,"Username",userField); addRow(form,"Current Password",curPass);
        addRow(form,"New Password",newPass); addRow(form,"Confirm Password",conPass);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton changeBtn = UI.DarkButton.primary("Update Password");
        changeBtn.addActionListener(e -> {
            String user=userField.getText().trim(), cur=new String(curPass.getPassword());
            String np=new String(newPass.getPassword()), cp=new String(conPass.getPassword());
            if(user.isEmpty()||cur.isEmpty()||np.isEmpty()){JOptionPane.showMessageDialog(this,"All fields required."); return;}
            if(!np.equals(cp)){JOptionPane.showMessageDialog(this,"New passwords do not match."); return;}
            if(np.length()<6){JOptionPane.showMessageDialog(this,"Minimum 6 characters."); return;}
            try{
                Connection conn=DBConnection.getConnection();
                PreparedStatement ps=conn.prepareStatement("UPDATE login SET password=? WHERE username=? AND password=?");
                ps.setString(1,np); ps.setString(2,user); ps.setString(3,cur);
                int rows=ps.executeUpdate(); ps.close();
                if(rows>0){
                    UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"Password updated!",false);
                    curPass.setText(""); newPass.setText(""); conPass.setText("");
                } else JOptionPane.showMessageDialog(this,"Incorrect username or current password.");
            }catch(SQLException ex){JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());}
        });
        btns.add(changeBtn);

        JPanel inner=new JPanel(new BorderLayout()); inner.setBackground(Theme.BG_PANEL);
        inner.add(form,BorderLayout.CENTER); inner.add(btns,BorderLayout.SOUTH);
        card.add(inner,BorderLayout.CENTER);
        return card;
    }

    // ── About ─────────────────────────────────────────────────────────────────
    private JPanel buildAboutSection() {
        JPanel card = sectionCard("About EMS Pro");
        JPanel info = new JPanel(new GridLayout(0,1,0,6)); info.setBackground(Theme.BG_PANEL);

        String[][] rows = {
            {"Version",   "3.0.0"},
            {"Build",     "2026 — Advanced Edition"},
            {"Stack",     "Java Swing + MySQL + SMTP"},
            {"DB Driver", "mysql-connector-j-9.7.0"},
            {"Mail",      "javax.mail (jakarta.mail)"},
            {"Java",      System.getProperty("java.version")},
            {"OS",        System.getProperty("os.name")},
            {"User",      SessionManager.username + " (" + SessionManager.role + ")"},
        };
        for(String[] row:rows){
            JPanel r=new JPanel(new BorderLayout()); r.setBackground(Theme.BG_PANEL);
            r.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER)); r.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
            JLabel k=new JLabel(row[0]); k.setFont(Theme.FONT_SMALL); k.setForeground(Theme.TEXT_MUT); k.setPreferredSize(new Dimension(120,20));
            JLabel v=new JLabel(row[1]); v.setFont(Theme.FONT_MED);   v.setForeground(Theme.TEXT_PRI);
            r.add(k,BorderLayout.WEST); r.add(v,BorderLayout.CENTER); info.add(r);
        }
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton logoutBtn=UI.DarkButton.danger("Logout");
        logoutBtn.addActionListener(e->{DBConnection.close();new LoginFrame().setVisible(true);((JFrame)SwingUtilities.getWindowAncestor(this)).dispose();});
        btns.add(logoutBtn);

        JPanel inner=new JPanel(new BorderLayout()); inner.setBackground(Theme.BG_PANEL);
        inner.add(info,BorderLayout.CENTER); inner.add(btns,BorderLayout.SOUTH);
        card.add(inner,BorderLayout.CENTER);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JPanel sectionCard(String title) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14,16,14,16)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE,9999));
        JLabel t=new JLabel(title); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.TEXT_PRI);
        t.setBorder(new EmptyBorder(0,0,10,0)); card.add(t,BorderLayout.NORTH);
        return card;
    }

    private void addRow(JPanel p, String label, JComponent field) {
        JLabel l=new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        p.add(l); p.add(field);
    }
}
