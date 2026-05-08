package edu.univ.erp.util;

import edu.univ.erp.data.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InspectDB {
    public static void main(String[] args) {
        System.out.println("Inspecting 'grades' table columns in erpdb...");
        String sql = "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'erpdb' AND TABLE_NAME = 'grades' ORDER BY ORDINAL_POSITION";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("%d. %s (%s)%n", count, rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"));
            }
            if (count == 0) System.out.println("No columns found - table may not exist.");
        } catch (Exception e) {
            System.err.println("Error inspecting DB:");
            e.printStackTrace();
        }
    }
}
