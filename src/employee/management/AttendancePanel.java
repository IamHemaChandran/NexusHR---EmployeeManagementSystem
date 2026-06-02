package employee.management;

import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class AttendancePanel extends JPanel {
    private MainFrame parent;
    private JTable table;
    private DefaultTableModel model;
    private JLabel dateLabel, statsLabel;
    private String selectedDate;

    public AttendancePanel(MainFrame parent){
        this.parent=parent;
        setLayout(new BorderLayout()); setBackground(Theme.BG_DEEP);
        selectedDate=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ensureTables();
        buildUI();
        loadAttendance();
    }

    private void ensureTables(){
        try{
            Connection conn=DBConnection.getConnection();
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "empId VARCHAR(20) NOT NULL," +
                "date DATE NOT NULL," +
                "status VARCHAR(20) DEFAULT 'Absent'," +
                "checkIn VARCHAR(10)," +
                "checkOut VARCHAR(10)," +
                "UNIQUE KEY uniq_emp_date (empId,date))");
        }catch(SQLException ignored){}
    }

    private void buildUI(){
        // Topbar
        JPanel top=new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_PANEL);
        top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER),new EmptyBorder(10,20,10,20)));
        JLabel title=new JLabel("  Attendance Tracker"); title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setBackground(Theme.BG_PANEL);
        dateLabel=new JLabel(selectedDate); dateLabel.setFont(Theme.FONT_MED); dateLabel.setForeground(Theme.ACCENT_BLU);
        JButton prevDay=new JButton("◀"); styleSmallBtn(prevDay);
        JButton nextDay=new JButton("▶"); styleSmallBtn(nextDay);
        prevDay.addActionListener(e->{
            selectedDate=LocalDate.parse(selectedDate).minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dateLabel.setText(selectedDate); loadAttendance();
        });
        nextDay.addActionListener(e->{
            selectedDate=LocalDate.parse(selectedDate).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dateLabel.setText(selectedDate); loadAttendance();
        });

        UI.DarkButton markAll=UI.DarkButton.success("Mark All Present");
        markAll.addActionListener(e->markAllPresent());
        UI.DarkButton saveBtn=UI.DarkButton.primary("Save Changes");
        saveBtn.addActionListener(e->saveAttendance());

        statsLabel=new JLabel(""); statsLabel.setFont(Theme.FONT_SMALL); statsLabel.setForeground(Theme.TEXT_MUT);

        right.add(statsLabel); right.add(prevDay); right.add(dateLabel); right.add(nextDay);
        // Only HR/Admin can mark and save attendance
        if(SessionManager.isHR()){
            right.add(markAll); right.add(saveBtn);
        }
        top.add(title,BorderLayout.WEST); top.add(right,BorderLayout.EAST);

        // Table
        String[] cols={"Employee ID","Name","Designation","Status","Check In","Check Out"};
        model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return c==3||c==4||c==5;}};
        table=new JTable(model){
            public Component prepareRenderer(TableCellRenderer r,int row,int col){
                Component c=super.prepareRenderer(r,row,col);
                c.setBackground(row%2==0?Theme.BG_DEEP:Theme.BG_PANEL); c.setForeground(Theme.TEXT_PRI);
                return c;
            }
        };
        styleTable();

        // Status column = combo
        JComboBox<String> statusCombo=new JComboBox<>(new String[]{"Present","Absent","Late","Half Day","Leave"});
        statusCombo.setBackground(Theme.BG_CARD); statusCombo.setForeground(Theme.TEXT_PRI);
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusCombo));
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());

        JScrollPane scroll=new JScrollPane(table);
        scroll.setBorder(new CompoundBorder(new EmptyBorder(12,14,14,14),new UI.RoundBorder(Theme.BORDER,10)));
        scroll.setBackground(Theme.BG_DEEP); scroll.getViewport().setBackground(Theme.BG_DEEP);

        add(top,BorderLayout.NORTH); add(scroll,BorderLayout.CENTER);
    }

    private void loadAttendance(){
        model.setRowCount(0);
        try{
            Connection conn=DBConnection.getConnection();
            if(SessionManager.isHR()){
                // Admin/HR: init absent for all employees
                conn.createStatement().execute(
                    "INSERT IGNORE INTO attendance (empId,date,status) SELECT empId,'"+selectedDate+"','Absent' FROM employee");
            } else {
                // Employee: init only for themselves
                conn.createStatement().execute(
                    "INSERT IGNORE INTO attendance (empId,date,status) VALUES ('"+SessionManager.empId+"','"+selectedDate+"','Absent')");
            }

            String sql = SessionManager.isEmployee()
                ? "SELECT e.empId,e.name,e.designation,a.status,a.checkIn,a.checkOut " +
                  "FROM employee e JOIN attendance a ON e.empId=a.empId " +
                  "WHERE a.date='"+selectedDate+"' AND e.empId='"+SessionManager.empId+"'"
                : "SELECT e.empId,e.name,e.designation,a.status,a.checkIn,a.checkOut " +
                  "FROM employee e JOIN attendance a ON e.empId=a.empId " +
                  "WHERE a.date='"+selectedDate+"' ORDER BY e.name";

            ResultSet rs=conn.createStatement().executeQuery(sql);
            int p=0,ab=0,tot=0;
            while(rs.next()){
                String st=nvl(rs.getString("status"),"Absent");
                model.addRow(new Object[]{rs.getString("empId"),rs.getString("name"),
                    nvl(rs.getString("designation"),"—"),st,
                    nvl(rs.getString("checkIn"),""),nvl(rs.getString("checkOut"),"")});
                tot++; if("Present".equals(st)||"Late".equals(st)) p++; else ab++;
            }
            statsLabel.setText("Present: "+p+"  Absent: "+ab+"  Total: "+tot);
            rs.close();
        }catch(SQLException e){
            JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markAllPresent(){
        String time=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        for(int i=0;i<model.getRowCount();i++){
            model.setValueAt("Present",i,3);
            model.setValueAt(time,i,4);
        }
    }

    private void saveAttendance(){
        try{
            Connection conn=DBConnection.getConnection();
            for(int i=0;i<model.getRowCount();i++){
                String empId=(String)model.getValueAt(i,0);
                String status=nvl((String)model.getValueAt(i,3),"Absent");
                String checkIn=nvl((String)model.getValueAt(i,4),"");
                String checkOut=nvl((String)model.getValueAt(i,5),"");
                PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO attendance (empId,date,status,checkIn,checkOut) VALUES (?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE status=?,checkIn=?,checkOut=?");
                ps.setString(1,empId); ps.setString(2,selectedDate); ps.setString(3,status);
                ps.setString(4,checkIn); ps.setString(5,checkOut);
                ps.setString(6,status); ps.setString(7,checkIn); ps.setString(8,checkOut);
                ps.executeUpdate(); ps.close();
            }
            NotificationManager.add("Attendance","Attendance saved for "+selectedDate,"info");
            UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"Attendance saved!",false);
            loadAttendance();
        }catch(SQLException e){
            JOptionPane.showMessageDialog(this,"Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleTable(){
        table.setBackground(Theme.BG_DEEP); table.setForeground(Theme.TEXT_PRI);
        table.setFont(Theme.FONT_MED); table.setRowHeight(40); table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(Theme.BG_CARD);
        JTableHeader h=table.getTableHeader(); h.setBackground(Theme.BG_PANEL);
        h.setForeground(Theme.TEXT_MUT); h.setFont(Theme.FONT_SMALL);
        h.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));
        int[] w={100,160,150,110,90,90};
        for(int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
    }

    private void styleSmallBtn(JButton b){
        b.setFont(Theme.FONT_SMALL); b.setForeground(Theme.TEXT_SEC);
        b.setBackground(Theme.BG_CARD); b.setBorder(new EmptyBorder(4,8,4,8));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String nvl(String s,String d){return(s==null||s.isEmpty())?d:s;}

    class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int r,int c){
            String s=val==null?"Absent":val.toString();
            Color fg=switch2(s);
            JLabel l=new JLabel(s,SwingConstants.CENTER); l.setFont(new Font("Segoe UI",Font.BOLD,11));
            l.setForeground(fg); l.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL); l.setOpaque(true);
            return l;
        }
        Color switch2(String s){
            if("Present".equals(s))  return Theme.ACCENT_GRN;
            if("Absent".equals(s))   return Theme.ACCENT_RED;
            if("Late".equals(s))     return Theme.ACCENT_YLW;
            if("Half Day".equals(s)) return Theme.ACCENT_BLU;
            return Theme.TEXT_MUT;
        }
    }
}