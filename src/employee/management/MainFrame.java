package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private JPanel contentArea;
    private String adminName;
    private JPanel[] navItems;
    private int activeNav = 0;
    private JLabel notifBadge;
    private JLabel themeLabel;

    // Nav items depend on role
    private String[] navLabels;
    private String[] navIcons;
    private int[]    navTargets; // panel indices

    public MainFrame(String adminName) {
        this.adminName = adminName;
        setTitle("EMS Pro");
        setSize(1150,720);
        setMinimumSize(new Dimension(900,600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildNav();
        buildUI();
    }

    private void buildNav() {
        if(SessionManager.isAdmin()){
            navLabels  = new String[]{"Dashboard","Employees","Attendance","Leave Mgmt","Search","Reports","Email/Chat","Notifications","Settings"};
            navIcons   = new String[]{"\u2302","\u2603","\u25A3","\u2709","\u2315","\u25A6","\u2709","\u25CF","\u2699"};
            navTargets = new int[]{    0,         1,         2,         3,         4,        5,        6,         7,        8};
        } else if(SessionManager.isHR()){
            navLabels  = new String[]{"Dashboard","Employees","Attendance","Leave Mgmt","Reports","Email/Chat","Notifications"};
            navIcons   = new String[]{"\u2302","\u2603","\u25A3","\u2709","\u25A6","\u2709","\u25CF"};
            navTargets = new int[]{    0,         1,         2,         3,         5,        6,        7};
        } else {
            navLabels  = new String[]{"My Dashboard","My Attendance","My Leaves","Email/Chat","Notifications"};
            navIcons   = new String[]{"\u2302","\u25A3","\u2709","\u2709","\u25CF"};
            navTargets = new int[]{    0,         2,         3,         6,         7};
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_DEEP);
        setContentPane(root);

        // ── Sidebar ──────────────────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.BG_PANEL);
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(210,0));
        sidebar.setBorder(new MatteBorder(0,0,0,1,Theme.BORDER));

        // Logo
        JPanel logoArea = new JPanel(new BorderLayout());
        logoArea.setBackground(Theme.BG_PANEL); logoArea.setBorder(new EmptyBorder(16,16,14,16));
        JLabel logoTitle=new JLabel("EMS Pro"); logoTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); logoTitle.setForeground(Theme.TEXT_PRI);
        JLabel logoSub=new JLabel(SessionManager.role.toUpperCase()+" PANEL"); logoSub.setFont(new Font("Segoe UI",Font.PLAIN,10)); logoSub.setForeground(Theme.ACCENT_BLU);
        logoArea.add(logoTitle,BorderLayout.NORTH); logoArea.add(logoSub,BorderLayout.SOUTH);

        JPanel logoDivider=new JPanel(); logoDivider.setBackground(Theme.BORDER);
        logoDivider.setPreferredSize(new Dimension(210,1)); logoDivider.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));

        sidebar.add(logoArea); sidebar.add(logoDivider); sidebar.add(Box.createVerticalStrut(8));

        navItems = new JPanel[navLabels.length];
        for(int i=0;i<navLabels.length;i++){
            final int idx=i;
            navItems[i]=createNavItem(navIcons[i],navLabels[i],i==0);
            navItems[i].addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){switchNav(idx);}
            });
            sidebar.add(navItems[i]);
        }

        sidebar.add(Box.createVerticalGlue());

        // Theme toggle
        JPanel themeRow=new JPanel(new FlowLayout(FlowLayout.LEFT,12,6));
        themeRow.setBackground(Theme.BG_PANEL); themeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        themeLabel=new JLabel(Theme.isDark?"☀ Light Mode":"☾ Dark Mode");
        themeLabel.setFont(Theme.FONT_SMALL); themeLabel.setForeground(Theme.TEXT_MUT);
        themeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeLabel.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ toggleTheme(); }
        });
        themeRow.add(themeLabel);

        // Admin row
        JPanel adminRow=new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));
        adminRow.setBackground(Theme.BG_PANEL); adminRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        UI.AvatarPanel av=new UI.AvatarPanel(adminName,3,32);
        JPanel adminInfo=new JPanel(new GridLayout(2,1,0,1)); adminInfo.setBackground(Theme.BG_PANEL);
        JLabel aName=new JLabel(adminName); aName.setFont(Theme.FONT_MED); aName.setForeground(Theme.TEXT_SEC);
        JLabel aRole=new JLabel(SessionManager.role); aRole.setFont(Theme.FONT_SMALL); aRole.setForeground(Theme.TEXT_MUT);
        adminInfo.add(aName); adminInfo.add(aRole);
        adminRow.add(av); adminRow.add(adminInfo);

        UI.DarkButton logout=UI.DarkButton.normal("Logout");
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE,30)); logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(e->{DBConnection.close();new LoginFrame().setVisible(true);dispose();});

        sidebar.add(new JSeparator(){{setForeground(Theme.BORDER);setMaximumSize(new Dimension(Integer.MAX_VALUE,1));}});
        sidebar.add(themeRow);
        sidebar.add(adminRow);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(logout);
        sidebar.add(Box.createVerticalStrut(10));

        // ── Top header bar ────────────────────────────────────────────────────
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Theme.BG_PANEL);
        topHeader.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER),new EmptyBorder(8,20,8,20)));
        topHeader.setPreferredSize(new Dimension(0,46));

        JLabel pageTitle=new JLabel("Dashboard");
        pageTitle.setFont(Theme.FONT_TITLE); pageTitle.setForeground(Theme.TEXT_PRI);
        pageTitle.setName("pageTitle");

        JPanel rightActions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        rightActions.setBackground(Theme.BG_PANEL);

        // Notification bell - proper panel
        JPanel bellPanel = new JPanel(null); // absolute layout for badge overlay
        bellPanel.setPreferredSize(new Dimension(32, 30));
        bellPanel.setBackground(Theme.BG_PANEL);
        bellPanel.setOpaque(false);

        JLabel bell = new JLabel("🔔");
        bell.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        bell.setForeground(Theme.TEXT_MUT);
        bell.setBounds(0, 4, 24, 24);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ switchToNotifications(); }
        });

        notifBadge = new JLabel(String.valueOf(NotificationManager.unreadCount()));
        notifBadge.setFont(new Font("Segoe UI", Font.BOLD, 8));
        notifBadge.setForeground(Color.WHITE);
        notifBadge.setBackground(Theme.ACCENT_RED);
        notifBadge.setOpaque(true);
        notifBadge.setBorder(new EmptyBorder(1,3,1,3));
        notifBadge.setBounds(14, 0, 18, 13);

        bellPanel.add(bell);
        bellPanel.add(notifBadge);

        NotificationManager.addListener(() -> SwingUtilities.invokeLater(() ->
            notifBadge.setText(String.valueOf(NotificationManager.unreadCount()))));

        // User avatar + name in topbar
        UI.AvatarPanel topAv = new UI.AvatarPanel(adminName, 3, 28);
        JLabel userTag = new JLabel(adminName + "  (" + SessionManager.role + ")");
        userTag.setFont(Theme.FONT_SMALL);
        userTag.setForeground(Theme.TEXT_MUT);

        rightActions.add(bellPanel);
        rightActions.add(Box.createHorizontalStrut(6));
        rightActions.add(topAv);
        rightActions.add(userTag);
        topHeader.add(pageTitle,BorderLayout.WEST); topHeader.add(rightActions,BorderLayout.EAST);

        // ── Content ───────────────────────────────────────────────────────────
        contentArea=new JPanel(new BorderLayout()); contentArea.setBackground(Theme.BG_DEEP);

        JPanel mainPanel=new JPanel(new BorderLayout());
        mainPanel.setBackground(Theme.BG_DEEP);
        mainPanel.add(topHeader,BorderLayout.NORTH);
        mainPanel.add(contentArea,BorderLayout.CENTER);

        root.add(sidebar,BorderLayout.WEST);
        root.add(mainPanel,BorderLayout.CENTER);

        switchNav(0);
    }

    private JPanel createNavItem(String icon, String label, boolean active) {
        JPanel item=new JPanel(new FlowLayout(FlowLayout.LEFT,14,8)){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(getClientProperty("active")==Boolean.TRUE){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setColor(Theme.ACCENT_BLU); g2.fillRect(getWidth()-3,0,3,getHeight()); g2.dispose();
                }
            }
        };
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel ico=new JLabel(icon); ico.setFont(new Font("Segoe UI Symbol",Font.PLAIN,14));
        JLabel txt=new JLabel(label); txt.setFont(Theme.FONT_MED);
        setNavColors(item,ico,txt,active);
        item.add(ico); item.add(txt);
        item.putClientProperty("iconLbl",ico); item.putClientProperty("textLbl",txt);
        item.putClientProperty("active",active);
        return item;
    }

    private void setNavColors(JPanel item, JLabel ico, JLabel txt, boolean active){
        item.setBackground(active?Theme.SIDEBAR_ACTIVE:Theme.BG_PANEL);
        ico.setForeground(active?Theme.ACCENT_BLU:Theme.TEXT_SEC);
        txt.setForeground(active?Theme.ACCENT_BLU:Theme.TEXT_SEC);
    }

    private void switchNav(int idx) {
        for(int i=0;i<navItems.length;i++){
            JPanel item=navItems[i];
            JLabel ico=(JLabel)item.getClientProperty("iconLbl");
            JLabel txt=(JLabel)item.getClientProperty("textLbl");
            boolean active=(i==idx);
            item.putClientProperty("active",active);
            setNavColors(item,ico,txt,active);
            item.repaint();
        }
        activeNav=idx;
        int target=navTargets[idx];
        loadPanel(target);
        // Update page title
        Component th=((BorderLayout)((JPanel)getContentPane().getComponent(1)).getLayout())
            .getLayoutComponent(BorderLayout.NORTH);
        if(th instanceof JPanel){
            for(Component c:((JPanel)th).getComponents())
                if(c instanceof JLabel&&"pageTitle".equals(c.getName()))
                    ((JLabel)c).setText(navLabels[idx]);
        }
    }

    private void loadPanel(int target){
        contentArea.removeAll();
        JPanel panel;
        switch(target){
            case 0: panel = SessionManager.isEmployee()
                          ? new MyDashboardPanel(this)
                          : new DashboardPanel(this);    break;
            case 1: panel = new EmployeePanel(this);     break;
            case 2: panel = new AttendancePanel(this);   break;
            case 3: panel = new LeavePanel(this);        break;
            case 4: panel = new SearchPanel(this);       break;
            case 5: panel = new ReportsPanel(this);      break;
            case 6: panel = new EmailChatPanel(this);    break;
            case 7: panel = new NotificationsPanel(this);break;
            case 8: panel = new SettingsPanel(this);     break;
            default: panel = SessionManager.isEmployee()
                           ? new MyDashboardPanel(this)
                           : new DashboardPanel(this);
        }
        contentArea.add(panel,BorderLayout.CENTER);
        contentArea.revalidate(); contentArea.repaint();
    }

    private void switchToNotifications(){
        for(int i=0;i<navTargets.length;i++) if(navTargets[i]==7){switchNav(i);return;}
    }

    private void toggleTheme(){
        Theme.isDark=!Theme.isDark; Theme.refresh();
        themeLabel.setText(Theme.isDark?"☀ Light Mode":"☾ Dark Mode");
        SwingUtilities.invokeLater(()->{
            SwingUtilities.updateComponentTreeUI(this);
            repaint(); revalidate();
        });
    }

    public void navigateTo(int target){ for(int i=0;i<navTargets.length;i++) if(navTargets[i]==target){switchNav(i);return;} }
    public String getAdminName(){return adminName;}
}