package edu.univ.erp.tools;

import edu.univ.erp.data.DBConnection;
import java.sql.*;

public class VerifyPasswordStorage {
    public static void main(String[] args) {
        System.out.println("[VerifyPasswordStorage] Checking password storage in authdb.users...\n");

        String query = "SELECT username, password_hash FROM users LIMIT 10";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println(String.format("%-40s | %-20s | %s", "Username", "Is Bcrypt?", "Hash Preview"));
            System.out.println("-".repeat(85));

            int count = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                String hash = rs.getString("password_hash");
                
                if (hash != null && !hash.isEmpty()) {
                    boolean isBcrypt = hash.startsWith("$2a$") || hash.startsWith("$2b$");
                    String preview = isBcrypt ? hash.substring(0, Math.min(15, hash.length())) + "..." : "(plaintext)";
                    System.out.println(String.format("%-40s | %-20s | %s", username, isBcrypt ? "YES ✓" : "NO ✗", preview));
                    count++;
                } else {
                    System.out.println(String.format("%-40s | %-20s | (empty or null)", username, "N/A"));
                }
            }

            System.out.println("-".repeat(85));
            System.out.println("[VerifyPasswordStorage] Total users checked: " + count);
            System.out.println("[VerifyPasswordStorage] All bcrypt hashes are one-way; plaintext passwords cannot be recovered.");

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to query authdb.users");
            e.printStackTrace();
        }
    }
}
