package edu.univ.erp.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    // 🔹 Fetch all students (Admin View) — Only Name, Roll, Enrollment Status
    public List<String[]> getAllStudents() {
        List<String[]> students = new ArrayList<>();

        String query = """
            SELECT s.name, s.roll_no,
                   COALESCE(c.name, 'Not Enrolled') AS course_name
            FROM students s
            LEFT JOIN courses c ON s.course_code = c.code
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                students.add(new String[]{
                        rs.getString("name"),
                        rs.getString("roll_no"),
                        rs.getString("course_name")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    // 🔹 Add new student AND auto-create login credentials
    public boolean addStudent(String name, String rollNo) {
        String insertStudent = """
            INSERT INTO students (name, roll_no, course_code)
            VALUES (?, ?, NULL)
        """;

        String insertUser = """
            INSERT INTO users (username, password_hash, role)
            VALUES (?, ?, 'Student')
            ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash)
        """;

        try (Connection erpConn = DBConnection.getERPConnection();
             PreparedStatement stmtStudent = erpConn.prepareStatement(insertStudent)) {

            // Insert into students table
            stmtStudent.setString(1, name);
            stmtStudent.setString(2, rollNo);
            boolean studentAdded = stmtStudent.executeUpdate() > 0;

            // 🟢 Auto-create login (username = name lowercase no spaces, password = roll number)
            if (studentAdded) {
                String username = name.toLowerCase().replaceAll("\\s+", "");

                try (Connection authConn = DBConnection.getAuthConnection();
                     PreparedStatement stmtUser = authConn.prepareStatement(insertUser)) {

                    stmtUser.setString(1, username);
                    stmtUser.setString(2, rollNo);
                    stmtUser.executeUpdate();
                }
            }
            return studentAdded;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Delete Student by Roll No (also remove login credentials)
    public boolean deleteStudent(String rollNo) {
        String deleteStudentQuery = "DELETE FROM students WHERE roll_no = ?";
        String deleteUserQuery = "DELETE FROM users WHERE password_hash = ? AND role = 'Student'";

        try (Connection erpConn = DBConnection.getERPConnection();
             PreparedStatement stmtStudent = erpConn.prepareStatement(deleteStudentQuery);
             Connection authConn = DBConnection.getAuthConnection();
             PreparedStatement stmtUser = authConn.prepareStatement(deleteUserQuery)) {

            stmtStudent.setString(1, rollNo);
            stmtUser.setString(1, rollNo);

            stmtUser.executeUpdate(); // delete login credentials
            return stmtStudent.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Fetch students for Instructor Dashboard (Course-wise)
    public List<String[]> getStudentsByInstructor(String instructorEmail) {
        List<String[]> students = new ArrayList<>();

        String query = """
            SELECT s.name, s.roll_no, c.name AS course_name
            FROM students s
            JOIN courses c ON s.course_code = c.code
            WHERE c.instructor_email = ?
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, instructorEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                students.add(new String[]{
                        rs.getString("name"),
                        rs.getString("roll_no"),
                        rs.getString("course_name")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    // 🔹 Fetch students by Course (Admin — View enrolled list)
    public List<String[]> getStudentsByCourse(String courseCode) {
        List<String[]> students = new ArrayList<>();

        String query = """
            SELECT s.name, s.roll_no
            FROM students s
            WHERE s.course_code = ?
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseCode);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                students.add(new String[]{
                        rs.getString("name"),
                        rs.getString("roll_no")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    // 🔹 Resolve roll number from username (username = lowercased name without spaces)
    public String getRollNoByUsername(String normalizedUsername) {
        if (normalizedUsername == null || normalizedUsername.isBlank()) return null;
        String query = """
            SELECT roll_no
            FROM students
            WHERE LOWER(REPLACE(name, ' ', '')) = ?
            LIMIT 1
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, normalizedUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("roll_no");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
