package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.sql.*;
import javax.imageio.*;

public class IDCardPanel extends JPanel {
    private String empId, name, desig, dept, phone, email, photo, joined;

    public IDCardPanel(String empId, String name, String desig, String dept,
                       String phone, String email, String photo, String joined) {
        this.empId=empId; this.name=name; this.desig=desig; this.dept=dept;
        this.phone=phone; this.email=email; this.photo=photo; this.joined=joined;
        setPreferredSize(new Dimension(320, 200));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w=getWidth(), h=getHeight();

        // Card background
        g2.setColor(new Color(0x16,0x1b,0x27));
        g2.fillRoundRect(0,0,w,h,16,16);

        // Top color strip
        GradientPaint gp = new GradientPaint(0,0,new Color(0x1e,0x3a,0x5f),w,0,new Color(0x2a,0x14,0x4a));
        g2.setPaint(gp);
        g2.fillRoundRect(0,0,w,55,16,16);
        g2.fillRect(0,30,w,25);

        // Company name
        g2.setColor(new Color(0x7c,0x9e,0xf5));
        g2.setFont(new Font("Segoe UI",Font.BOLD,13));
        g2.drawString("EMS Pro", 14, 22);
        g2.setColor(new Color(0x94,0xa3,0xb8));
        g2.setFont(new Font("Segoe UI",Font.PLAIN,8));
        g2.drawString("EMPLOYEE MANAGEMENT SYSTEM", 14, 34);

        // EMPLOYEE ID CARD text on right
        g2.setColor(new Color(0x7c,0x9e,0xf5));
        g2.setFont(new Font("Segoe UI",Font.BOLD,8));
        g2.drawString("IDENTITY CARD", w-80, 22);

        // ID badge
        g2.setColor(new Color(0x0a,0x1f,0x18));
        g2.fillRoundRect(w-80,26,70,14,6,6);
        g2.setColor(new Color(0x4a,0xde,0x80));
        g2.setFont(new Font("Segoe UI",Font.BOLD,8));
        g2.drawString(empId, w-76, 36);

        // Photo circle
        int px=16, py=46, ps=60;
        g2.setColor(new Color(0x1e,0x25,0x35));
        g2.fillOval(px,py,ps,ps);
        g2.setColor(new Color(0x2a,0x2f,0x3d));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(px,py,ps,ps);

        // Draw photo or initials
        if(!photo.isEmpty()){
            File pf=new File(photo);
            if(pf.exists()){
                try{
                    BufferedImage img=ImageIO.read(pf);
                    // Clip to circle
                    BufferedImage circle=new BufferedImage(ps,ps,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cg=(Graphics2D)circle.createGraphics();
                    cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    cg.setClip(new java.awt.geom.Ellipse2D.Float(0,0,ps,ps));
                    cg.drawImage(img,0,0,ps,ps,null);
                    cg.dispose();
                    g2.drawImage(circle,px,py,null);
                }catch(IOException ignored){ drawInitials(g2,px,py,ps); }
            } else drawInitials(g2,px,py,ps);
        } else drawInitials(g2,px,py,ps);

        // Name
        g2.setColor(new Color(0xe2,0xe8,0xf0));
        g2.setFont(new Font("Segoe UI",Font.BOLD,14));
        g2.drawString(name, px+ps+12, py+18);

        // Designation
        g2.setColor(new Color(0x7c,0x9e,0xf5));
        g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
        g2.drawString(desig, px+ps+12, py+32);

        // Info rows
        g2.setFont(new Font("Segoe UI",Font.PLAIN,9));
        int ix=px+ps+12, iy=py+48;
        drawInfo(g2, "📞", phone, ix, iy);
        drawInfo(g2, "✉", email.length()>22?email.substring(0,20)+"..":email, ix, iy+13);
        drawInfo(g2, "🎓", dept, ix, iy+26);
        drawInfo(g2, "📅", "Joined: "+joined, ix, iy+39);

        // Bottom bar
        g2.setColor(new Color(0x1e,0x25,0x35));
        g2.fillRoundRect(0,h-28,w,28,16,16);
        g2.fillRect(0,h-40,w,14);

        // QR code placeholder (drawn as decorative squares)
        drawQR(g2, w-54, h-54, 44, empId);

        // Bottom text
        g2.setColor(new Color(0x64,0x74,0x8b));
        g2.setFont(new Font("Segoe UI",Font.PLAIN,8));
        g2.drawString("This card is property of EMS Pro. If found please return.", 10, h-10);

        // Border
        g2.setColor(new Color(0x2a,0x2f,0x3d));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0,0,w-1,h-1,16,16);

        g2.dispose();
    }

    private void drawInitials(Graphics2D g2, int x, int y, int size){
        String[] parts=name.split(" ");
        StringBuilder sb=new StringBuilder();
        for(String p:parts) if(!p.isEmpty()) sb.append(p.charAt(0));
        String ini=sb.length()>2?sb.substring(0,2):sb.toString();
        Color[] AVATAR_COLORS={new Color(0xaf,0xa9,0xec),new Color(0xed,0x93,0xb1),
            new Color(0x5d,0xca,0xa5),new Color(0x7c,0xb9,0xf5),new Color(0xfb,0xbf,0x24)};
        g2.setColor(AVATAR_COLORS[Math.abs(empId.hashCode())%AVATAR_COLORS.length]);
        g2.setFont(new Font("Segoe UI",Font.BOLD,size/3));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(ini, x+(size-fm.stringWidth(ini))/2, y+(size+fm.getAscent()-fm.getDescent())/2);
    }

    private void drawInfo(Graphics2D g2, String icon, String text, int x, int y){
        g2.setColor(new Color(0x64,0x74,0x8b));
        g2.setFont(new Font("Segoe UI Symbol",Font.PLAIN,9));
        g2.drawString(icon+" ", x, y);
        g2.setColor(new Color(0x94,0xa3,0xb8));
        g2.setFont(new Font("Segoe UI",Font.PLAIN,9));
        g2.drawString(text, x+14, y);
    }

    private void drawQR(Graphics2D g2, int x, int y, int size, String data){
        // Simple decorative QR-like pattern based on empId hash
        int cells=7;
        int cell=size/cells;
        g2.setColor(new Color(0x1e,0x25,0x35));
        g2.fillRect(x,y,size,size);
        g2.setColor(new Color(0x94,0xa3,0xb8));
        // Corner squares
        int[][] corners={{0,0},{cells-3,0},{0,cells-3}};
        for(int[] c:corners){
            g2.fillRect(x+c[0]*cell, y+c[1]*cell, 3*cell, 3*cell);
            g2.setColor(new Color(0x1e,0x25,0x35));
            g2.fillRect(x+c[0]*cell+cell, y+c[1]*cell+cell, cell, cell);
            g2.setColor(new Color(0x94,0xa3,0xb8));
        }
        // Random data cells from hash
        int hash=data.hashCode();
        for(int r=0;r<cells;r++) for(int c=0;c<cells;c++){
            if((hash^(r*13+c*7))%3==0) g2.fillRect(x+c*cell,y+r*cell,cell,cell);
        }
        // Border
        g2.setColor(new Color(0x2a,0x2f,0x3d));
        g2.drawRect(x,y,size-1,size-1);
        // Label
        g2.setColor(new Color(0x64,0x74,0x8b));
        g2.setFont(new Font("Segoe UI",Font.PLAIN,7));
        g2.drawString("SCAN", x+10, y+size+10);
    }
}