package employee.management;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;

public class EmployeePanel extends JPanel {
    private MainFrame parent;
    private DefaultTableModel tableModel;
    private JTable table;
    private UI.DarkField searchField;
    private UI.DarkCombo filterDesig;
    private JLabel countLabel;
    private List<Object[]> allRows = new ArrayList<>();
    private static final String[] COLS = {"#","Employee","Designation","Phone","Email","Salary","Status","Actions"};

    public EmployeePanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        buildUI();
        loadEmployees();
    }

    private void buildUI() {
        // Topbar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Theme.BG_PANEL);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER),new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel(SessionManager.isEmployee() ? "  My Profile" : "  Employees");
        title.setFont(Theme.FONT_TITLE); title.setForeground(Theme.TEXT_PRI);
        countLabel = new JLabel(""); countLabel.setFont(Theme.FONT_SMALL); countLabel.setForeground(Theme.TEXT_MUT);
        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        rightBar.setBackground(Theme.BG_PANEL);
        rightBar.add(countLabel);
        // Export Excel button - visible to HR/Admin
        if(SessionManager.isHR()){
            UI.DarkButton exportBtn = UI.DarkButton.normal("⬇ Export Excel");
            exportBtn.addActionListener(e -> ExcelExporter.exportEmployees(
                (JFrame)SwingUtilities.getWindowAncestor(this)));
            UI.DarkButton idCardBtn = UI.DarkButton.normal("🪪 ID Cards");
            idCardBtn.addActionListener(e -> openIDCardGenerator());
            UI.DarkButton addBtn = UI.DarkButton.primary("+ Add Employee");
            addBtn.addActionListener(e -> openDialog(null));
            rightBar.add(exportBtn);
            rightBar.add(idCardBtn);
            rightBar.add(addBtn);
        }
        topbar.add(title, BorderLayout.WEST);
        topbar.add(rightBar, BorderLayout.EAST);

        // Search row — hide for employee role
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
        searchRow.setBackground(Theme.BG_DEEP);
        searchRow.setBorder(new EmptyBorder(8,14,4,14));
        if (!SessionManager.isEmployee()) {
            searchField = new UI.DarkField("Search by name, ID, designation, email...");
            searchField.setPreferredSize(new Dimension(320,32));
            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                public void insertUpdate(javax.swing.event.DocumentEvent e){filterTable();}
                public void removeUpdate(javax.swing.event.DocumentEvent e){filterTable();}
                public void changedUpdate(javax.swing.event.DocumentEvent e){}
            });
            filterDesig = new UI.DarkCombo(new String[]{"All Designations"});
            filterDesig.setPreferredSize(new Dimension(180,32));
            filterDesig.addActionListener(e -> filterTable());
            UI.DarkButton refreshBtn = UI.DarkButton.normal("⟳ Refresh");
            refreshBtn.addActionListener(e -> loadEmployees());
            searchRow.add(searchField); searchRow.add(filterDesig); searchRow.add(refreshBtn);
        } else {
            searchField = new UI.DarkField(""); // dummy, not shown
            filterDesig = new UI.DarkCombo(new String[]{"All Designations"});
        }

        // Table
        tableModel = new DefaultTableModel(COLS, 0){public boolean isCellEditable(int r,int c){return c==7;}};
        table = new JTable(tableModel){
            public Component prepareRenderer(TableCellRenderer r,int row,int col){
                Component c=super.prepareRenderer(r,row,col);
                c.setBackground(row%2==0?Theme.BG_DEEP:Theme.BG_PANEL);
                c.setForeground(Theme.TEXT_PRI);
                if(isRowSelected(row)) c.setBackground(Theme.BG_CARD);
                return c;
            }
        };
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new CompoundBorder(new EmptyBorder(0,14,14,14),new UI.RoundBorder(Theme.BORDER,10)));
        scroll.setBackground(Theme.BG_DEEP); scroll.getViewport().setBackground(Theme.BG_DEEP);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG_DEEP);
        if (!SessionManager.isEmployee()) body.add(searchRow, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        add(topbar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private void styleTable() {
        table.setBackground(Theme.BG_DEEP); table.setForeground(Theme.TEXT_PRI);
        table.setFont(Theme.FONT_MED); table.setRowHeight(54); table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(Theme.BG_CARD); table.setSelectionForeground(Theme.TEXT_PRI);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.BG_PANEL); header.setForeground(Theme.TEXT_MUT);
        header.setFont(Theme.FONT_SMALL); header.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));
        header.setReorderingAllowed(false);
        int[] widths={40,185,125,115,175,95,65,210};
        for(int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumn("Actions").setCellRenderer(new ActionCellRenderer());
        table.getColumn("Actions").setCellEditor(new ActionCellEditor());
        table.getColumn("Status").setCellRenderer(new StatusCellRenderer());
        table.getColumn("Employee").setCellRenderer(new EmployeeCellRenderer());
        table.getColumn("Salary").setCellRenderer(new SalaryCellRenderer());
    }

    public void loadEmployees() {
        allRows.clear(); tableModel.setRowCount(0);
        Set<String> desigs = new LinkedHashSet<>(); desigs.add("All Designations");
        try {
            Connection conn = DBConnection.getConnection();
            // Employee role: only show their own record
            String sql = SessionManager.isEmployee()
                ? "SELECT empId,name,fname,designation,phone,email,salary,photo FROM employee WHERE empId='"
                  + SessionManager.empId + "'"
                : "SELECT empId,name,fname,designation,phone,email,salary,photo FROM employee ORDER BY created_at DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            int i=1;
            while(rs.next()){
                String id=rs.getString("empId"), name=rs.getString("name");
                String desig=nvl(rs.getString("designation"),"—"), phone=nvl(rs.getString("phone"),"—");
                String email=nvl(rs.getString("email"),"—"), sal=nvl(rs.getString("salary"),"0");
                String photo=nvl(rs.getString("photo"),"");
                desigs.add(desig);
                allRows.add(new Object[]{i++, name+"|"+id+"|"+photo, desig, phone, email, sal, "Active", id});
            }
            rs.close();
        } catch(SQLException e){
            JOptionPane.showMessageDialog(this,"DB Error: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        String selectedDesig=(String)filterDesig.getSelectedItem();
        filterDesig.removeAllItems();
        for(String d:desigs) filterDesig.addItem(d);
        if(selectedDesig!=null) filterDesig.setSelectedItem(selectedDesig);
        filterTable();
    }

    private void filterTable(){
        String q=searchField.getText().toLowerCase().trim();
        String fd=(String)filterDesig.getSelectedItem();
        if("All Designations".equals(fd)) fd=null;
        tableModel.setRowCount(0); int shown=0;
        for(Object[] row:allRows){
            String nameId=row[1].toString().toLowerCase(), desig=row[2].toString().toLowerCase();
            String phone=row[3].toString().toLowerCase(), email=row[4].toString().toLowerCase();
            boolean matchQ=q.isEmpty()||nameId.contains(q)||desig.contains(q)||phone.contains(q)||email.contains(q);
            boolean matchD=fd==null||row[2].toString().equals(fd);
            if(matchQ&&matchD){tableModel.addRow(row);shown++;}
        }
        countLabel.setText(shown+" employees");
    }

    // ── MAIN DIALOG ───────────────────────────────────────────────────────────
    private void openDialog(String empId) {
        boolean isEdit = empId != null;
        ResultSet existing = null;
        if(isEdit){
            try{
                Connection conn=DBConnection.getConnection();
                PreparedStatement ps=conn.prepareStatement("SELECT * FROM employee WHERE empId=?");
                ps.setString(1,empId); existing=ps.executeQuery();
                if(!existing.next()){existing=null;}
            }catch(SQLException e){JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); return;}
        }
        final ResultSet rs = existing;

        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
            isEdit?"Edit Employee":"Add Employee", true);
        dialog.setSize(680, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(Theme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL);

        // ── Header bar ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x13,0x18,0x24));
        header.setBorder(new EmptyBorder(14,20,14,20));
        JLabel heading = new JLabel(isEdit ? "✎  Edit Employee" : "+  Add New Employee");
        heading.setFont(new Font("Segoe UI",Font.BOLD,15)); heading.setForeground(Theme.TEXT_PRI);
        JLabel subh = new JLabel(isEdit ? "Update employee information" : "Fill in the details below");
        subh.setFont(Theme.FONT_SMALL); subh.setForeground(Theme.TEXT_MUT);
        JPanel htxt = new JPanel(new GridLayout(2,1,0,2)); htxt.setBackground(header.getBackground());
        htxt.add(heading); htxt.add(subh);
        UI.DarkButton closeBtn = UI.DarkButton.normal("✕");
        closeBtn.setPreferredSize(new Dimension(32,28));
        closeBtn.addActionListener(e->dialog.dispose());
        header.add(htxt, BorderLayout.WEST); header.add(closeBtn, BorderLayout.EAST);

        // ── Content (scroll) ──────────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(16,0));
        content.setBackground(Theme.BG_PANEL);
        content.setBorder(new EmptyBorder(16,20,0,20));

        // LEFT: Photo panel
        JPanel photoPanel = buildPhotoPanel();
        final String[] photoPathHolder = {""};
        JButton photoBtn = (JButton) photoPanel.getClientProperty("btn");
        JLabel photoLabel = (JLabel) photoPanel.getClientProperty("label");
        if(photoBtn != null) photoBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Images","jpg","jpeg","png","gif"));
            if(fc.showOpenDialog(dialog)==JFileChooser.APPROVE_OPTION){
                File f = fc.getSelectedFile();
                photoPathHolder[0] = f.getAbsolutePath();
                try{
                    BufferedImage img = ImageIO.read(f);
                    Image scaled = img.getScaledInstance(80,80,Image.SCALE_SMOOTH);
                    photoLabel.setIcon(new ImageIcon(scaled));
                    photoLabel.setText("");
                }catch(IOException ex){photoLabel.setText("Error");}
            }
        });

        // If editing, load existing photo
        if(isEdit && rs!=null){
            try{
                String ph = nvl(rs.getString("photo"),"");
                if(!ph.isEmpty()){ File f=new File(ph);
                    if(f.exists()){ photoPathHolder[0]=ph;
                        BufferedImage img=ImageIO.read(f);
                        Image scaled=img.getScaledInstance(80,80,Image.SCALE_SMOOTH);
                        photoLabel.setIcon(new ImageIcon(scaled)); photoLabel.setText(""); }
                }
            }catch(Exception ignored){}
        }

        // RIGHT: Fields grid
        JPanel fieldsWrap = new JPanel();
        fieldsWrap.setLayout(new BoxLayout(fieldsWrap, BoxLayout.Y_AXIS));
        fieldsWrap.setBackground(Theme.BG_PANEL);

        UI.DarkField fId    = mkField("Employee ID",   "EMP-001");
        UI.DarkField fName  = mkField("Full Name",     "e.g. Rahul Kumar");
        UI.DarkField fFname = mkField("Father's Name", "e.g. Suresh Kumar");
        UI.DarkField fDob   = mkField("Date of Birth", "YYYY-MM-DD");
        UI.DarkField fDesig = mkField("Designation",   "e.g. Software Engineer");
        UI.DarkField fPhone = mkField("Phone",         "+91 XXXXX XXXXX");
        UI.DarkField fEmail = mkField("Email",         "email@company.com");
        UI.DarkField fSal   = mkField("Salary (₹)",   "e.g. 75000");
        UI.DarkField fEdu   = mkField("Education",     "e.g. B.Tech");
        UI.DarkField fAddr  = mkField("Address",       "Full address");
        UI.DarkField fAadh  = mkField("Aadhaar No.",   "12-digit number");

        if(isEdit && rs!=null){
            try{
                fId.setText(nvl(rs.getString("empId"),"")); fId.setEditable(false);
                fId.setBackground(new Color(0x1a,0x1a,0x2e));
                fName.setText(nvl(rs.getString("name"),""));
                fFname.setText(nvl(rs.getString("fname"),""));
                fDob.setText(nvl(rs.getString("dob"),""));
                fDesig.setText(nvl(rs.getString("designation"),""));
                fPhone.setText(nvl(rs.getString("phone"),""));
                fEmail.setText(nvl(rs.getString("email"),""));
                fSal.setText(nvl(rs.getString("salary"),""));
                fEdu.setText(nvl(rs.getString("education"),""));
                fAddr.setText(nvl(rs.getString("address"),""));
                fAadh.setText(nvl(rs.getString("aadhaar"),""));
            }catch(SQLException ex){ex.printStackTrace();}
        }

        // Section: Personal Info
        fieldsWrap.add(sectionLabel("Personal Information"));
        fieldsWrap.add(Box.createVerticalStrut(6));
        fieldsWrap.add(makeFieldRow(fId, "Employee ID*", fName, "Full Name*"));
        fieldsWrap.add(Box.createVerticalStrut(8));
        fieldsWrap.add(makeFieldRow(fFname, "Father's Name", fDob, "Date of Birth"));
        fieldsWrap.add(Box.createVerticalStrut(8));
        fieldsWrap.add(makeFieldRow(fAadh, "Aadhaar Number", fEdu, "Education"));
        fieldsWrap.add(Box.createVerticalStrut(14));

        // Section: Job Info
        fieldsWrap.add(sectionLabel("Job Information"));
        fieldsWrap.add(Box.createVerticalStrut(6));
        fieldsWrap.add(makeFieldRow(fDesig, "Designation*", fSal, "Salary (₹)"));
        fieldsWrap.add(Box.createVerticalStrut(14));

        // Section: Contact Info
        fieldsWrap.add(sectionLabel("Contact Information"));
        fieldsWrap.add(Box.createVerticalStrut(6));
        fieldsWrap.add(makeFieldRow(fPhone, "Phone Number", fEmail, "Email Address"));
        fieldsWrap.add(Box.createVerticalStrut(8));
        fieldsWrap.add(makeFullRow(fAddr, "Address"));
        fieldsWrap.add(Box.createVerticalStrut(14));

        // Section: Login Credentials (only for new employee)
        UI.DarkField fLoginUser = mkField("Login Username", "e.g. rahul");
        UI.DarkPassField fLoginPass = new UI.DarkPassField("Min 6 characters");
        fLoginPass.setPreferredSize(new Dimension(0,34));
        fLoginPass.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));

        if(!isEdit){
            fieldsWrap.add(sectionLabel("Login Credentials"));
            fieldsWrap.add(Box.createVerticalStrut(6));

            JPanel loginNote = new JPanel(new BorderLayout());
            loginNote.setBackground(Theme.BG_PANEL);
            loginNote.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));
            JLabel note = new JLabel("ℹ  Employee will use these to login with 'Employee' role");
            note.setFont(new Font("Segoe UI",Font.ITALIC,10));
            note.setForeground(Theme.ACCENT_YLW);
            loginNote.add(note, BorderLayout.WEST);
            fieldsWrap.add(loginNote);
            fieldsWrap.add(Box.createVerticalStrut(6));

            // Auto-fill username from name
            fName.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                public void insertUpdate(javax.swing.event.DocumentEvent e){ autoFill(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e){ autoFill(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e){}
                void autoFill(){
                    String n=fName.getText().trim().toLowerCase().split(" ")[0];
                    if(!n.isEmpty()) fLoginUser.setText(n);
                }
            });
            // Auto-fill password from phone
            fPhone.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                public void insertUpdate(javax.swing.event.DocumentEvent e){ autoFillPass(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e){ autoFillPass(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e){}
                void autoFillPass(){
                    String p=fPhone.getText().replaceAll("[^0-9]","");
                    if(p.length()>=6) fLoginPass.setText(p.substring(p.length()-6));
                }
            });

            JPanel loginRow = new JPanel(new GridLayout(1,2,12,0));
            loginRow.setBackground(Theme.BG_PANEL);
            loginRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
            loginRow.add(labeledField(fLoginUser,"Username*"));
            loginRow.add(labeledField2(fLoginPass,"Password*"));
            fieldsWrap.add(loginRow);
            fieldsWrap.add(Box.createVerticalStrut(10));
        }

        JScrollPane fieldScroll = new JScrollPane(fieldsWrap);
        fieldScroll.setBorder(null); fieldScroll.setBackground(Theme.BG_PANEL);
        fieldScroll.getViewport().setBackground(Theme.BG_PANEL);
        fieldScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        content.add(photoPanel, BorderLayout.WEST);
        content.add(fieldScroll, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BG_PANEL);
        footer.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,Theme.BORDER),new EmptyBorder(12,20,14,20)));

        JLabel required = new JLabel("* Required fields");
        required.setFont(Theme.FONT_SMALL); required.setForeground(Theme.TEXT_MUT);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton cancel = UI.DarkButton.normal("Cancel");
        cancel.setPreferredSize(new Dimension(90,36));
        cancel.addActionListener(e->dialog.dispose());

        UI.DarkButton save = isEdit ? UI.DarkButton.primary("  Update Employee") : UI.DarkButton.success("  Save Employee");
        save.setPreferredSize(new Dimension(160,36));
        save.addActionListener(e->{
            String id=fId.getText().trim(), name=fName.getText().trim(), desig=fDesig.getText().trim();
            if(id.isEmpty()||name.isEmpty()||desig.isEmpty()){
                showFieldError(dialog,"Employee ID, Name and Designation are required."); return;
            }
            // Validate login fields for new employee
            String loginUser="", loginPass="";
            if(!isEdit){
                loginUser=fLoginUser.getText().trim();
                loginPass=new String(fLoginPass.getPassword()).trim();
                if(loginUser.isEmpty()||loginPass.isEmpty()){
                    showFieldError(dialog,"Username and Password are required for login."); return;
                }
                if(loginPass.length()<6){
                    showFieldError(dialog,"Password must be at least 6 characters."); return;
                }
            }
            final String finalLoginUser=loginUser, finalLoginPass=loginPass;
            try{
                Connection conn=DBConnection.getConnection();
                // Save photo to project photos dir
                String savedPhoto = photoPathHolder[0];
                if(!savedPhoto.isEmpty() && !savedPhoto.startsWith("photos/")){
                    File photosDir = new File("photos");
                    photosDir.mkdirs();
                    String ext = savedPhoto.substring(savedPhoto.lastIndexOf('.')+1);
                    File dest = new File(photosDir, id+"_photo."+ext);
                    Files.copy(Paths.get(savedPhoto), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    savedPhoto = "photos/"+dest.getName();
                }

                // Check if photo column exists, add if not
                ensurePhotoColumn(conn);

                if(!isEdit){
                    PreparedStatement chk=conn.prepareStatement("SELECT empId FROM employee WHERE empId=?");
                    chk.setString(1,id);
                    if(chk.executeQuery().next()){showFieldError(dialog,"Employee ID already exists!"); chk.close(); return;}
                    chk.close();
                    PreparedStatement ps=conn.prepareStatement(
                        "INSERT INTO employee (empId,name,fname,dob,salary,address,phone,email,education,designation,aadhaar,photo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
                    ps.setString(1,id); ps.setString(2,fName.getText().trim()); ps.setString(3,fFname.getText().trim());
                    ps.setString(4,fDob.getText().trim()); ps.setString(5,fSal.getText().trim());
                    ps.setString(6,fAddr.getText().trim()); ps.setString(7,fPhone.getText().trim());
                    ps.setString(8,fEmail.getText().trim()); ps.setString(9,fEdu.getText().trim());
                    ps.setString(10,fDesig.getText().trim()); ps.setString(11,fAadh.getText().trim());
                    ps.setString(12,savedPhoto);
                    ps.executeUpdate(); ps.close();

                    // Create login account for the new employee
                    try{
                        // Check if username already exists
                        PreparedStatement lchk=conn.prepareStatement("SELECT id FROM login WHERE username=?");
                        lchk.setString(1,finalLoginUser);
                        if(lchk.executeQuery().next()){
                            showFieldError(dialog,"Username '"+finalLoginUser+"' already exists! Choose a different one.");
                            lchk.close(); return;
                        }
                        lchk.close();
                        PreparedStatement lps=conn.prepareStatement(
                            "INSERT INTO login (username,password,role,empId) VALUES (?,?,'employee',?)");
                        lps.setString(1,finalLoginUser);
                        lps.setString(2,finalLoginPass);
                        lps.setString(3,id);
                        lps.executeUpdate(); lps.close();
                    }catch(SQLException lex){
                        JOptionPane.showMessageDialog(dialog,"Employee saved but login creation failed: "+lex.getMessage());
                    }

                    // Show success dialog with credentials
                    showCredentialsDialog(dialog, fName.getText().trim(), finalLoginUser, finalLoginPass);
                    NotificationManager.add("New Employee","Login created for "+fName.getText().trim(),"info");
                }else{
                    PreparedStatement ps=conn.prepareStatement(
                        "UPDATE employee SET name=?,fname=?,dob=?,salary=?,address=?,phone=?,email=?,education=?,designation=?,aadhaar=?,photo=? WHERE empId=?");
                    ps.setString(1,fName.getText().trim()); ps.setString(2,fFname.getText().trim());
                    ps.setString(3,fDob.getText().trim()); ps.setString(4,fSal.getText().trim());
                    ps.setString(5,fAddr.getText().trim()); ps.setString(6,fPhone.getText().trim());
                    ps.setString(7,fEmail.getText().trim()); ps.setString(8,fEdu.getText().trim());
                    ps.setString(9,fDesig.getText().trim()); ps.setString(10,fAadh.getText().trim());
                    ps.setString(11,savedPhoto); ps.setString(12,id);
                    ps.executeUpdate(); ps.close();
                    UI.showToast((JFrame)SwingUtilities.getWindowAncestor(EmployeePanel.this),"Employee updated successfully!",false);
                }
                dialog.dispose(); loadEmployees();
            }catch(Exception ex){
                JOptionPane.showMessageDialog(dialog,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        btns.add(cancel); btns.add(save);
        footer.add(required, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    /** Shows credentials popup after creating employee */
    private void showCredentialsDialog(JDialog parent, String name, String username, String password){
        JDialog d=new JDialog(parent,"Employee Created",true);
        d.setSize(380,280); d.setLocationRelativeTo(parent);
        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL); root.setBorder(new EmptyBorder(24,28,24,28));

        JLabel icon=new JLabel("✓",SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI",Font.BOLD,32)); icon.setForeground(Theme.ACCENT_GRN);

        JLabel msg=new JLabel(name+" added successfully!",SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI",Font.BOLD,14)); msg.setForeground(Theme.TEXT_PRI);

        JLabel sub=new JLabel("Share these login credentials with the employee:",SwingConstants.CENTER);
        sub.setFont(Theme.FONT_SMALL); sub.setForeground(Theme.TEXT_MUT);

        // Credentials box
        JPanel credBox=new JPanel(new GridLayout(2,1,0,6)); credBox.setBackground(Theme.BG_CARD);
        credBox.setBorder(new CompoundBorder(new UI.RoundBorder(Theme.BORDER,8),new EmptyBorder(10,16,10,16)));

        JLabel uLbl=new JLabel("Username:  "+username+"     Role: Employee");
        uLbl.setFont(new Font("Consolas",Font.BOLD,13)); uLbl.setForeground(Theme.ACCENT_BLU);
        JLabel pLbl=new JLabel("Password:  "+password);
        pLbl.setFont(new Font("Consolas",Font.BOLD,13)); pLbl.setForeground(Theme.ACCENT_GRN);
        credBox.add(uLbl); credBox.add(pLbl);

        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0)); btns.setBackground(Theme.BG_PANEL);
        UI.DarkButton ok=UI.DarkButton.success("  OK  ");
        ok.setPreferredSize(new Dimension(100,36));
        ok.addActionListener(e->d.dispose());
        btns.add(ok);

        JPanel center=new JPanel(); center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        center.setBackground(Theme.BG_PANEL);
        for(JComponent c:new JComponent[]{icon,msg,sub}){
            c.setAlignmentX(Component.CENTER_ALIGNMENT); center.add(c); center.add(Box.createVerticalStrut(6));
        }
        center.add(Box.createVerticalStrut(8)); center.add(credBox);

        root.add(center,BorderLayout.CENTER); root.add(btns,BorderLayout.SOUTH);
        d.setContentPane(root); d.setVisible(true);
    }

    /** Like labeledField but for JPasswordField */
    private JPanel labeledField2(UI.DarkPassField field, String label){
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG_PANEL);
        JLabel l=new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        p.add(l); p.add(Box.createVerticalStrut(3)); p.add(field);
        return p;
    }

    private void ensurePhotoColumn(Connection conn){
        try{conn.createStatement().execute("ALTER TABLE employee ADD COLUMN photo VARCHAR(255) DEFAULT ''");}
        catch(SQLException ignored){}
    }

    // ── Photo panel ───────────────────────────────────────────────────────────
    private JPanel buildPhotoPanel(){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(new EmptyBorder(0,0,0,16));
        p.setPreferredSize(new Dimension(130, 0));

        JPanel circle = new JPanel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD); g2.fillOval(0,0,getWidth()-1,getHeight()-1);
                g2.setColor(Theme.BORDER);  g2.drawOval(0,0,getWidth()-1,getHeight()-1);
                g2.dispose();
            }
        };
        circle.setLayout(new BorderLayout());
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(90,90));
        circle.setMaximumSize(new Dimension(90,90));
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel photoLabel = new JLabel("Photo", SwingConstants.CENTER);
        photoLabel.setFont(Theme.FONT_SMALL); photoLabel.setForeground(Theme.TEXT_MUT);
        circle.add(photoLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("Profile Picture", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI",Font.PLAIN,10)); hint.setForeground(Theme.TEXT_MUT);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        UI.DarkButton uploadBtn = UI.DarkButton.normal("Upload");
        uploadBtn.setFont(new Font("Segoe UI",Font.PLAIN,11));
        uploadBtn.setMaximumSize(new Dimension(90,28));
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalStrut(4));
        p.add(circle);
        p.add(Box.createVerticalStrut(6));
        p.add(hint);
        p.add(Box.createVerticalStrut(8));
        p.add(uploadBtn);

        p.putClientProperty("btn", uploadBtn);
        p.putClientProperty("label", photoLabel);
        return p;
    }

    // ── Layout helpers ────────────────────────────────────────────────────────
    private JPanel sectionLabel(String text){
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(Theme.BG_PANEL);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));
        JLabel l=new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI",Font.BOLD,10)); l.setForeground(Theme.ACCENT_BLU);
        JPanel line=new JPanel(); line.setBackground(Theme.BORDER);
        line.setPreferredSize(new Dimension(0,1)); line.setBorder(new EmptyBorder(0,8,0,0));
        p.add(l,BorderLayout.WEST); p.add(line,BorderLayout.CENTER);
        return p;
    }

    private UI.DarkField mkField(String label, String placeholder){
        UI.DarkField f=new UI.DarkField(placeholder);
        f.setPreferredSize(new Dimension(0,34)); f.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        f.putClientProperty("label",label);
        return f;
    }

    private JPanel makeFieldRow(UI.DarkField f1, String l1, UI.DarkField f2, String l2){
        JPanel p=new JPanel(new GridLayout(1,2,12,0));
        p.setBackground(Theme.BG_PANEL); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        p.add(labeledField(f1,l1)); p.add(labeledField(f2,l2));
        return p;
    }

    private JPanel makeFullRow(UI.DarkField f, String label){
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_PANEL); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        p.add(labeledField(f,label), BorderLayout.CENTER);
        return p;
    }

    private JPanel labeledField(UI.DarkField field, String label){
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(Theme.BG_PANEL);
        JLabel l=new JLabel(label); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(3)); p.add(field);
        return p;
    }

    private void showFieldError(JDialog d, String msg){
        JOptionPane.showMessageDialog(d,msg,"Validation",JOptionPane.WARNING_MESSAGE);
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    private void confirmDelete(String empId, String name){
        JDialog d=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Confirm Delete",true);
        d.setSize(360,260); d.setLocationRelativeTo(this);
        d.setResizable(false);

        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL);
        root.setBorder(new EmptyBorder(28,32,24,32));

        // Icon
        JLabel icon=new JLabel("\uD83D\uDDD1", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji",Font.PLAIN,36));
        icon.setForeground(Theme.ACCENT_RED);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg1=new JLabel("Delete Employee?",SwingConstants.CENTER);
        msg1.setFont(new Font("Segoe UI",Font.BOLD,15));
        msg1.setForeground(Theme.TEXT_PRI);
        msg1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg2=new JLabel(name,SwingConstants.CENTER);
        msg2.setFont(new Font("Segoe UI",Font.BOLD,13));
        msg2.setForeground(Theme.ACCENT_RED);
        msg2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg3=new JLabel("This action cannot be undone.",SwingConstants.CENTER);
        msg3.setFont(Theme.FONT_SMALL);
        msg3.setForeground(Theme.TEXT_MUT);
        msg3.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel center=new JPanel();
        center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        center.setBackground(Theme.BG_PANEL);
        center.add(icon);          center.add(Box.createVerticalStrut(8));
        center.add(msg1);          center.add(Box.createVerticalStrut(4));
        center.add(msg2);          center.add(Box.createVerticalStrut(4));
        center.add(msg3);

        // Buttons
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER,10,0));
        btns.setBackground(Theme.BG_PANEL);
        btns.setBorder(new EmptyBorder(16,0,0,0));

        UI.DarkButton cancel=UI.DarkButton.normal("  Cancel  ");
        cancel.setPreferredSize(new Dimension(110,36));
        cancel.addActionListener(e->d.dispose());

        UI.DarkButton del=UI.DarkButton.danger("  Delete  ");
        del.setPreferredSize(new Dimension(110,36));
        del.addActionListener(e->{
            try{
                Connection conn=DBConnection.getConnection();
                PreparedStatement ps=conn.prepareStatement("DELETE FROM employee WHERE empId=?");
                ps.setString(1,empId); ps.executeUpdate(); ps.close();
                UI.showToast((JFrame)SwingUtilities.getWindowAncestor(this),"Employee deleted.",true);
                d.dispose(); loadEmployees();
            }catch(SQLException ex){JOptionPane.showMessageDialog(d,"Error: "+ex.getMessage());}
        });
        btns.add(cancel); btns.add(del);

        root.add(center,BorderLayout.CENTER);
        root.add(btns,BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

    private String nvl(String s,String def){return(s==null||s.isEmpty())?def:s;}

    // ── Cell Renderers ────────────────────────────────────────────────────────
    class EmployeeCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int r,int c){
            JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,10,6));
            Color bg=r%2==0?Theme.BG_DEEP:Theme.BG_PANEL; if(sel)bg=Theme.BG_CARD;
            p.setBackground(bg);
            String[] parts=val.toString().split("\\|");
            String name=parts[0], id=parts.length>1?parts[1]:"", photo=parts.length>2?parts[2]:"";
            // Try to show photo, fallback to avatar
            if(!photo.isEmpty()){
                File f=new File(photo);
                if(f.exists()){
                    try{BufferedImage img=ImageIO.read(f); Image scaled=img.getScaledInstance(32,32,Image.SCALE_SMOOTH);
                        JLabel imgLbl=new JLabel(new ImageIcon(scaled)); p.add(imgLbl);
                    }catch(IOException ignored){p.add(new UI.AvatarPanel(name,r,32));}
                }else p.add(new UI.AvatarPanel(name,r,32));
            }else p.add(new UI.AvatarPanel(name,r,32));
            JPanel info=new JPanel(new GridLayout(2,1,0,1)); info.setBackground(bg);
            JLabel nm=new JLabel(name); nm.setFont(Theme.FONT_MED); nm.setForeground(Theme.TEXT_PRI);
            JLabel idl=new JLabel(id); idl.setFont(Theme.FONT_SMALL); idl.setForeground(Theme.TEXT_MUT);
            info.add(nm); info.add(idl); p.add(info); return p;
        }
    }
    class SalaryCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int r,int c){
            JLabel l=new JLabel("₹"+fmt(val.toString())); l.setFont(Theme.FONT_MED); l.setForeground(Theme.ACCENT_GRN);
            l.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL); l.setOpaque(true); l.setBorder(new EmptyBorder(0,12,0,0));
            return l;
        }
        private String fmt(String s){try{return String.format("%,d",Long.parseLong(s));}catch(Exception e){return s;}}
    }
    class StatusCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int r,int c){
            JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,0,10));
            p.setBackground(r%2==0?Theme.BG_DEEP:Theme.BG_PANEL);
            p.add(UI.BadgeLabel.green("Active")); return p;
        }
    }
    class ActionCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int r,int c){
            JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,4,11));
            Color bg=r%2==0?Theme.BG_DEEP:Theme.BG_PANEL;
            p.setBackground(bg);
            p.add(makeActionBtn("👁 View",  new Color(0x1e,0x25,0x35), Theme.ACCENT_BLU, new Color(0x2a,0x4a,0x6f)));
            if(SessionManager.isHR()){
                p.add(makeActionBtn("✎ Edit",   new Color(0x1e,0x25,0x35), Theme.TEXT_SEC,  Theme.BORDER));
                p.add(makeActionBtn("✕ Delete", new Color(0x3a,0x10,0x10), Theme.ACCENT_RED,new Color(0x5a,0x15,0x15)));
            }
            return p;
        }
    }

    class ActionCellEditor extends DefaultCellEditor {
        private JPanel panel; private String currentEmpId, currentName;
        ActionCellEditor(){super(new JCheckBox()); setClickCountToStart(1);}
        public Component getTableCellEditorComponent(JTable t,Object val,boolean sel,int r,int c){
            Object nameId=t.getValueAt(r,1); String[] parts=nameId.toString().split("\\|");
            currentName=parts[0]; currentEmpId=parts.length>1?parts[1]:"";
            panel=new JPanel(new FlowLayout(FlowLayout.CENTER,4,11));
            panel.setBackground(Theme.BG_CARD);

            JButton view=makeActionBtn("👁 View",new Color(0x1e,0x25,0x35),Theme.ACCENT_BLU,new Color(0x2a,0x4a,0x6f));
            view.addActionListener(e->{ stopCellEditing(); openProfile(currentEmpId); });
            panel.add(view);

            if(SessionManager.isHR()){
                JButton edit=makeActionBtn("✎ Edit",new Color(0x1e,0x25,0x35),Theme.TEXT_SEC,Theme.BORDER);
                edit.addActionListener(e->{ stopCellEditing(); openDialog(currentEmpId); });
                JButton del=makeActionBtn("✕ Delete",new Color(0x3a,0x10,0x10),Theme.ACCENT_RED,new Color(0x5a,0x15,0x15));
                del.addActionListener(e->{ stopCellEditing(); confirmDelete(currentEmpId,currentName); });
                panel.add(edit); panel.add(del);
            }
            return panel;
        }
        public Object getCellEditorValue(){return "";}
    }

    private JButton makeActionBtn(String text, Color bg, Color fg, Color border){
        JButton b = new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(border);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public Dimension getPreferredSize(){ return new Dimension(78,28); }
        };
        b.setFont(new Font("Segoe UI",Font.PLAIN,11));
        b.setForeground(fg);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void openIDCardGenerator(){
        Container contentArea = this.getParent();
        if(contentArea!=null){
            contentArea.removeAll();
            contentArea.add(new IDCardGenerator(parent), BorderLayout.CENTER);
            contentArea.revalidate(); contentArea.repaint();
        }
    }

    private void openProfile(String empId){
        Container contentArea = this.getParent();
        if(contentArea!=null){
            contentArea.removeAll();
            contentArea.add(new EmployeeProfilePanel(parent,empId), BorderLayout.CENTER);
            contentArea.revalidate(); contentArea.repaint();
        }
    }
}