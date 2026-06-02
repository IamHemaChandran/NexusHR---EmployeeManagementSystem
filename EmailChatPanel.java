package employee.management;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class EmailChatPanel extends JPanel {
    private MainFrame parent;

    // Left: contact list
    private JPanel contactList;
    private UI.DarkField contactSearch;

    // Right: chat area
    private JPanel chatMessages;
    private UI.DarkField messageInput;
    private JLabel chatHeader;
    private JLabel chatSubHeader;
    private UI.AvatarPanel chatAvatar;
    private JPanel chatHeaderPanel;
    private JScrollPane chatScroll;

    private String selectedContact = null;
    private int selectedContactIdx = 0;
    private Map<String, List<Message>> conversations = new LinkedHashMap<>();
    private List<ContactEntry> contacts = new ArrayList<>();

    static class Message {
        final String text; final boolean outgoing; final String time;
        Message(String text, boolean outgoing, String time) { this.text=text; this.outgoing=outgoing; this.time=time; }
        String text() { return text; } boolean outgoing() { return outgoing; } String time() { return time; }
    }
    static class ContactEntry {
        final String name, role, lastMsg; final int idx;
        ContactEntry(String name, String role, String lastMsg, int idx) { this.name=name; this.role=role; this.lastMsg=lastMsg; this.idx=idx; }
        String name() { return name; } String role() { return role; } String lastMsg() { return lastMsg; } int idx() { return idx; }
    }

    public EmailChatPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DEEP);
        loadContacts();
        buildUI();
    }

    private void loadContacts() {
        contacts.clear();
        String[] defaultMessages = {
            "Can you update the salary report?",
            "Meeting at 3 PM today",
            "Please review the new policy doc",
            "Monthly attendance submitted",
            "Q2 appraisal cycle begins soon"
        };
        try {
            Connection conn = DBConnection.getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT name, designation FROM employee ORDER BY created_at DESC LIMIT 10"
            );
            int i = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                String role = nvl(rs.getString("designation"), "Employee");
                contacts.add(new ContactEntry(name, role, defaultMessages[i % defaultMessages.length], i));
                i++;
            }
            rs.close();
        } catch (SQLException e) { /* use defaults */ }

        if (contacts.isEmpty()) {
            contacts.add(new ContactEntry("Rahul Kumar",  "Software Engineer", "Can you review the PR?", 0));
            contacts.add(new ContactEntry("Priya Shah",   "HR Manager",        "Payroll processed ✓",   1));
            contacts.add(new ContactEntry("Arjun Mehta",  "DevOps Lead",       "Server is back online", 2));
            contacts.add(new ContactEntry("Sneha Patel",  "UI Designer",       "Designs sent to you",   3));
        }

        // Seed some demo conversations
        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry c = contacts.get(i);
            List<Message> msgs = new ArrayList<>();
            msgs.add(new Message("Hey! " + c.lastMsg(), false, "10:30 AM"));
            msgs.add(new Message("Thanks for letting me know. I'll get on it right away.", true, "10:32 AM"));
            msgs.add(new Message("Perfect, let me know when it's done.", false, "10:35 AM"));
            conversations.put(c.name(), msgs);
        }
    }

    private void buildUI() {
        // ── Topbar ────────────────────────────────────────────────────────────
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Theme.BG_PANEL);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,Theme.BORDER), new EmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("  Email / Chat");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRI);
        JLabel subtitle = new JLabel("Internal messaging");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.TEXT_MUT);
        JPanel titleCol = new JPanel(new GridLayout(2,1));
        titleCol.setBackground(Theme.BG_PANEL);
        titleCol.add(title); titleCol.add(subtitle);
        topbar.add(titleCol, BorderLayout.WEST);

        // ── Main split ────────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(1);
        split.setDividerLocation(260);
        split.setBorder(null);
        split.setBackground(Theme.BORDER);

        // LEFT: Contact list
        JPanel leftPanel = buildContactListPanel();

        // RIGHT: Chat area
        JPanel rightPanel = buildChatPanel();

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        add(topbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        // Render contact list first, then select
        renderContactList();
        if (!contacts.isEmpty()) selectContact(contacts.get(0));
    }

    private JPanel buildContactListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(new MatteBorder(0,0,0,1,Theme.BORDER));

        // Search
        JPanel searchArea = new JPanel(new BorderLayout());
        searchArea.setBackground(Theme.BG_PANEL);
        searchArea.setBorder(new EmptyBorder(12,12,8,12));
        contactSearch = new UI.DarkField("Search contacts...");
        contactSearch.setPreferredSize(new Dimension(0,32));
        searchArea.add(contactSearch, BorderLayout.CENTER);

        // List
        contactList = new JPanel();
        contactList.setLayout(new BoxLayout(contactList, BoxLayout.Y_AXIS));
        contactList.setBackground(Theme.BG_PANEL);

        JScrollPane scroll = new JScrollPane(contactList);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_PANEL);
        scroll.getViewport().setBackground(Theme.BG_PANEL);

        panel.add(searchArea, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DEEP);

        // Chat header
        chatHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        chatHeaderPanel.setBackground(Theme.BG_PANEL);
        chatHeaderPanel.setBorder(new MatteBorder(0,0,1,0,Theme.BORDER));

        chatAvatar  = new UI.AvatarPanel("?", 0, 34);
        chatHeader  = new JLabel("Select a contact");
        chatHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chatHeader.setForeground(Theme.TEXT_PRI);
        chatSubHeader = new JLabel("");
        chatSubHeader.setFont(Theme.FONT_SMALL);
        chatSubHeader.setForeground(Theme.TEXT_MUT);

        JPanel headerInfo = new JPanel(new GridLayout(2,1,0,1));
        headerInfo.setBackground(Theme.BG_PANEL);
        headerInfo.add(chatHeader);
        headerInfo.add(chatSubHeader);

        UI.DarkButton emailBtn = UI.DarkButton.normal("✉ Send Email");
        emailBtn.addActionListener(e -> sendEmail());

        chatHeaderPanel.add(chatAvatar);
        chatHeaderPanel.add(headerInfo);
        chatHeaderPanel.add(Box.createHorizontalStrut(20));
        chatHeaderPanel.add(emailBtn);

        // Messages area
        chatMessages = new JPanel();
        chatMessages.setLayout(new BoxLayout(chatMessages, BoxLayout.Y_AXIS));
        chatMessages.setBackground(Theme.BG_DEEP);
        chatMessages.setBorder(new EmptyBorder(12, 12, 12, 12));

        chatScroll = new JScrollPane(chatMessages);
        chatScroll.setBorder(null);
        chatScroll.setBackground(Theme.BG_DEEP);
        chatScroll.getViewport().setBackground(Theme.BG_DEEP);

        // Input bar
        JPanel inputBar = new JPanel(new BorderLayout(8,0));
        inputBar.setBackground(Theme.BG_PANEL);
        inputBar.setBorder(new CompoundBorder(
            new MatteBorder(1,0,0,0,Theme.BORDER),
            new EmptyBorder(10,14,10,14)
        ));

        messageInput = new UI.DarkField("Type a message...");
        messageInput.setPreferredSize(new Dimension(0,36));
        messageInput.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        UI.DarkButton sendBtn = UI.DarkButton.primary("Send ↵");
        sendBtn.setPreferredSize(new Dimension(90, 36));
        sendBtn.addActionListener(e -> sendMessage());

        inputBar.add(messageInput, BorderLayout.CENTER);
        inputBar.add(sendBtn, BorderLayout.EAST);

        panel.add(chatHeaderPanel, BorderLayout.NORTH);
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(inputBar, BorderLayout.SOUTH);
        return panel;
    }

    private void renderContactList() {
        contactList.removeAll();
        String q = contactSearch.getText().toLowerCase().trim();
        for (ContactEntry c : contacts) {
            if (!q.isEmpty() && !c.name().toLowerCase().contains(q) && !c.role().toLowerCase().contains(q)) continue;
            contactList.add(buildContactRow(c));
        }
        contactList.revalidate();
        contactList.repaint();
    }

    private JPanel buildContactRow(ContactEntry c) {
        boolean active = c.name().equals(selectedContact);
        JPanel row = new JPanel(new BorderLayout(10,0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setColor(active ? Theme.SIDEBAR_ACTIVE : Theme.BG_PANEL);
                g2.fillRect(0,0,getWidth(),getHeight());
                if (active) {
                    g2.setColor(Theme.ACCENT_BLU);
                    g2.fillRect(0,0,3,getHeight());
                }
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10,12,10,12));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        UI.AvatarPanel av = new UI.AvatarPanel(c.name(), c.idx(), 36);

        JPanel info = new JPanel(new GridLayout(2,1,0,2));
        info.setOpaque(false);
        JLabel nm = new JLabel(c.name()); nm.setFont(new Font("Segoe UI",Font.BOLD,12)); nm.setForeground(active?Theme.TEXT_PRI:Theme.TEXT_SEC);
        JLabel last = new JLabel(c.lastMsg()); last.setFont(Theme.FONT_SMALL); last.setForeground(Theme.TEXT_MUT);
        info.add(nm); info.add(last);

        row.add(av, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { selectContact(c); }
            @Override public void mouseEntered(MouseEvent e) { if (!active) { row.setBackground(Theme.BG_HOVER); row.repaint(); } }
            @Override public void mouseExited(MouseEvent e)  { row.repaint(); }
        });
        return row;
    }

    private void selectContact(ContactEntry c) {
        selectedContact    = c.name();
        selectedContactIdx = c.idx();

        // Safely update avatar in header (it's always the first FlowLayout component)
        chatHeaderPanel.removeAll();
        UI.AvatarPanel newAv = new UI.AvatarPanel(c.name(), c.idx(), 34);
        chatAvatar = newAv;
        chatHeader.setText(c.name());
        chatSubHeader.setText(c.role() + " • Active now");

        // Rebuild header contents
        JPanel headerInfo = new JPanel(new GridLayout(2,1,0,1));
        headerInfo.setBackground(Theme.BG_PANEL);
        headerInfo.add(chatHeader);
        headerInfo.add(chatSubHeader);

        UI.DarkButton emailBtn = UI.DarkButton.normal("✉ Send Email");
        emailBtn.addActionListener(e -> sendEmail());

        chatHeaderPanel.add(newAv);
        chatHeaderPanel.add(headerInfo);
        chatHeaderPanel.add(Box.createHorizontalStrut(20));
        chatHeaderPanel.add(emailBtn);
        chatHeaderPanel.revalidate(); chatHeaderPanel.repaint();

        renderContactList();
        renderMessages();
    }

    private void renderMessages() {
        chatMessages.removeAll();
        List<Message> msgs = conversations.getOrDefault(selectedContact, new ArrayList<>());
        for (Message msg : msgs) {
            chatMessages.add(buildMessageBubble(msg));
            chatMessages.add(Box.createVerticalStrut(8));
        }
        chatMessages.revalidate();
        chatMessages.repaint();
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chatScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JPanel buildMessageBubble(Message msg) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG_DEEP);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));

        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(msg.outgoing() ? Theme.BTN_BLU_BG : Theme.BG_PANEL);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(msg.outgoing() ? new Color(0x2a,0x4a,0x6f) : Theme.BORDER);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(9,13,9,13));

        JLabel text = new JLabel("<html><body style='width:220px'>" + msg.text() + "</body></html>");
        text.setFont(Theme.FONT_MED);
        text.setForeground(msg.outgoing() ? Theme.TEXT_PRI : Theme.TEXT_PRI);

        JLabel time = new JLabel(msg.time());
        time.setFont(new Font("Segoe UI",Font.PLAIN,10));
        time.setForeground(Theme.TEXT_MUT);

        bubble.add(text);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(time);

        JPanel align = new JPanel(new FlowLayout(msg.outgoing() ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        align.setBackground(Theme.BG_DEEP);
        align.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
        bubble.setMaximumSize(new Dimension(300, 999));
        align.add(bubble);
        wrapper.add(align, BorderLayout.CENTER);
        return wrapper;
    }

    private void sendMessage() {
        if (selectedContact == null) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        List<Message> msgs = conversations.computeIfAbsent(selectedContact, k -> new ArrayList<>());
        msgs.add(new Message(text, true, time));
        messageInput.setText("");
        renderMessages();

        // Simulate reply after 1s
        String[] replies = {
            "Got it, thanks!",
            "Sure, I'll look into it.",
            "Understood. Will update you soon.",
            "On it!",
            "Thanks for the heads up.",
        };
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> {
            msgs.add(new Message(replies[new Random().nextInt(replies.length)], false,
                LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))));
            renderMessages();
        });
        t.setRepeats(false);
        t.start();
    }

    private void sendEmail() {
        if (selectedContact == null) return;

        // Lookup actual email from DB
        String[] recipientEmail = {""};
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT email FROM employee WHERE name=? LIMIT 1");
            ps.setString(1, selectedContact);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) recipientEmail[0] = nvl(rs.getString("email"),"");
            rs.close(); ps.close();
        } catch (SQLException ignored) {}

        JDialog d = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Send Email", true);
        d.setSize(500, 480); d.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PANEL); root.setBorder(new EmptyBorder(20,24,20,24));

        JLabel h = new JLabel("✉  Send Email to " + selectedContact);
        h.setFont(Theme.FONT_TITLE); h.setForeground(Theme.TEXT_PRI); h.setBorder(new EmptyBorder(0,0,14,0));

        JPanel form = new JPanel(new GridLayout(0,1,0,6)); form.setBackground(Theme.BG_PANEL);

        UI.DarkField toField  = new UI.DarkField("Recipient email");
        toField.setText(recipientEmail[0]);
        UI.DarkField subField = new UI.DarkField("Subject");
        JTextArea body = new JTextArea(7,30);
        body.setFont(Theme.FONT_MED); body.setForeground(Theme.TEXT_PRI);
        body.setBackground(Theme.BG_INPUT); body.setCaretColor(Theme.TEXT_PRI);
        body.setBorder(new CompoundBorder(new UI.RoundBorder(Theme.BORDER,8),new EmptyBorder(8,10,8,10)));
        body.setLineWrap(true); body.setWrapStyleWord(true);
        JScrollPane bodySP = new JScrollPane(body); bodySP.setBorder(new UI.RoundBorder(Theme.BORDER,8));

        // SMTP config hint
        JLabel smtpHint = new JLabel("ℹ  Configure SMTP in Settings to send real emails");
        smtpHint.setFont(new Font("Segoe UI",Font.ITALIC,10)); smtpHint.setForeground(Theme.ACCENT_YLW);

        addFormLabel(form,"To (email):"); form.add(toField);
        addFormLabel(form,"Subject:"); form.add(subField);
        addFormLabel(form,"Message:"); form.add(bodySP);
        form.add(smtpHint);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        acts.setBackground(Theme.BG_PANEL); acts.setBorder(new EmptyBorder(12,0,0,0));
        UI.DarkButton cancel = UI.DarkButton.normal("Cancel");
        cancel.addActionListener(e2 -> d.dispose());
        UI.DarkButton send = UI.DarkButton.primary("✉ Send Email");
        send.addActionListener(e2 -> {
            String to = toField.getText().trim();
            String sub = subField.getText().trim();
            String msg = body.getText().trim();
            if (to.isEmpty() || sub.isEmpty()) {
                JOptionPane.showMessageDialog(d,"To and Subject are required."); return;
            }
            send.setText("Sending..."); send.setEnabled(false);
            SwingWorker<Boolean,Void> worker = new SwingWorker<>(){
                @Override protected Boolean doInBackground(){
                    return EmailSender.send(to, sub, msg);
                }
                @Override protected void done(){
                    try {
                        boolean ok = get();
                        d.dispose();
                        List<Message> msgs = conversations.computeIfAbsent(selectedContact, k -> new ArrayList<>());
                        msgs.add(new Message("📧 Email sent to "+to+": \""+sub+"\"", true,
                            LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))));
                        renderMessages();
                        NotificationManager.add("Email Sent","Email to "+selectedContact+" delivered","info");
                        UI.showToast((JFrame)SwingUtilities.getWindowAncestor(EmailChatPanel.this),
                            ok ? "Email sent to "+to : "Saved (configure SMTP to deliver)", !ok);
                    } catch(Exception ex){ send.setText("✉ Send Email"); send.setEnabled(true); }
                }
            };
            worker.execute();
        });
        acts.add(cancel); acts.add(send);
        root.add(h,BorderLayout.NORTH); root.add(form,BorderLayout.CENTER); root.add(acts,BorderLayout.SOUTH);
        d.setContentPane(root); d.setVisible(true);
    }

    private void addFormLabel(JPanel p, String text) {
        JLabel l = new JLabel(text); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUT);
        p.add(l);
    }

    private String nvl(String s, String def) { return (s==null||s.isEmpty()) ? def : s; }
}