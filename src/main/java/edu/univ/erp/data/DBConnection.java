package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String USER = "root";             // your MySQL username
    private static final String PASSWORD = "root";             // add your MySQL password if set
    private static final String ERP_URL =
            "jdbc:mysql://localhost:3307/erpdb?useSSL=false&serverTimezone=UTC";
    private static final String AUTH_URL =
            "jdbc:mysql://localhost:3307/authdb?useSSL=false&serverTimezone=UTC";

    // ERP database connection (students, courses, grades)
    public static Connection getERPConnection() {
        try {
            return DriverManager.getConnection(ERP_URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to ERP DB (erpdb)");
            e.printStackTrace();
            return null;
        }
    }

    // Authentication database connection (users)
    public static Connection getAuthConnection() {
        try {
            return DriverManager.getConnection(AUTH_URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to AUTH DB (authdb)");
            e.printStackTrace();
            return null;
        }
    }
}
