package employee.management;

import java.util.*;

public class NotificationManager {
    public static class Notification {
        public final String title, body, type, time;
        public boolean read;
        public Notification(String title, String body, String type) {
            this.title = title; this.body = body; this.type = type;
            this.time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a"));
            this.read = false;
        }
    }

    private static final List<Notification> list = new ArrayList<>();
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void add(String title, String body, String type) {
        list.add(0, new Notification(title, body, type));
        listeners.forEach(Runnable::run);
    }

    public static List<Notification> getAll() { return list; }

    public static long unreadCount() { return list.stream().filter(n -> !n.read).count(); }

    public static void markAllRead() { list.forEach(n -> n.read = true); listeners.forEach(Runnable::run); }

    public static void addListener(Runnable r) { listeners.add(r); }

    static {
        add("Welcome", "EMS Pro started successfully", "info");
    }
}
