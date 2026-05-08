package edu.univ.erp.data;

import java.sql.*;
import java.time.LocalTime;
import java.util.*;

public class TimetableDAO {

    // Ensure timetable table exists and generate simple random slots
    public void generateTimetablesIfMissing() {
        String create = """
            CREATE TABLE IF NOT EXISTS timetable (
                id INT AUTO_INCREMENT PRIMARY KEY,
                course_code VARCHAR(50) NOT NULL,
                day_of_week VARCHAR(10) NOT NULL,
                start_time TIME NOT NULL,
                duration_minutes INT NOT NULL
            ) ENGINE=InnoDB;
        """;

        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(create);

            // For each course ensure at least 3 entries exist
            CourseDAO courseDAO = new CourseDAO();
            List<String[]> courses = courseDAO.getAllCourses();

            String countSql = "SELECT COUNT(*) AS cnt FROM timetable WHERE course_code = ?";
            String insertSql = "INSERT INTO timetable (course_code, day_of_week, start_time, duration_minutes) VALUES (?, ?, ?, ?)";

            try (PreparedStatement countStmt = conn.prepareStatement(countSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                Random rnd = new Random();
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

                for (String[] course : courses) {
                    String courseCode = course[1]; // name, code, credits...

                    countStmt.setString(1, courseCode);
                    try (ResultSet rs = countStmt.executeQuery()) {
                        int existing = 0;
                        if (rs.next()) existing = rs.getInt("cnt");

                        if (existing >= 3) continue; // already have enough

                        // pick distinct days
                        List<Integer> indices = new ArrayList<>();
                        for (int i = 0; i < days.length; i++) indices.add(i);
                        Collections.shuffle(indices, rnd);

                        for (int i = 0; i < 3; i++) {
                            String day = days[indices.get(i)];
                            // choose hour between 8 and 16 inclusive (so 1 hour fits)
                            int hour = 8 + rnd.nextInt(9); // 8..16
                            LocalTime t = LocalTime.of(hour, 0);
                            Time sqlTime = Time.valueOf(t);

                            insertStmt.setString(1, courseCode);
                            insertStmt.setString(2, day);
                            insertStmt.setTime(3, sqlTime);
                            insertStmt.setInt(4, 60);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }

            System.out.println("[TimetableDAO] Timetables ensured for courses (3 slots each where missing)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Return timetable rows for a student (only courses they are enrolled in)
    public List<String[]> getTimetableForStudent(String studentRollNo) {
        List<String[]> rows = new ArrayList<>();

        String query = """
            SELECT t.course_code, c.name AS course_name, t.day_of_week, t.start_time, t.duration_minutes
            FROM timetable t
            JOIN enrollments e ON e.course_code = t.course_code
            JOIN courses c ON c.code = t.course_code
            WHERE e.student_roll_no = ?
            ORDER BY FIELD(t.day_of_week, 'Mon','Tue','Wed','Thu','Fri','Sat'), t.start_time
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentRollNo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String courseCode = rs.getString("course_code");
                    String courseName = rs.getString("course_name");
                    String day = rs.getString("day_of_week");
                    Time start = rs.getTime("start_time");
                    int dur = rs.getInt("duration_minutes");

                    rows.add(new String[]{courseName, courseCode, day, start.toString(), String.valueOf(dur)});
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }
}
