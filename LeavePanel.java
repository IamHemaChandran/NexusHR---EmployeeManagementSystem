package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;

public class LeavePanel extends JPanel {
    private MainFrame parent;
    private DefaultTableModel model;
    private JTable table;

    public LeavePanel(MainFrame parent){
        this.parent=parent;
        setLayout(new BorderLayout()); setBackground(Theme.BG_DEEP);
        ensureTable();
        buildUI();
        loadLeaves();
    }

    private void ensureTable(){
        try{
            Connection conn=DBConnection.getConnection();
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS leaves (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "empId VARCHAR(20) NOT NULL," +
                "empName VARCHAR(40)," +
                "leaveType VARCHAR(30)," +
                "fromDate DATE," +
                "toDate DATE," +
                "days INT DEFAULT 1," +
                "reason TEXT," +
                "status VARCHAR(20) DEFAULT 'Pending'," +
                "appliedOn TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }catch(SQLException ignored){}
    }

    private void buildUI(){
        JPanel top=new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_PANEL);
        top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER),new EmptyBorder(10,20,10,20)));
        JLabel title=new JLabel("  Leave Management"); title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setBackground(Theme.BG_PANEL);
        UI.DarkButton applyBtn=UI.DarkButton.primary("+ Apply Leave");
        applyBtn.addActionListener(e->openApplyDialog());
        right.add(applyBtn);
        top.add(title,BorderLayout.WEST); top.add(right,BorderLayout.EAST);

        // Stats row
        JPanel statsRow=new JPanel(new GridLayout(1,4,10,0));
        statsRow.setBackground(Theme.BG_DEEP); statsRow.setBorder(new EmptyBorder(12,14,8,14));
        statsRow.setPreferredSize(new Dimension(0,72));
        statsRow.add(statCard("Pending","—",Theme.ACCENT_YLW,"pending-count"));
        statsRow.add(statCard("Approved","—",Theme.ACCENT_GRN,"approved-count"));
        statsRow.add(statCard("Rejected","—",Theme.ACCENT_RED,"rejected-count"));
        statsRow.add(statCard("This Month","—",Theme.ACCENT_BLU,"month-count"));

        // Table
        String[] cols={"ID","Employee","Type","From","To","Days","Reason","Status","Actions"};
        model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return c==8;}};
        table=new JTable(model){
            public Component prepareRenderer(TableCellRenderer r,int row,int col){
                Component c=super.prepareRenderer(r,row,col);
                c.setBackground(row%2==0?Theme.BG_DEEP:Theme.BG_PANEL); c.setForeground(Theme.TEXT_PRI);
                return c;
            }
        };
        styleTable();

        JScrollPane scroll=new JScrollPane(table);
        scroll.setBorder(new CompoundBorder(new EmptyBorder(0,14,14,14),new UI.RoundBorder(Theme.BORDER,10)));
        scroll.setBackground(Theme.BG_DEEP); scroll.getViewport().setBackground(Theme.BG_DEEP);

        JPanel body=new JPanel(new BorderLayout()); body.setBackground(Theme.BG_DEEP);
        body.add(statsRow,BorderLayout.NORTH); body.add(scroll,BorderLayout.CENTER);

        add(top,BorderLayout.NORTH); add(body,BorderLayout.CENTER);
    }

    private JPanel statCard(String label, String val, Color color, String key){
        JPanel p=new JPanel(){@Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BG_PANEL); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
            g2.setColor(Theme.BORDER);   g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
            g2.dispose();
        }};
        p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setBorder(new EmptyBorder(10,14,10,14));
        JLabel l=new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        JLabel v=new JLabel(val);   v.setFont(new Font("Segoe UI",Font.BOLD,20)); v.setForeground(color);
        v.setName(key); p.add(l); p.add(Box.createVerticalStrut(3)); p.add(v);
        return p;
    }

    private void loadLeaves(){
        model.setRowCount(0);
        int pending=0,approved=0,rejected=0,month=0;
        try{
            Connection conn=DBConnection.getConnection();
            String sql=SessionManager.isAdmin()||SessionManager.isHR()
                ? "SELECT * FROM leaves ORDER BY appliedOn DESC"
                : "SELECT * FROM leaves WHERE empId='"+SessionManager.empId+"' ORDER BY appliedOn DESC";
            ResultSet rs=conn.createStatement().executeQuery(sql);
            while(rs.next()){
                String st=nvl(rs.getString("status"),"Pending");
                model.addRow(new Object[]{rs.getInt("id"),
                    nvl(rs.getString("empName"),""),nvl(rs.getString("leaveType"),""),
                    nvl(rs.getString("fromDate"),""),nvl(rs.getString("toDate"),""),
                    rs.getInt("days"),nvl(rs.getString("reason"),""),st,"•"});
                if("Pending".equals(st))  pending++;
                if("Approved".equals(st)) approved++;
                if("Rejected".equals(st)) rejected++;
                month++;
            }
            rs.close();
        }catch(SQLException e){
            JOptionPane.showMessageDialog(this,"DB: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        // Update stat cards
        updateStat("pending-count",String.valueOf(pending));
        updateStat("approved-count",String.valueOf(approved));
        updateStat("rejected-count",String.valueOf(rejected));
        updateStat("month-count",String.valueOf(month));
    }

    private void updateStat(String key, String val){
        updateStatIn(this, key, val);
    }
    private void updateStatIn(Container c, String key, String val){
        for(Component comp:c.getComponents()){
            if(comp instanceof JLabel && key.equals(comp.getName())) ((JLabel)comp).setText(val);
            if(comp instanceof Container) updateStatIn((Container)comp,key,val);
        }
    }

    private void openApplyDialog(){
        JDialog d=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Apply Leave",true);
        d.setSize(480,420); d.setLocationRelativeTo(this);
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(Theme.BG_PANEL); root.setBorder(new EmptyBorder(20,24,20,24));

        JLabel h=new JLabel("Apply for Leave"); h.setFont(Theme.FONT_TITLE); h.setForeground(Theme.TEXT_PRI);
        h.setBorder(new EmptyBorder(0,0,14,0));

        JPanel form=new JPanel(new GridLayout(0,2,10,10)); form.setBackground(Theme.BG_PANEL);

        // Employee selector (admin/hr can pick any)
        UI.DarkField empField;
        if(SessionManager.isHR()){
            empField=new UI.DarkField("Employee ID");
        } else {
            empField=new UI.DarkField(SessionManager.empId);
            empField.setText(SessionManager.empId); empField.setEditable(false);
        }
        UI.DarkCombo typeCombo=new UI.DarkCombo(new String[]{"Annual Leave","Sick Leave","Emergency","Maternity/Paternity","Unpaid Leave"});
        UI.DarkField fromField=new UI.DarkField("YYYY-MM-DD");
        UI.DarkField toField=new UI.DarkField("YYYY-MM-DD");

        addFLbl(form,"Employee ID"); form.add(empField);
        addFLbl(form,"Leave Type");  form.add(typeCombo);
        addFLbl(form,"From Date");   form.add(fromField);
        addFLbl(form,"To Date");     form.add(toField);

        JTextArea reason=new JTextArea(4,20);
        reason.setFont(Theme.FONT_MED); reason.setForeground(Theme.TEXT_PRI);
        reason.setBackground(Theme.BG_INPUT); reason.setCaretColor(Theme.TEXT_PRI);
        reason.setBorder(new CompoundBorder(new UI.RoundBorder(Theme.BORDER,8),new EmptyBorder(8,10,8,10)));
        reason.setLineWrap(true);
        JScrollPane rsp=new JScrollPane(reason); rsp.setBorder(null);

        JPanel reasonRow=new JPanel(new BorderLayout()); reasonRow.setBackground(Theme.BG_PANEL);
        addFLbl(form,"Reason"); form.add(rsp);

        JPanel acts=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); acts.setBackground(Theme.BG_PANEL);
        acts.setBorder(new EmptyBorder(12,0,0,0));
        UI.DarkButton cancel=UI.DarkButton.normal("Cancel"); cancel.addActionListener(e->d.dispose());
        UI.DarkButton submit=UI.DarkButton.success("Submit Application");
        submit.addActionListener(e->{
            String empId=empField.getText().trim();
            if(empId.isEmpty()||fromField.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(d,"Employee ID and From date required."); return;
            }
            try{
                Connection conn=DBConnection.getConnection();
                // Get emp name
                String empName="Unknown";
                PreparedStatement ep=conn.prepareStatement("SELECT name FROM employee WHERE empId=?");
                ep.setString(1,empId); ResultSet er=ep.executeQuery();
                if(er.next()) empName=er.getString("name"); else{
                    JOptionPane.showMessageDialog(d,"Employee not found: "+empId); er.close();ep.close();return;}
                er.close(); ep.close();

                String from=fromField.getText().trim(), to=toField.getText().trim();
                if(to.isEmpty()) to=from;
                long days=1;
                try{days=ChronoUnit.DAYS.between(LocalDate.parse(from),LocalDate.parse(to))+1;}catch(Exception ex){days=1;}

                PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO leaves (empId,empName,leaveType,fromDate,toDate,days,reason,status) VALUES (?,?,?,?,?,?,?,'Pending')");
                ps.setString(1,empId); ps.setString(2,empName);
                ps.setString(3,(String)typeCombo.getSelectedItem());
                ps.setString(4,from); ps.setString(5,to); ps.setLong(6,days);
                ps.setString(7,reason.getText().trim());
                ps.executeUpdate(); ps.close();
                NotificationManager.add("Leave","New leave request from "+empName,"info");
                UI.showToast((JFrame)SwingUtilities.getWindowAncestor(LeavePanel.this),"Leave applied!",false);
                d.dispose(); loadLeaves();
            }catch(SQLException ex){JOptionPane.showMessageDialog(d,"Error: "+ex.getMessage());}
        });
        acts.add(cancel); acts.add(submit);
        root.add(h,BorderLayout.NORTH);
        JPanel scroll2=new JPanel(new BorderLayout()); scroll2.setBackground(Theme.BG_PANEL);
        scroll2.add(form,BorderLayout.NORTH); scroll2.add(reasonRow,BorderLayout.CENTER);
        root.add(form,BorderLayout.CENTER); root.add(acts,BorderLayout.SOUTH);
        d.setContentPane(root); d.setVisible(true);
    }

    private void addFLbl(JPanel p, String text){
        JLabel l=new JLabel(text); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT); p.add(l);
    }

    private void approveReject(int rowIdx, String action){
        int id=(Integer)model.getValueAt(rowIdx,0);
        String empName=(String)model.getValueAt(rowIdx,1);
        try{
            Connection conn=DBConnection.getConnection();
            PreparedStatement ps=conn.prepareStatement("UPDATE leaves SET status=? WHERE id=?");
            ps.setString(1,action); ps.setInt(2,id); ps.executeUpdate(); ps.close();
            NotificationManager.add("Leave "+action,"Leave for "+empName+" has been "+action.toLowerCase(),"info");
            UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"Leave "+action.toLowerCase()+"!","Rejected".equals(action));
            loadLeaves();
        }catch(SQLException e){JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());}
    }

    private void styleTable(){
        table.setBackground(Theme.BG_DEEP); table.setForeground(Theme.TEXT_PRI);
        table.setFont(Theme.FONT_MED); table.setRowHeight(40); table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0)); table.setSelectionBackground(Theme.BG_CARD);
        JTableHeader h=table.getTableHeader(); h.setBackground(Theme.BG_PANEL);
        h.setForeground(Theme.TEXT_MUT); h.setFont(Theme.FONT_SMALL);
        h.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));
        int[] w={50,130,120,95,95,50,140,90,110};
        for(int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.getColumn("Status").setCellRenderer(new StatusRenderer());
        table.getColumn("Actions").setCellRenderer(new ActRenderer());
        table.getColumn("Actions").setCellEditor(new ActEditor());
    }

    private String nvl(String s,String d){return(s==null||s.isEmpty())?d:s;}

    class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
            String st=v==null?"Pending":v.toString();
            UI.BadgeLabel b;
            if("Approved".equals(st))b=UI.BadgeLabel.green("Approved");
            else if("Rejected".equals(st))b=UI.BadgeLabel.red("Rejected");
            else b=UI.BadgeLabel.yellow("Pending");
            JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,8)); p.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL);
            p.add(b); return p;
        }
    }
    class ActRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
            String status=(String)t.getValueAt(r,7);
            JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,3,6)); p.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL);
            if(SessionManager.isHR()&&"Pending".equals(status)){
                UI.DarkButton a=UI.DarkButton.success("✓"); a.setPreferredSize(new Dimension(34,26));
                UI.DarkButton rj=UI.DarkButton.danger("✕");  rj.setPreferredSize(new Dimension(34,26));
                p.add(a); p.add(rj);
            } else { JLabel l=new JLabel("—"); l.setForeground(Theme.TEXT_MUT); p.add(l); }
            return p;
        }
    }
    class ActEditor extends DefaultCellEditor {
        private JPanel panel; private int currentRow;
        ActEditor(){super(new JCheckBox()); setClickCountToStart(1);}
        public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){
            currentRow=r; String status=(String)t.getValueAt(r,7);
            panel=new JPanel(new FlowLayout(FlowLayout.CENTER,3,6)); panel.setBackground(Theme.BG_CARD);
            if(SessionManager.isHR()&&"Pending".equals(status)){
                UI.DarkButton a=UI.DarkButton.success("✓"); a.setPreferredSize(new Dimension(34,26));
                a.addActionListener(e->{stopCellEditing();approveReject(currentRow,"Approved");});
                UI.DarkButton rj=UI.DarkButton.danger("✕"); rj.setPreferredSize(new Dimension(34,26));
                rj.addActionListener(e->{stopCellEditing();approveReject(currentRow,"Rejected");});
                panel.add(a); panel.add(rj);
            } else { JLabel l=new JLabel("—"); l.setForeground(Theme.TEXT_MUT); panel.add(l); }
            return panel;
        }
        public Object getCellEditorValue(){return "•";}
    }
}
