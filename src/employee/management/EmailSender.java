package employee.management;

import java.util.Properties;


/**
 * Sends real emails via SMTP using Jakarta Mail (javax.mail).
 * Requires jakarta.mail.jar in the lib/ folder at runtime.
 * Download from: https://mvnrepository.com/artifact/com.sun.mail/jakarta.mail/2.0.1
 *
 * For Gmail: Enable 2-Step Verification, create an App Password,
 * use that 16-char app password here (NOT your Gmail login password).
 */
public class EmailSender {
    public static String SMTP_HOST = "smtp.gmail.com";
    public static String SMTP_PORT = "587";
    public static String SMTP_USER = "hemachandran2005v@gmail.com";
    public static String SMTP_PASS = "";   // Set this in Settings → SMTP Settings
    public static String FROM_NAME = "EMS Pro";

    private static final String PROPS_FILE = "smtp.properties";

    static { loadConfig(); }

    public static void loadConfig() {
        try {
            java.io.File f = new java.io.File(PROPS_FILE);
            if (f.exists()) {
                Properties p = new Properties();
                p.load(new java.io.FileInputStream(f));
                SMTP_HOST = p.getProperty("host", SMTP_HOST);
                SMTP_PORT = p.getProperty("port", SMTP_PORT);
                SMTP_USER = p.getProperty("user", SMTP_USER);
                SMTP_PASS = p.getProperty("pass", SMTP_PASS);
                FROM_NAME = p.getProperty("fromName", FROM_NAME);
            }
        } catch (Exception ignored) {}
    }

    public static void saveConfig() {
        try {
            Properties p = new Properties();
            p.setProperty("host",     SMTP_HOST);
            p.setProperty("port",     SMTP_PORT);
            p.setProperty("user",     SMTP_USER);
            p.setProperty("pass",     SMTP_PASS);
            p.setProperty("fromName", FROM_NAME);
            p.store(new java.io.FileOutputStream(PROPS_FILE), "EMS SMTP Config");
        } catch (Exception ignored) {}
    }

    /**
     * Sends email using reflection — works without jakarta.mail at compile time.
     * Returns true if sent, false if jar missing or credentials not set.
     */
    public static boolean send(String toEmail, String subject, String bodyText) {
        if (SMTP_USER.isEmpty() || SMTP_PASS.isEmpty()) {
            System.out.println("[EmailSender] SMTP not configured. Set user/pass in Settings.");
            return false;
        }
        try {
            // Use reflection to call jakarta.mail if available
            Class.forName("jakarta.mail.Session");
            return sendViaJakarta(toEmail, subject, bodyText);
        } catch (ClassNotFoundException e) {
            System.out.println("[EmailSender] jakarta.mail.jar not in lib/ — email not sent.");
            System.out.println("[EmailSender] Download from: https://mvnrepository.com/artifact/com.sun.mail/jakarta.mail/2.0.1");
            return false;
        } catch (Exception e) {
            System.err.println("[EmailSender] Send failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean sendViaJakarta(String toEmail, String subject, String bodyText) throws Exception {
        String html = "<div style='font-family:Segoe UI,sans-serif;'>"
            + "<div style='background:#1e3a5f;padding:16px;border-radius:8px 8px 0 0;'>"
            + "<h2 style='color:#7cb9f5;margin:0;'>EMS Pro</h2></div>"
            + "<div style='background:#161b27;padding:20px;border-radius:0 0 8px 8px;'>"
            + "<p style='color:#e2e8f0;font-size:14px;'>" + bodyText.replace("\n","<br>") + "</p>"
            + "<hr style='border-color:#2a2f3d;'/>"
            + "<p style='color:#64748b;font-size:11px;'>Sent via EMS Pro</p></div></div>";

        Properties props = new Properties();
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.host",SMTP_HOST);
        props.put("mail.smtp.port",SMTP_PORT);
        props.put("mail.smtp.ssl.trust",SMTP_HOST);

        Class<?> sessionCls   = Class.forName("jakarta.mail.Session");
        Class<?> authCls      = Class.forName("jakarta.mail.Authenticator");
        Class<?> pwAuthCls    = Class.forName("jakarta.mail.PasswordAuthentication");
        Class<?> msgCls       = Class.forName("jakarta.mail.internet.MimeMessage");
        Class<?> transportCls = Class.forName("jakarta.mail.Transport");
        Class<?> multipartCls = Class.forName("jakarta.mail.internet.MimeMultipart");
        Class<?> bodyPartCls  = Class.forName("jakarta.mail.internet.MimeBodyPart");
        Class<?> iaCls        = Class.forName("jakarta.mail.internet.InternetAddress");
        Class<?> recipCls     = Class.forName("jakarta.mail.Message$RecipientType");

        Object auth = java.lang.reflect.Proxy.newProxyInstance(
            sessionCls.getClassLoader(), new Class[]{authCls},
            (proxy, method, args2) -> {
                if("getPasswordAuthentication".equals(method.getName()))
                    return pwAuthCls.getConstructor(String.class,String.class).newInstance(SMTP_USER,SMTP_PASS);
                return null;
            });

        Object session = sessionCls.getMethod("getInstance",Properties.class,authCls).invoke(null,props,auth);
        Object msg = msgCls.getConstructor(sessionCls).newInstance(session);
        Object from = iaCls.getConstructor(String.class,String.class).newInstance(SMTP_USER,FROM_NAME);
        msgCls.getMethod("setFrom",Class.forName("jakarta.mail.Address")).invoke(msg,from);
        Object toAddr = iaCls.getMethod("parse",String.class).invoke(null,toEmail);
        Object toType = java.util.Arrays.stream(recipCls.getEnumConstants())
            .filter(e->e.toString().contains("TO")).findFirst().orElse(null);
        msgCls.getMethod("setRecipients",recipCls,Class.forName("[Ljakarta.mail.Address;")).invoke(msg,toType,toAddr);
        msgCls.getMethod("setSubject",String.class).invoke(msg,subject);

        Object mp = multipartCls.getConstructor(String.class).newInstance("alternative");
        Object tp = bodyPartCls.getConstructor().newInstance();
        bodyPartCls.getMethod("setText",String.class,String.class).invoke(tp,bodyText,"utf-8");
        Object hp = bodyPartCls.getConstructor().newInstance();
        bodyPartCls.getMethod("setContent",Object.class,String.class).invoke(hp,html,"text/html; charset=utf-8");
        Class<?> bpCls = Class.forName("jakarta.mail.BodyPart");
        multipartCls.getMethod("addBodyPart",bpCls).invoke(mp,tp);
        multipartCls.getMethod("addBodyPart",bpCls).invoke(mp,hp);
        msgCls.getMethod("setContent",Class.forName("jakarta.mail.Multipart")).invoke(msg,mp);
        transportCls.getMethod("send",Class.forName("jakarta.mail.Message")).invoke(null,msg);
        System.out.println("[EmailSender] Sent to "+toEmail);
        return true;
    }
}