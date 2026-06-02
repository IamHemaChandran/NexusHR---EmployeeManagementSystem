package employee.management;

import java.sql.*;

public class DBConnection {
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB   = "EmployeeManagement";
    private static final String USER = "root";
    private static final String PASS = "12345678.";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
                       + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, USER, PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found: " + e.getMessage());
            }
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}