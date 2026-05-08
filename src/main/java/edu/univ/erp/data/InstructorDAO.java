package edu.univ.erp.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class InstructorDAO {

    // Fetch all instructors
    public List<String[]> getAllInstructors() {
        List<String[]> instructors = new ArrayList<>();

        String query = "SELECT * FROM instructors";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String[] row = new String[3];
                row[0] = rs.getString("name");
                row[1] = rs.getString("department");
                row[2] = rs.getString("email");
                instructors.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return instructors;
    }

    // Add a new instructor
    public boolean addInstructor(String name, String department, String email) {
        String insertInstructor = "INSERT INTO instructors (name, department, email) VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBConnection.getERPConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(insertInstructor);
            stmt.setString(1, name);
            stmt.setString(2, department);
            stmt.setString(3, email);

            int rows = stmt.executeUpdate();
            if (rows <= 0) {
                conn.rollback();
                return false;
            }

            // Create auth user in authdb: username = email (lowercased), password = 'inst123', role = 'Instructor'
            String username = email == null ? null : email.toLowerCase().trim();
            if (username == null || username.isEmpty()) {
                // invalid email, rollback
                conn.rollback();
                return false;
            }

            String insertUser = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

            java.sql.Connection authConn = DBConnection.getAuthConnection();
            if (authConn == null) {
                // auth DB not available; rollback to avoid orphan instructor record
                conn.rollback();
                return false;
            }

            try (java.sql.PreparedStatement userStmt = authConn.prepareStatement(insertUser)) {
                userStmt.setString(1, username);
                // store bcrypt hash instead of plaintext
                String hashed = BCrypt.hashpw("inst123", BCrypt.gensalt());
                userStmt.setString(2, hashed);
                userStmt.setString(3, "Instructor");
                userStmt.executeUpdate();

            } catch (SQLException ae) {
                // If user already exists (duplicate), treat as success; otherwise rollback instructor insert.
                String msg = ae.getMessage() == null ? "" : ae.getMessage().toLowerCase();
                if (ae instanceof java.sql.SQLIntegrityConstraintViolationException || msg.contains("duplicate") || msg.contains("unique")) {
                    // user already exists — ignore and commit instructor insert
                } else {
                    try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
                    ae.printStackTrace();
                    try { authConn.close(); } catch (SQLException ex) { /* ignore */ }
                    return false;
                }
            } finally {
                try { authConn.close(); } catch (SQLException ex) { /* ignore */ }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { /* ignore */ }
        }
    }

    // Delete an instructor by email
    public boolean deleteInstructor(String email) {
        String query = "DELETE FROM instructors WHERE email = ?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Migrate all plaintext passwords in authdb.users to bcrypt hashes
    public static void migratePasswordsToBcrypt() {
        System.out.println("[InstructorDAO] Migrating plaintext passwords to bcrypt...");
        String selectQuery = "SELECT username, password_hash FROM users WHERE password_hash IS NOT NULL AND password_hash NOT LIKE '$2%'";
        String updateQuery = "UPDATE users SET password_hash = ? WHERE username = ?";

        try (Connection authConn = DBConnection.getAuthConnection();
             PreparedStatement selectStmt = authConn.prepareStatement(selectQuery);
             ResultSet rs = selectStmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                String plaintext = rs.getString("password_hash");

                if (plaintext != null && !plaintext.isEmpty()) {
                    try (PreparedStatement updateStmt = authConn.prepareStatement(updateQuery)) {
                        String hashed = BCrypt.hashpw(plaintext, BCrypt.gensalt());
                        updateStmt.setString(1, hashed);
                        updateStmt.setString(2, username);
                        updateStmt.executeUpdate();
                        count++;
                    } catch (SQLException ue) {
                        System.err.println("[InstructorDAO] Error hashing password for " + username + ": " + ue.getMessage());
                    }
                }
            }

            System.out.println("[InstructorDAO] Migrated " + count + " passwords to bcrypt.");

        } catch (SQLException e) {
            System.err.println("[InstructorDAO] Error during password migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
