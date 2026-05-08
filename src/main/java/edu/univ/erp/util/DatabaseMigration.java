package edu.univ.erp.util;

import edu.univ.erp.data.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseMigration {
    
    public static void main(String[] args) {
        System.out.println("🔄 Starting database migration...");
        createEnrollmentsTable();
        migrateExistingData();
        verifyMigration();
    }
    
    public static void createEnrollmentsTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS enrollments (" +
                "    id INT AUTO_INCREMENT PRIMARY KEY," +
                "    student_roll_no VARCHAR(50) NOT NULL," +
                "    course_code VARCHAR(10) NOT NULL," +
                "    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    UNIQUE KEY unique_enrollment (student_roll_no, course_code)," +
                "    FOREIGN KEY (student_roll_no) REFERENCES students(roll_no) ON DELETE CASCADE," +
                "    FOREIGN KEY (course_code) REFERENCES courses(code) ON DELETE CASCADE," +
                "    INDEX idx_student (student_roll_no)," +
                "    INDEX idx_course (course_code)" +
                ")";
        
        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("✅ Enrollments table created successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error creating enrollments table:");
            e.printStackTrace();
        }
    }
    
    public static void migrateExistingData() {
        String migrateSQL = "INSERT INTO enrollments (student_roll_no, course_code) " +
                "SELECT roll_no, course_code FROM students " +
                "WHERE course_code IS NOT NULL AND course_code != ''";
        
        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement()) {
            int rowsInserted = stmt.executeUpdate(migrateSQL);
            System.out.println("✅ Migrated " + rowsInserted + " existing enrollments!");
        } catch (Exception e) {
            System.err.println("❌ Error during data migration:");
            e.printStackTrace();
        }
    }
    
    public static void verifyMigration() {
        String verifySQL = "SELECT COUNT(*) as total_enrollments FROM enrollments";
        
        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement()) {
            var resultSet = stmt.executeQuery(verifySQL);
            if (resultSet.next()) {
                int count = resultSet.getInt("total_enrollments");
                System.out.println("✅ Verification: " + count + " total enrollments in database");
            }
        } catch (Exception e) {
            System.err.println("❌ Error verifying migration:");
            e.printStackTrace();
        }
    }
}
