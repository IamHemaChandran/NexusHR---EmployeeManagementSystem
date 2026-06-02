package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class NotificationsPanel extends JPanel {
    private MainFrame parent;
    private JPanel listPanel;

    public NotificationsPanel(MainFrame parent){
        this.parent=parent;
        setLayout(new BorderLayout()); setBackground(Theme.BG_DEEP);
        buildUI();
        NotificationManager.addListener(this::renderNotifs);
    }

    private void buildUI(){
        JPanel top=new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_PANEL);
        top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER),new EmptyBorder(10,20,10,20)));
        JLabel title=new JLabel("  Notifications"); title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setBackground(Theme.BG_PANEL);
        UI.DarkButton markRead=UI.DarkButton.normal("Mark All Read");
        markRead.addActionListener(e->{NotificationManager.markAllRead(); renderNotifs();});
        right.add(markRead);
        top.add(title,BorderLayout.WEST); top.add(right,BorderLayout.EAST);

        listPanel=new JPanel(); listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.BG_DEEP); listPanel.setBorder(new EmptyBorder(12,14,14,14));

        JScrollPane scroll=new JScrollPane(listPanel);
        scroll.setBorder(null); scroll.setBackground(Theme.BG_DEEP); scroll.getViewport().setBackground(Theme.BG_DEEP);

        add(top,BorderLayout.NORTH); add(scroll,BorderLayout.CENTER);
        renderNotifs();
    }

    private void renderNotifs(){
        listPanel.removeAll();
        List<NotificationManager.Notification> notifs=NotificationManager.getAll();
        if(notifs.isEmpty()){
            JLabel empty=new JLabel("No notifications yet");
            empty.setFont(Theme.FONT_MED); empty.setForeground(Theme.TEXT_MUT);
            empty.setBorder(new EmptyBorder(30,0,0,0)); empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for(NotificationManager.Notification n:notifs){
                listPanel.add(buildCard(n)); listPanel.add(Box.createVerticalStrut(8));
            }
        }
        listPanel.revalidate(); listPanel.repaint();
    }

    private JPanel buildCard(NotificationManager.Notification n){
        Color accent="info".equals(n.type)?Theme.ACCENT_BLU:
                     "warning".equals(n.type)?Theme.ACCENT_YLW:Theme.ACCENT_RED;
        JPanel card=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(n.read?Theme.BG_PANEL:Theme.BG_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(accent); g2.fillRoundRect(0,0,4,getHeight(),4,4);
                g2.setColor(Theme.BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setLayout(new BorderLayout(10,0));
        card.setBorder(new EmptyBorder(12,16,12,12)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));

        JPanel textPanel=new JPanel(new GridLayout(2,1,0,3)); textPanel.setOpaque(false);
        JLabel tit=new JLabel(n.title); tit.setFont(new Font("Segoe UI",Font.BOLD,13)); tit.setForeground(Theme.TEXT_PRI);
        JLabel bod=new JLabel(n.body);  bod.setFont(Theme.FONT_SMALL); bod.setForeground(Theme.TEXT_MUT);
        textPanel.add(tit); textPanel.add(bod);

        JPanel right=new JPanel(new BorderLayout()); right.setOpaque(false);
        JLabel time=new JLabel(n.time); time.setFont(new Font("Segoe UI",Font.PLAIN,10)); time.setForeground(Theme.TEXT_MUT);
        if(!n.read){
            JPanel dot=new JPanel(){@Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setColor(Theme.ACCENT_BLU); g2.fillOval(0,0,8,8); g2.dispose();}};
            dot.setOpaque(false); dot.setPreferredSize(new Dimension(8,8));
            right.add(dot,BorderLayout.NORTH);
        }
        right.add(time,BorderLayout.SOUTH);
        card.add(textPanel,BorderLayout.CENTER); card.add(right,BorderLayout.EAST);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){n.read=true;renderNotifs();}});
        return card;
    }
}
