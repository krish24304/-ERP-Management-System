package edu.univ.erp.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    // 🔹 Fetch all courses (Admin)
    public List<String[]> getAllCourses() {
        List<String[]> courses = new ArrayList<>();

        String query = """
            SELECT name, code, credits, instructor_email, capacity,
                   quiz_weight, assignment_weight, midsem_weight, endsem_weight,
                   drop_deadline
            FROM courses
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                courses.add(new String[]{
                    rs.getString("name"),
                    rs.getString("code"),
                    String.valueOf(rs.getInt("credits")),
                    rs.getString("instructor_email"),
                    String.valueOf(rs.getInt("capacity")),
                    String.valueOf(rs.getInt("quiz_weight")),
                    String.valueOf(rs.getInt("assignment_weight")),
                    String.valueOf(rs.getInt("midsem_weight")),
                    String.valueOf(rs.getInt("endsem_weight")),
                    rs.getString("drop_deadline")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // 🔹 Add Course
    public boolean addCourse(String name, String code, int credits, String instructorEmail,
                             int quizWeight, int assignmentWeight, int midsemWeight,
                             int endsemWeight, int capacity, String dropDeadline) {

        // ensure courses table has drop_deadline column
        ensureDropDeadlineColumnExists();

        String query = """
            INSERT INTO courses
            (name, code, credits, instructor_email, quiz_weight,
             assignment_weight, midsem_weight, endsem_weight, capacity, drop_deadline)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);
            stmt.setString(2, code);
            stmt.setInt(3, credits);
            stmt.setString(4, instructorEmail);
            stmt.setInt(5, quizWeight);
            stmt.setInt(6, assignmentWeight);
            stmt.setInt(7, midsemWeight);
            stmt.setInt(8, endsemWeight);
            stmt.setInt(9, capacity);
            if (dropDeadline == null || dropDeadline.isBlank()) {
                stmt.setNull(10, Types.TIMESTAMP);
            } else {
                stmt.setString(10, dropDeadline);
            }

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Ensure courses table has drop_deadline column (safe check via information_schema)
    public void ensureDropDeadlineColumnExists() {
        String check = "SELECT COUNT(*) AS cnt FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'drop_deadline'";
        String alter = "ALTER TABLE courses ADD COLUMN drop_deadline DATETIME NULL";
        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(check)) {
                boolean exists = false;
                if (rs.next()) exists = rs.getInt("cnt") > 0;
                if (!exists) {
                    stmt.execute(alter);
                    System.out.println("[CourseDAO] Added drop_deadline column to courses table.");
                }
            }

        } catch (SQLException e) {
            // log and continue
            System.err.println("[CourseDAO] Could not ensure drop_deadline column: " + e.getMessage());
        }
    }

    // Set default drop deadline for existing courses that have NULL or empty deadlines
    public void setDefaultDeadlinesIfMissing() {
        String defaultDeadline = "2025-12-05 12:00:00"; // fixed default per request
        String updateNull = "UPDATE courses SET drop_deadline = ? WHERE drop_deadline IS NULL";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(updateNull)) {

            stmt.setString(1, defaultDeadline);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("[CourseDAO] Set default drop_deadline for " + updated + " courses (NULL entries).");
            }

            // Try to fix any empty-string values if they exist; run separately and tolerate errors
            try (PreparedStatement stmt2 = conn.prepareStatement("UPDATE courses SET drop_deadline = ? WHERE drop_deadline = ''")) {
                stmt2.setString(1, defaultDeadline);
                try {
                    int updated2 = stmt2.executeUpdate();
                    if (updated2 > 0) System.out.println("[CourseDAO] Set default drop_deadline for " + updated2 + " courses (empty-string entries).");
                } catch (SQLException ex) {
                    // some MySQL strict modes may reject comparing DATETIME to empty string; ignore
                }
            }

        } catch (SQLException e) {
            System.err.println("[CourseDAO] Error setting default deadlines: " + e.getMessage());
        }
    }

    // Allow a student to drop a course before the deadline and remove all traces
    public boolean dropStudentFromCourse(String studentRollNo, String courseCode) {
        // check deadline
        String getDeadline = "SELECT drop_deadline FROM courses WHERE code = ?";
        String deleteGrades = "DELETE FROM grades WHERE student_roll_no = ? AND course_code = ?";
        String deleteEnrollment = "DELETE FROM enrollments WHERE student_roll_no = ? AND course_code = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement dlStmt = conn.prepareStatement(getDeadline);
             PreparedStatement delGradesStmt = conn.prepareStatement(deleteGrades);
             PreparedStatement delEnrollStmt = conn.prepareStatement(deleteEnrollment)) {

            dlStmt.setString(1, courseCode);
            try (ResultSet rs = dlStmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("drop_deadline");
                    if (ts != null) {
                        Timestamp now = new Timestamp(System.currentTimeMillis());
                        if (now.after(ts)) {
                            // past deadline
                            return false;
                        }
                    }
                } else {
                    // course not found
                    return false;
                }
            }

            conn.setAutoCommit(false);

            delGradesStmt.setString(1, studentRollNo);
            delGradesStmt.setString(2, courseCode);
            delGradesStmt.executeUpdate();

            delEnrollStmt.setString(1, studentRollNo);
            delEnrollStmt.setString(2, courseCode);
            int removed = delEnrollStmt.executeUpdate();

            conn.commit();
            return removed > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Delete Course
    public boolean deleteCourse(String code) {
        String query = "DELETE FROM courses WHERE code = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, code);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Fetch courses assigned to a specific instructor
    public List<String> getCoursesByInstructor(String instructorEmail) {
        List<String> courses = new ArrayList<>();

        String query = "SELECT name FROM courses WHERE instructor_email = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, instructorEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // 🔹 Update course capacity
    public boolean updateCourseCapacity(String courseCode, int newCapacity) {
        String query = "UPDATE courses SET capacity = ? WHERE code = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, newCapacity);
            stmt.setString(2, courseCode);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Get course code using course name
    public String getCourseCodeByName(String courseName) {
        String query = "SELECT code FROM courses WHERE name = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("code");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 Get weights (quiz, assignment, midsem, endsem) for a course code
    public int[] getCourseWeights(String courseCode) {
        String query = "SELECT quiz_weight, assignment_weight, midsem_weight, endsem_weight FROM courses WHERE code = ?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int q = rs.getInt("quiz_weight");
                    int a = rs.getInt("assignment_weight");
                    int m = rs.getInt("midsem_weight");
                    int e = rs.getInt("endsem_weight");
                    return new int[]{q, a, m, e};
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // default: equal weights if not found
        return new int[]{25, 25, 25, 25};
    }

    // 🔹 Display list of courses with available seats
    public List<String[]> getAvailableCourses() {
        List<String[]> courses = new ArrayList<>();

        String query = """
            SELECT c.name, c.code, c.instructor_email,
                   (c.capacity - COUNT(e.id)) AS seats_left
            FROM courses c
            LEFT JOIN enrollments e ON c.code = e.course_code
            GROUP BY c.name, c.code, c.instructor_email, c.capacity
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                courses.add(new String[]{
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getString("instructor_email"),
                        String.valueOf(rs.getInt("seats_left"))
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // 🔹 Register student into a course (supports multiple enrollments via enrollments table)
    public boolean registerStudentToCourse(String studentRollNo, String courseCode) {
        // Trim inputs to avoid whitespace issues
        if (studentRollNo != null) studentRollNo = studentRollNo.trim();
        if (courseCode != null) courseCode = courseCode.trim();

        String checkStudent = "SELECT roll_no FROM students WHERE roll_no = ?";
        String checkAlreadyEnrolled = "SELECT id FROM enrollments WHERE student_roll_no = ? AND course_code = ?";
        String getCourseCapacity = "SELECT capacity FROM courses WHERE code = ?";
        String countEnrolled = "SELECT COUNT(*) AS enrolled FROM enrollments WHERE course_code = ?";
        String insertEnrollment = "INSERT INTO enrollments (student_roll_no, course_code) VALUES (?, ?)";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkStudent);
             PreparedStatement checkEnrollStmt = conn.prepareStatement(checkAlreadyEnrolled);
             PreparedStatement capStmt = conn.prepareStatement(getCourseCapacity);
             PreparedStatement countStmt = conn.prepareStatement(countEnrolled);
             PreparedStatement insertStmt = conn.prepareStatement(insertEnrollment)) {

            conn.setAutoCommit(false);

            // 👀 Check if student exists
            checkStmt.setString(1, studentRollNo);
            try (ResultSet rs1 = checkStmt.executeQuery()) {
                if (!rs1.next()) {
                    conn.rollback();
                    System.err.println("DEBUG: Student not found: " + studentRollNo);
                    return false;
                }
            }

            // 🔄 Check if already enrolled in this course
            checkEnrollStmt.setString(1, studentRollNo);
            checkEnrollStmt.setString(2, courseCode);
            try (ResultSet rsEnroll = checkEnrollStmt.executeQuery()) {
                if (rsEnroll.next()) {
                    conn.rollback();
                    System.err.println("DEBUG: Student already enrolled in: " + courseCode);
                    return false;
                }
            }

            // 🪑 Check course exists and has capacity
            capStmt.setString(1, courseCode);
            int capacity = 0;
            try (ResultSet rs2 = capStmt.executeQuery()) {
                if (!rs2.next()) {
                    conn.rollback();
                    System.err.println("DEBUG: Course not found: " + courseCode);
                    return false;
                }
                capacity = rs2.getInt("capacity");
            }

            // Count enrolled
            countStmt.setString(1, courseCode);
            int enrolled = 0;
            try (ResultSet rs3 = countStmt.executeQuery()) {
                if (rs3.next()) {
                    enrolled = rs3.getInt("enrolled");
                }
            }

            int seatsLeft = capacity - enrolled;
            if (seatsLeft <= 0) {
                conn.rollback();
                System.err.println("DEBUG: No seats available. Capacity: " + capacity + ", Enrolled: " + enrolled);
                return false;
            }

            // ✍ Register student into course
            insertStmt.setString(1, studentRollNo);
            insertStmt.setString(2, courseCode);
            int inserted = insertStmt.executeUpdate();
            if (inserted > 0) {
                conn.commit();
                System.out.println("DEBUG: Enrollment successful for " + studentRollNo + " in " + courseCode);
                return true;
            } else {
                conn.rollback();
                System.err.println("DEBUG: Enrollment insert failed");
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Get courses enrolled by a student
    public List<String[]> getEnrolledCourses(String studentRollNo) {
        List<String[]> courses = new ArrayList<>();

        String query = """
            SELECT c.name, c.code, c.instructor_email, c.credits
            FROM courses c
            JOIN enrollments e ON c.code = e.course_code
            WHERE e.student_roll_no = ?
            ORDER BY c.name
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentRollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(new String[]{
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getString("instructor_email"),
                        String.valueOf(rs.getInt("credits"))
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }
}
