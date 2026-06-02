package employee.management;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

public class LoginFrame extends JFrame {
    private UI.DarkField usernameField;
    private UI.DarkPassField passwordField;
    private JLabel statusLabel;

    public LoginFrame() {
        // Always start in dark mode
        Theme.isDark = true;
        Theme.refresh();
        setTitle("EMS Pro — Login");
        setSize(440, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setBackground(Theme.BG_DEEP);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG_DEEP);
        setContentPane(root);

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36,40,36,40));
        card.setPreferredSize(new Dimension(360,440));

        // Logo
        JPanel logoCircle = new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BTN_BLU_BG); g2.fillOval(0,0,48,48);
                g2.setColor(Theme.ACCENT_BLU); g2.setFont(new Font("Segoe UI",Font.BOLD,20));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString("E",(48-fm.stringWidth("E"))/2,(48+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        logoCircle.setOpaque(false); logoCircle.setPreferredSize(new Dimension(48,48));
        logoCircle.setMaximumSize(new Dimension(48,48)); logoCircle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logo = new JLabel("EMS Pro");
        logo.setFont(new Font("Segoe UI",Font.BOLD,22)); logo.setForeground(Theme.TEXT_PRI);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Employee Management System");
        sub.setFont(Theme.FONT_SMALL); sub.setForeground(Theme.TEXT_MUT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER); sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));

        // Role selector
        JLabel roleLbl = new JLabel("Login as");
        roleLbl.setFont(Theme.FONT_SMALL); roleLbl.setForeground(Theme.TEXT_MUT);
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        UI.DarkCombo roleCombo = new UI.DarkCombo(new String[]{"Admin","HR","Employee"});
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(Theme.FONT_SMALL); userLbl.setForeground(Theme.TEXT_MUT);
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new UI.DarkField("Enter username");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(Theme.FONT_SMALL); passLbl.setForeground(Theme.TEXT_MUT);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new UI.DarkPassField("Enter password");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.FONT_SMALL); statusLabel.setForeground(Theme.ACCENT_RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        UI.DarkButton loginBtn = UI.DarkButton.primary("Sign In →");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> doLogin(roleCombo.getSelectedIndex()));

        passwordField.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER) doLogin(roleCombo.getSelectedIndex());}
        });

        // Live clock label
        JLabel clockLabel = new JLabel("", SwingConstants.CENTER);
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clockLabel.setForeground(Theme.TEXT_MUT);
        clockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, e -> {
            clockLabel.setText(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  |  hh:mm:ss a")));
        });
        clockTimer.start();
        clockLabel.setText(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  |  hh:mm:ss a")));

        card.add(logoCircle); card.add(Box.createVerticalStrut(6));
        card.add(logo);       card.add(Box.createVerticalStrut(2));
        card.add(sub);        card.add(Box.createVerticalStrut(4));
        card.add(clockLabel); card.add(Box.createVerticalStrut(14));
        card.add(sep);        card.add(Box.createVerticalStrut(14));
        card.add(roleLbl);    card.add(Box.createVerticalStrut(4));
        card.add(roleCombo);  card.add(Box.createVerticalStrut(10));
        card.add(userLbl);    card.add(Box.createVerticalStrut(4));
        card.add(usernameField); card.add(Box.createVerticalStrut(10));
        card.add(passLbl);    card.add(Box.createVerticalStrut(4));
        card.add(passwordField); card.add(Box.createVerticalStrut(6));
        card.add(statusLabel); card.add(Box.createVerticalStrut(6));
        card.add(loginBtn);

        root.add(card);
    }

    private void doLogin(int roleIdx) {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if(user.isEmpty()||pass.isEmpty()){statusLabel.setText("Please enter username and password"); return;}

        String[] roles = {"admin","hr","employee"};
        String selectedRole = roles[roleIdx];

        try {
            Connection conn = DBConnection.getConnection();
            // Always reset session first
            SessionManager.username = "";
            SessionManager.role     = "admin";
            SessionManager.empId    = "";

            // Fetch login row including empId column
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, role, empId FROM login WHERE username=? AND password=?");
            ps.setString(1,user); ps.setString(2,pass);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                String dbRole  = rs.getString("role");
                String dbEmpId = rs.getString("empId");
                if(dbRole==null||dbRole.isEmpty()) dbRole = selectedRole;
                if(dbEmpId==null) dbEmpId = "";

                // ── Role mismatch check ──────────────────────────────────────
                // Selected role must match the actual DB role
                if(!dbRole.equals(selectedRole)){
                    rs.close(); ps.close();
                    statusLabel.setForeground(Theme.ACCENT_RED);
                    statusLabel.setText("Wrong role selected. Please select: " + dbRole.toUpperCase());
                    return;
                }

                SessionManager.username = user;
                SessionManager.role     = dbRole;
                SessionManager.empId    = dbEmpId;

                // If employee but empId still blank, try matching employee table
                if("employee".equals(dbRole) && SessionManager.empId.isEmpty()){
                    PreparedStatement ep = conn.prepareStatement(
                        "SELECT empId FROM employee WHERE email=? OR empId=? LIMIT 1");
                    ep.setString(1,user); ep.setString(2,user);
                    ResultSet er = ep.executeQuery();
                    if(er.next()) SessionManager.empId = er.getString("empId");
                    er.close(); ep.close();
                }

                rs.close(); ps.close();
                NotificationManager.add("Login","Welcome back, "+user+"!","info");
                String displayName = user.substring(0,1).toUpperCase()+user.substring(1);
                new MainFrame(displayName).setVisible(true);
                dispose();
            } else {
                rs.close(); ps.close();
                statusLabel.setForeground(Theme.ACCENT_RED);
                statusLabel.setText("Invalid username or password");
            }
        } catch(SQLException e){
            statusLabel.setForeground(Theme.ACCENT_YLW);
            statusLabel.setText("DB error: "+e.getMessage());
        }
    }
}