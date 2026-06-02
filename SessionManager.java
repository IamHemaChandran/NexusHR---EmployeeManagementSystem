package employee.management;

/** Holds the currently logged-in user's info across the session. */
public class SessionManager {
    public static String username = "";
    public static String role     = "admin";   // "admin" | "hr" | "employee"
    public static String empId    = "";         // set if role=employee

    public static boolean isAdmin()    { return "admin".equals(role); }
    public static boolean isHR()       { return "hr".equals(role) || isAdmin(); }
    public static boolean isEmployee() { return "employee".equals(role); }
}
