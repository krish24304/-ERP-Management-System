package edu.univ.erp.data;

import java.sql.*;

public class MaintenanceDAO {

    public boolean isMaintenanceMode() {
        String query = "SELECT is_enabled FROM maintenance_mode WHERE id = 1";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getBoolean("is_enabled");
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public void setMaintenanceMode(boolean status) {
        String query = "UPDATE maintenance_mode SET is_enabled = ? WHERE id = 1";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, status);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
