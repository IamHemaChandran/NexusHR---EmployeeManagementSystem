package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;
import java.io.*;
import java.sql.*;
import javax.imageio.*;

public class IDCardGenerator extends JPanel {
    private MainFrame parent;

    public IDCardGenerator(MainFrame parent) {
        this.parent = parent;
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
        JLabel title = new JLabel("  Employee ID Card Generator");
        title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        right.setBackground(Theme.BG_PANEL);
        UI.DarkButton backBtn = UI.DarkButton.normal("← Back");
        backBtn.addActionListener(e -> parent.navigateTo(1));
        right.add(backBtn);
        top.add(title, BorderLayout.WEST); top.add(right, BorderLayout.EAST);

        // Content
        JPanel content = new JPanel(new BorderLayout(16,0));
        content.setBackground(Theme.BG_DEEP);
        content.setBorder(new EmptyBorder(16,16,16,16));

        // LEFT: Employee selector
        JPanel leftPanel = buildSelectorPanel();

        // RIGHT: Card preview
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Theme.BG_DEEP);
        JLabel previewHint = new JLabel("Select an employee to preview their ID card",SwingConstants.CENTER);
        previewHint.setFont(Theme.FONT_MED); previewHint.setForeground(Theme.TEXT_MUT);
        rightPanel.add(previewHint, BorderLayout.CENTER);
        rightPanel.setName("previewPanel");

        content.add(leftPanel, BorderLayout.WEST);
        content.add(rightPanel, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(new CompoundBorder(
            new UI.RoundBorder(Theme.BORDER, 10),
            new EmptyBorder(14,14,14,14)));
        panel.setPreferredSize(new Dimension(220, 0));

        JLabel h = new JLabel("Select Employee");
        h.setFont(Theme.FONT_TITLE); h.setForeground(Theme.TEXT_PRI);
        h.setBorder(new EmptyBorder(0,0,10,0));

        // Employee list
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.BG_PANEL);

        try {
            Connection conn = DBConnection.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT empId, name, designation FROM employee ORDER BY name");
            while(rs.next()){
                String id   = rs.getString("empId");
                String name = rs.getString("name");
                String desig= nvl(rs.getString("designation"),"—");
                list.add(buildEmpItem(id, name, desig, list));
                list.add(Box.createVerticalStrut(4));
            }
            rs.close();
        } catch(SQLException e){
            JLabel err = new JLabel("DB Error"); err.setForeground(Theme.ACCENT_RED);
            list.add(err);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null); scroll.setBackground(Theme.BG_PANEL);
        scroll.getViewport().setBackground(Theme.BG_PANEL);

        panel.add(h, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEmpItem(String empId, String name, String desig, JPanel list) {
        JPanel item = new JPanel(new BorderLayout(8,0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(8,10,8,10));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        UI.AvatarPanel av = new UI.AvatarPanel(name, Math.abs(empId.hashCode())%5, 32);
        JPanel info = new JPanel(new GridLayout(2,1,0,1)); info.setOpaque(false);
        JLabel nm = new JLabel(name); nm.setFont(Theme.FONT_MED); nm.setForeground(Theme.TEXT_PRI);
        JLabel dg = new JLabel(desig); dg.setFont(Theme.FONT_SMALL); dg.setForeground(Theme.TEXT_MUT);
        info.add(nm); info.add(dg);
        item.add(av, BorderLayout.WEST); item.add(info, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ loadCard(empId); }
            @Override public void mouseEntered(MouseEvent e){ item.setBackground(Theme.BG_HOVER); item.repaint(); }
            @Override public void mouseExited(MouseEvent e){ item.repaint(); }
        });
        return item;
    }

    private void loadCard(String empId) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM employee WHERE empId=?");
            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) showCard(rs);
            rs.close(); ps.close();
        } catch(SQLException e){
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showCard(ResultSet rs) throws SQLException {
        String empId = rs.getString("empId");
        String name  = nvl(rs.getString("name"), "—");
        String desig = nvl(rs.getString("designation"), "—");
        String dept  = nvl(rs.getString("education"), "—");
        String phone = nvl(rs.getString("phone"), "—");
        String email = nvl(rs.getString("email"), "—");
        String photo = nvl(rs.getString("photo"), "");
        String joined= rs.getTimestamp("created_at")!=null
            ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString() : "—";

        // Build card panel
        IDCardPanel card = new IDCardPanel(empId, name, desig, dept, phone, email, photo, joined);

        // Find preview panel
        Container content = (Container) getComponent(1);
        if(content instanceof JPanel){
            Component previewPanel = ((BorderLayout)((JPanel)content).getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
            if(previewPanel instanceof JPanel){
                JPanel pp = (JPanel) previewPanel;
                pp.removeAll();

                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setBackground(Theme.BG_DEEP);

                // Center the card
                JPanel cardHolder = new JPanel(new GridBagLayout());
                cardHolder.setBackground(Theme.BG_DEEP);
                cardHolder.add(card);

                // Action buttons
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
                actions.setBackground(Theme.BG_DEEP);

                UI.DarkButton printBtn = UI.DarkButton.primary("🖨 Print ID Card");
                printBtn.setPreferredSize(new Dimension(160,38));
                printBtn.addActionListener(e -> printCard(card));

                UI.DarkButton saveBtn = UI.DarkButton.success("💾 Save as PNG");
                saveBtn.setPreferredSize(new Dimension(160,38));
                saveBtn.addActionListener(e -> saveCard(card, name));

                actions.add(printBtn); actions.add(saveBtn);

                wrapper.add(cardHolder, BorderLayout.CENTER);
                wrapper.add(actions, BorderLayout.SOUTH);
                pp.add(wrapper, BorderLayout.CENTER);
                pp.revalidate(); pp.repaint();
            }
        }
    }

    private void printCard(IDCardPanel card) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("EMS ID Card");
        job.setPrintable((g, pf, pi) -> {
            if(pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            double sx = pf.getImageableWidth()  / card.getPreferredSize().width;
            double sy = pf.getImageableHeight() / card.getPreferredSize().height;
            double scale = Math.min(sx, sy);
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(scale, scale);
            card.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if(job.printDialog()){
            try{ job.print(); }
            catch(PrinterException e){
                JOptionPane.showMessageDialog(this,"Print failed: "+e.getMessage());
            }
        }
    }

    private void saveCard(IDCardPanel card, String name) {
        Dimension size = card.getPreferredSize();
        BufferedImage img = new BufferedImage(size.width*2, size.height*2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.scale(2,2);
        card.printAll(g2);
        g2.dispose();

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("IDCard_"+name.replace(" ","_")+".png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image","png"));
        if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
            try{
                File f = fc.getSelectedFile();
                if(!f.getName().endsWith(".png")) f=new File(f.getAbsolutePath()+".png");
                ImageIO.write(img,"PNG",f);
                UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"ID Card saved!",false);
                NotificationManager.add("ID Card","ID Card for "+name+" saved as PNG","info");
            }catch(IOException e){
                JOptionPane.showMessageDialog(this,"Save failed: "+e.getMessage());
            }
        }
    }

    private String nvl(String s, String d){ return(s==null||s.isEmpty())?d:s; }
}
