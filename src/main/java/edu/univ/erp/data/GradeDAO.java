package edu.univ.erp.data;

import java.sql.*;

public class GradeDAO {

    // Get grades/results for a specific student - returns ALL enrolled courses with their grades
    public java.util.List<String[]> getResultsByStudent(String studentRollNo) {
        java.util.List<String[]> results = new java.util.ArrayList<>();

        // LEFT JOIN so we get all enrolled courses even if grades don't exist yet
        String query = """
            SELECT c.name, COALESCE(g.total_marks, 0) as total_marks, 
                   COALESCE(g.final_grade, 'Not Graded') as final_grade
            FROM enrollments e
            JOIN courses c ON e.course_code = c.code
            LEFT JOIN grades g ON g.student_roll_no = e.student_roll_no AND g.course_code = e.course_code
            WHERE e.student_roll_no = ?
            ORDER BY c.name
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentRollNo);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String totalMarks = String.format("%.2f", rs.getDouble("total_marks"));
                String finalGrade = rs.getString("final_grade");

                results.add(new String[]{
                        rs.getString("name"),
                        totalMarks,
                        finalGrade
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // Get grades for a course (for instructor)
    public java.util.List<String[]> getGradesForCourse(String instructorEmail, String courseCode) {
        java.util.List<String[]> grades = new java.util.ArrayList<>();

        // Return: Student Name, Roll No, Quiz, Assignment, Midsem, Endsem, Total, Final Grade
        String query = """
            SELECT s.name AS student_name, s.roll_no, g.quiz_marks, g.assignment_marks, g.midsem_marks, g.endsem_marks, g.total_marks, g.final_grade
            FROM grades g
            JOIN students s ON g.student_roll_no = s.roll_no
            WHERE g.course_code = ?
            ORDER BY s.roll_no
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseCode);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String quiz = rs.getObject("quiz_marks") == null ? "0" : String.valueOf(rs.getInt("quiz_marks"));
                String assignment = rs.getObject("assignment_marks") == null ? "0" : String.valueOf(rs.getInt("assignment_marks"));
                String midsem = rs.getObject("midsem_marks") == null ? "0" : String.valueOf(rs.getInt("midsem_marks"));
                String endsem = rs.getObject("endsem_marks") == null ? "0" : String.valueOf(rs.getInt("endsem_marks"));
                String total = rs.getObject("total_marks") == null ? "0" : String.format("%.2f", rs.getDouble("total_marks"));
                String finalGrade = rs.getString("final_grade");
                if (finalGrade == null || finalGrade.trim().isEmpty()) finalGrade = "Not Graded";

                grades.add(new String[]{
                    rs.getString("student_name"),
                    rs.getString("roll_no"),
                    quiz,
                    assignment,
                    midsem,
                    endsem,
                    total,
                    finalGrade
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades;
    }

    // Generate course statistics
    public String generateCourseStats(String courseCode) {
                String aggQuery = """
                        SELECT
                            COUNT(*) AS total_students,
                            AVG(total_marks) AS avg_grade,
                            MAX(total_marks) AS highest_grade,
                            MIN(total_marks) AS lowest_grade
                        FROM grades
                        WHERE course_code = ?
                """;

        String gradeDistQuery = "SELECT final_grade, COUNT(*) AS cnt FROM grades WHERE course_code = ? GROUP BY final_grade";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement aggStmt = conn.prepareStatement(aggQuery);
             PreparedStatement distStmt = conn.prepareStatement(gradeDistQuery)) {

            aggStmt.setString(1, courseCode);
            ResultSet aggRs = aggStmt.executeQuery();

            StringBuilder out = new StringBuilder();
            if (aggRs.next()) {
                 out.append("🔍 Course Statistics:\n\n");
                 out.append("📘 Total Students: ").append(aggRs.getInt("total_students")).append("\n");
                 out.append("📊 Average Total Marks: ").append(String.format("%.2f", aggRs.getDouble("avg_grade"))).append("\n");
                 out.append("🏆 Highest Total Marks: ").append(String.format("%.2f", aggRs.getDouble("highest_grade"))).append("\n");
                 out.append("📉 Lowest Total Marks: ").append(String.format("%.2f", aggRs.getDouble("lowest_grade"))).append("\n\n");
            } else {
                return "No data found for this course.";
            }

            // grade distribution
            distStmt.setString(1, courseCode);
            ResultSet distRs = distStmt.executeQuery();

            out.append("📑 Grade Distribution:\n");
            java.util.Map<String, Integer> dist = new java.util.HashMap<>();
            while (distRs.next()) {
                String g = distRs.getString("final_grade");
                if (g == null) g = "UNASSIGNED";
                dist.put(g, distRs.getInt("cnt"));
            }

            // order we want to show
            String[] order = {"A+","A","B","C","D","F","UNASSIGNED"};
            for (String k : order) {
                if (dist.containsKey(k)) {
                    out.append("  ").append(k).append(": ").append(dist.get(k)).append("\n");
                }
            }

            return out.toString();

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error while fetching statistics.";
        }
    }

    public java.util.Map<String, Integer> getGradeDistribution(String courseCode) {
        String query = "SELECT final_grade, COUNT(*) AS cnt FROM grades WHERE course_code = ? GROUP BY final_grade";
        java.util.Map<String, Integer> raw = new java.util.HashMap<>();

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String grade = rs.getString("final_grade");
                    if (grade == null || grade.isBlank()) grade = "UNASSIGNED";
                    raw.put(grade, rs.getInt("cnt"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String[] order = {"A+", "A", "B", "C", "D", "F", "UNASSIGNED"};
        java.util.Map<String, Integer> ordered = new java.util.LinkedHashMap<>();
        for (String key : order) {
            int val = raw.getOrDefault(key, 0);
            if (val > 0) {
                ordered.put(key, val);
            }
        }
        for (java.util.Map.Entry<String, Integer> entry : raw.entrySet()) {
            if (!ordered.containsKey(entry.getKey())) {
                ordered.put(entry.getKey(), entry.getValue());
            }
        }
        return ordered;
    }

    // Export grades for a course to CSV under reports/ folder. Returns path on success or null on failure.
    public String exportGradesToCSV(String courseCode) {
        java.util.List<String[]> grades = getGradesForCourse(null, courseCode);
        if (grades == null || grades.isEmpty()) return null;

        java.io.File reportsDir = new java.io.File("reports");
        if (!reportsDir.exists()) reportsDir.mkdirs();

        String fileName = String.format("reports/%s_grades.csv", courseCode);
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(fileName))) {
            // header
            pw.println("Student Name,Roll No,Quiz,Assignment,Midsem,Endsem,Total,Final Grade");
            for (String[] r : grades) {
                // ensure commas are escaped/sanitized simply by quoting fields
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < r.length; i++) {
                    String cell = r[i] == null ? "" : r[i].replace("\"", "\"\"");
                    line.append('"').append(cell).append('"');
                    if (i < r.length - 1) line.append(',');
                }
                pw.println(line.toString());
            }
            pw.flush();
            return new java.io.File(fileName).getAbsolutePath();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper: compute letter grade from numeric total (0-100)
    public static String computeLetterGrade(double total) {
        // slabs requested:
        // A+ : 90 and above
        // A  : 85 - 89.99
        // B  : 70 - 84.99
        // C  : 50 - 69.99  (largest slab)
        // D  : 30 - 49.99  (large slab)
        // F  : below 30
        if (total >= 90.0) return "A+";
        if (total >= 85.0) return "A";
        if (total >= 70.0) return "B";
        if (total >= 50.0) return "C";
        if (total >= 30.0) return "D";
        return "F";
    }

    // Batch update: convert all numeric final_grades to letter grades (for existing records)
    public void convertNumericGradesToLetters() {
        // Query to get grades that need fixing:
        // 1. Records where final_grade is NULL or numeric (old data)
        // 2. Records where total_marks seems wrong (raw sum instead of weighted, >100 or sum of components > 150)
        String query = """
            SELECT g.id, g.student_roll_no, g.course_code, g.quiz_marks, g.assignment_marks, g.midsem_marks, g.endsem_marks, g.total_marks, g.final_grade
            FROM grades g
            WHERE g.final_grade IS NULL 
               OR g.final_grade REGEXP '^[0-9]+(\\.[0-9]+)?$'
               OR g.total_marks > 100
               OR (g.quiz_marks + g.assignment_marks + g.midsem_marks + g.endsem_marks) > 150
        """;
        String update = "UPDATE grades SET quiz_marks = ?, assignment_marks = ?, midsem_marks = ?, endsem_marks = ?, total_marks = ?, final_grade = ? WHERE id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement selectStmt = conn.prepareStatement(query);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = conn.prepareStatement(update)) {

            int count = 0;
            CourseDAO courseDAO = new CourseDAO();

            while (rs.next()) {
                int id = rs.getInt("id");
                String courseCode = rs.getString("course_code");
                int quiz = rs.getInt("quiz_marks");
                int assignment = rs.getInt("assignment_marks");
                int midsem = rs.getInt("midsem_marks");
                int endsem = rs.getInt("endsem_marks");
                String currentFinalGrade = rs.getString("final_grade");
                double currentTotal = rs.getDouble("total_marks");
                String rollNo = rs.getString("student_roll_no");

                // Get course weights
                int[] weights = courseDAO.getCourseWeights(courseCode);
                if (weights == null) {
                    System.out.println("[GradeDAO] Warning: No weights found for course " + courseCode + ", skipping id=" + id);
                    continue;
                }

                // Recalculate weighted total out of 100
                double weightedTotal = (quiz * weights[0] / 100.0) + 
                                       (assignment * weights[1] / 100.0) + 
                                       (midsem * weights[2] / 100.0) + 
                                       (endsem * weights[3] / 100.0);
                weightedTotal = Math.round(weightedTotal * 100.0) / 100.0;  // Round to 2 decimals

                String letter = computeLetterGrade(weightedTotal);
                
                // Update with recalculated weighted total and letter grade
                updateStmt.setInt(1, quiz);
                updateStmt.setInt(2, assignment);
                updateStmt.setInt(3, midsem);
                updateStmt.setInt(4, endsem);
                updateStmt.setDouble(5, weightedTotal);
                updateStmt.setString(6, letter);
                updateStmt.setInt(7, id);
                updateStmt.addBatch();
                count++;
                System.out.println("[GradeDAO] Fixing: id=" + id + ", course=" + courseCode + ", roll=" + rollNo + 
                                 ", marks(" + quiz + "," + assignment + "," + midsem + "," + endsem + 
                                 "), oldTotal=" + String.format("%.2f", currentTotal) + ", newTotal=" + String.format("%.2f", weightedTotal) + 
                                 ", oldGrade=" + currentFinalGrade + ", newGrade=" + letter);
            }

            if (count > 0) {
                int[] results = updateStmt.executeBatch();
                System.out.println("[GradeDAO] ===== Converted/fixed " + results.length + " grades with weighted totals and letter format =====");
            } else {
                System.out.println("[GradeDAO] No grades needed conversion");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Insert or update a grade record for a student in a course
    public boolean updateGrade(String studentRollNo, String courseCode,
                               double quiz, double assignment, double midsem, double endsem, double finalGrade) {
        String update = "UPDATE grades SET quiz_marks = ?, assignment_marks = ?, midsem_marks = ?, endsem_marks = ?, total_marks = ?, final_grade = ? WHERE student_roll_no = ? AND course_code = ?";
        String insert = "INSERT INTO grades (student_roll_no, course_code, quiz_marks, assignment_marks, midsem_marks, endsem_marks, total_marks, final_grade) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // determine letter grade
        String letter = computeLetterGrade(finalGrade);

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement up = conn.prepareStatement(update)) {
            // Bind parameters for UPDATE: 1..6 => set columns, 7 => student_roll_no, 8 => course_code
            up.setInt(1, (int) quiz);
            up.setInt(2, (int) assignment);
            up.setInt(3, (int) midsem);
            up.setInt(4, (int) endsem);
            // total_marks (position 5) and final_grade (position 6)
            up.setDouble(5, finalGrade);
            up.setString(6, letter);
            up.setString(7, studentRollNo);
            up.setString(8, courseCode);

            // debug: print what we're about to write
            System.out.println("[GradeDAO] UPDATE params: quiz=" + quiz + ", assignment=" + assignment + ", midsem=" + midsem + ", endsem=" + endsem + ", total=" + finalGrade + ", letter=" + letter + ", student=" + studentRollNo + ", course=" + courseCode);
            int updated = up.executeUpdate();
            if (updated > 0) return true;

            try (PreparedStatement ins = conn.prepareStatement(insert)) {
                ins.setString(1, studentRollNo);
                ins.setString(2, courseCode);
                ins.setInt(3, (int) quiz);
                ins.setInt(4, (int) assignment);
                ins.setInt(5, (int) midsem);
                ins.setInt(6, (int) endsem);
                // total_marks
                ins.setDouble(7, finalGrade);
                // final_grade (store letter)
                ins.setString(8, letter);
                System.out.println("[GradeDAO] INSERT params: quiz=" + quiz + ", assignment=" + assignment + ", midsem=" + midsem + ", endsem=" + endsem + ", total=" + finalGrade + ", letter=" + letter + ", student=" + studentRollNo + ", course=" + courseCode);
                int inserted = ins.executeUpdate();
                return inserted > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
