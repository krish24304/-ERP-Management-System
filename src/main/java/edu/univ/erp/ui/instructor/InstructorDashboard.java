package edu.univ.erp.ui.instructor;

import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.MaintenanceDAO;
import edu.univ.erp.ui.common.ChangePasswordDialog;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class InstructorDashboard extends JFrame {

    private final String instructorEmail;

    public InstructorDashboard(String username, String instructorEmail) {
        this.instructorEmail = instructorEmail;
        MaintenanceDAO mdao = new MaintenanceDAO();
        boolean isMaintenance = mdao.isMaintenanceMode();
        setTitle("Instructor Dashboard - " + username);
        setSize(750, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        if (isMaintenance) {
            JLabel banner = new JLabel("⚠ Maintenance is ON: No changes will be recorded ⚠", SwingConstants.CENTER);
            banner.setOpaque(true);
            banner.setBackground(new Color(255, 230, 130));
            banner.setForeground(Color.RED);
            banner.setFont(new Font("Segoe UI", Font.BOLD, 16));
            add(banner, BorderLayout.NORTH);
        }

        JLabel heading = new JLabel("Welcome, Instructor " + username + "!", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(heading, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        String[] options = {
                "View Assigned Courses",
                "Manage Student Grades",
                "View Course Statistics",
                "View Student List",
                "Update Course Capacity",
                "Export Grades CSV",
                "Change Password",
                "Logout"
        };

        for (String opt : options) {
            JButton btn = new JButton(opt);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setFocusPainted(false);

            switch (opt) {
                case "Logout" -> btn.addActionListener(e -> logout());
                case "View Assigned Courses" -> btn.addActionListener(e -> showAssignedCourses());
                case "View Student List" -> btn.addActionListener(e -> showStudentList());
                case "Manage Student Grades" -> btn.addActionListener(e -> manageGrades());
                case "View Course Statistics" -> btn.addActionListener(e -> showCourseStatistics());
                case "Export Grades CSV" -> btn.addActionListener(e -> exportGradesCSV());
                case "Change Password" -> btn.addActionListener(e -> openChangePassword());
                case "Update Course Capacity" -> btn.addActionListener(e -> updateCapacity());
            }
            buttonPanel.add(btn);
        }

        add(buttonPanel, BorderLayout.CENTER);
    }

    private void logout() {
        dispose();
        JOptionPane.showMessageDialog(null, "You have been logged out.");
        System.exit(0);
    }

    private void showAssignedCourses() {
        CourseDAO dao = new CourseDAO();
        List<String> courses = dao.getCoursesByInstructor(instructorEmail);

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses assigned to you.");
            return;
        }

        JOptionPane.showMessageDialog(this,
                String.join("\n", courses),
                "Assigned Courses", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showStudentList() {
        CourseDAO courseDAO = new CourseDAO();
        List<String> courses = courseDAO.getCoursesByInstructor(instructorEmail);

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses assigned to you.");
            return;
        }

        String selectedCourse = (String) JOptionPane.showInputDialog(
                this,
                "Select a course to view enrolled students:",
                "View Students",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                courses.get(0)
        );

        if (selectedCourse == null) return;

        String courseCode = courseDAO.getCourseCodeByName(selectedCourse);

        // Fetch enrolled students from enrollments table
        java.util.List<String[]> students = new java.util.ArrayList<>();
        try (java.sql.Connection conn = edu.univ.erp.data.DBConnection.getERPConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.name, s.roll_no FROM enrollments e JOIN students s ON e.student_roll_no = s.roll_no WHERE e.course_code = ? ORDER BY s.name")) {
            ps.setString(1, courseCode);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(new String[]{rs.getString("name"), rs.getString("roll_no")});
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // fallback: if no enrollments, try old students.course_code column
        if (students.isEmpty()) {
            StudentDAO studentDAO = new StudentDAO();
            students = studentDAO.getStudentsByCourse(courseCode);
        }

        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students enrolled in this course.");
            return;
        }

        // Create table with only 2 columns (Name, Roll No) since we're showing one course
        String[] columns = {"Student Name", "Roll No"};
        String[][] tableData = students.toArray(new String[0][]);

        JTable table = new JTable(tableData, columns);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(24);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));
        
        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Students Enrolled in " + selectedCourse,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void manageGrades() {
        CourseDAO courseDAO = new CourseDAO();
        List<String> courses = courseDAO.getCoursesByInstructor(instructorEmail);

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses assigned to you.");
            return;
        }

        String selectedCourse = (String) JOptionPane.showInputDialog(
                this,
                "Select a course:",
                "Manage Grades",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                courses.get(0)
        );

        if (selectedCourse == null) return;

        String courseCode = courseDAO.getCourseCodeByName(selectedCourse);

        GradeDAO gradeDAO = new GradeDAO();
        List<String[]> gradeData = gradeDAO.getGradesForCourse(instructorEmail, courseCode);

        // Build a map of existing grades by roll_no
        java.util.Map<String, String[]> gradeMap = new java.util.HashMap<>();
        for (String[] row : gradeData) {
            if (row.length >= 2) {
                gradeMap.put(row[1], row);
            }
        }

        // Fetch enrolled students from enrollments (many-to-many). If no enrollments, fallback to students.course_code.
        java.util.List<String[]> enrolled = new java.util.ArrayList<>();
        try (java.sql.Connection conn = edu.univ.erp.data.DBConnection.getERPConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.name, s.roll_no FROM enrollments e JOIN students s ON e.student_roll_no = s.roll_no WHERE e.course_code = ?")) {
            ps.setString(1, courseCode);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    enrolled.add(new String[]{rs.getString("name"), rs.getString("roll_no")});
                }
            }
        } catch (Exception ex) {
            // ignore — will fallback
        }

        if (enrolled.isEmpty()) {
            // fallback to older single-course column
            StudentDAO studentDAO = new StudentDAO();
            enrolled = studentDAO.getStudentsByCourse(courseCode);
        }

        if (enrolled.isEmpty() && gradeMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students enrolled / no grade records found.");
            return;
        }

        // Build final gradeData list: include every enrolled student; if grade exists use it, else zeroed row
        java.util.List<String[]> finalRows = new java.util.ArrayList<>();
        // Add enrolled students first
        for (String[] s : enrolled) {
            String roll = s[1];
            String name = s[0];
            if (gradeMap.containsKey(roll)) {
                // Create a copy to avoid shared reference issues
                String[] original = gradeMap.remove(roll);
                String[] copy = new String[original.length];
                System.arraycopy(original, 0, copy, 0, original.length);
                finalRows.add(copy);
                System.out.println("[DEBUG] Added grade row for " + name + " (" + roll + "): " + java.util.Arrays.toString(copy));
            } else {
                // Create a new row with correct structure: Name, Roll, Quiz, Assignment, Midsem, Endsem, Total, Final Grade
                String[] newRow = new String[]{name, roll, "0", "0", "0", "0", "0", "0"};
                finalRows.add(newRow);
                System.out.println("[DEBUG] Added new row for " + name + " (" + roll + "): " + java.util.Arrays.toString(newRow));
            }
        }
        // Any remaining grades for students not in enrolled (edge cases) — include them too
        for (String[] rem : gradeMap.values()) {
            // Create a copy to avoid shared reference issues
            String[] copy = new String[rem.length];
            System.arraycopy(rem, 0, copy, 0, rem.length);
            finalRows.add(copy);
            System.out.println("[DEBUG] Added remaining grade row: " + java.util.Arrays.toString(copy));
        }

        // Use finalRows as the data source for the UI
        gradeData = finalRows;

        // Column names match GradeDAO.getGradesForCourse output
        String[] columns = {"Student Name", "Roll No", "Quiz", "Assignment", "Midsem", "Endsem", "Total", "Final Grade"};

        // Use DefaultTableModel to allow editing of grade columns
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // allow editing only for Quiz, Assignment, Midsem, Endsem (cols 2-5)
                return col >= 2 && col <= 5;
            }
        };

        // Add rows as Object arrays to ensure they're truly independent copies
        for (String[] row : gradeData) {
            Object[] objRow = new Object[row.length];
            for (int i = 0; i < row.length; i++) {
                objRow[i] = row[i];
            }
            model.addRow(objRow);
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(25);

        // Put table in a dialog with Save/Close buttons
        JDialog dialog = new JDialog(this, "Manage Grades - " + courseCode, true);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Changes");
        JButton closeBtn = new JButton("Close");

        saveBtn.addActionListener(e -> {
            MaintenanceDAO mdao = new MaintenanceDAO();
            if (mdao.isMaintenanceMode()) {
                JOptionPane.showMessageDialog(dialog, "Maintenance mode is ON. No changes will be recorded.");
                return;
            }
            boolean allOk = true;
            // fetch weights for this course
            CourseDAO cdao = new CourseDAO();
            int[] weights = cdao.getCourseWeights(courseCode); // {quiz, assignment, midsem, endsem}

            StringBuilder saveLog = new StringBuilder();
            System.out.println("[DEBUG] ===== SAVE START: Course=" + courseCode + ", Total Rows=" + model.getRowCount() + " =====");
            for (int r = 0; r < model.getRowCount(); r++) {
                try {
                    String studentName = String.valueOf(model.getValueAt(r, 0));
                    String rollNo = String.valueOf(model.getValueAt(r, 1));
                    String quizStr = String.valueOf(model.getValueAt(r, 2));
                    String assignmentStr = String.valueOf(model.getValueAt(r, 3));
                    String midsemStr = String.valueOf(model.getValueAt(r, 4));
                    String endsemStr = String.valueOf(model.getValueAt(r, 5));

                    System.out.println("[DEBUG] Row " + r + ": Name=" + studentName + ", Roll=" + rollNo + 
                                     ", Q=" + quizStr + ", A=" + assignmentStr + ", M=" + midsemStr + ", E=" + endsemStr);

                    // Parse values; skip if empty
                    if (quizStr.trim().isEmpty() || assignmentStr.trim().isEmpty() || midsemStr.trim().isEmpty() || endsemStr.trim().isEmpty()) {
                        saveLog.append("Row ").append(r + 1).append(" (").append(rollNo).append("): Skipped (empty values)\n");
                        continue;
                    }

                    double quiz = Double.parseDouble(quizStr);
                    double assignment = Double.parseDouble(assignmentStr);
                    double midsem = Double.parseDouble(midsemStr);
                    double endsem = Double.parseDouble(endsemStr);

                    // compute weighted total out of 100 using course weight percentages
                    double weightedTotal = 0.0;
                    weightedTotal += quiz * (weights[0] / 100.0);
                    weightedTotal += assignment * (weights[1] / 100.0);
                    weightedTotal += midsem * (weights[2] / 100.0);
                    weightedTotal += endsem * (weights[3] / 100.0);

                    // round to 2 decimals for display/storage
                    double totalRounded = Math.round(weightedTotal * 100.0) / 100.0;
                    String letterGrade = edu.univ.erp.data.GradeDAO.computeLetterGrade(totalRounded);

                    System.out.println("[DEBUG] Saving: rollNo=" + rollNo + ", course=" + courseCode + 
                                     ", marks(" + quiz + "," + assignment + "," + midsem + "," + endsem + 
                                     "), total=" + totalRounded + ", grade=" + letterGrade);

                    boolean ok = gradeDAO.updateGrade(rollNo, courseCode, quiz, assignment, midsem, endsem, totalRounded);
                    if (!ok) {
                        allOk = false;
                        saveLog.append("Row ").append(r + 1).append(" (").append(rollNo).append("): FAILED to save\n");
                    } else {
                        saveLog.append("Row ").append(r + 1).append(" (").append(rollNo).append("): Saved as ").append(letterGrade).append("\n");
                        // update Total and Final Grade columns in UI with calculated values
                        model.setValueAt(String.format("%.2f", totalRounded), r, 6);
                        model.setValueAt(letterGrade, r, 7);
                    }
                } catch (NumberFormatException ex) {
                    allOk = false;
                    saveLog.append("Row ").append(r + 1).append(": Invalid number format\n");
                    ex.printStackTrace();
                }
            }
            String message = allOk ? "✓ All grades saved successfully.\n\n" + saveLog.toString() : "✗ Some rows failed to save:\n\n" + saveLog.toString();
            JOptionPane.showMessageDialog(dialog, message, "Save Result", JOptionPane.INFORMATION_MESSAGE);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showCourseStatistics() {
        CourseDAO courseDAO = new CourseDAO();
        List<String> courses = courseDAO.getCoursesByInstructor(instructorEmail);
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses found.");
            return;
        }
        String selectedCourse = (String) JOptionPane.showInputDialog(
                this,
                "Select a course:",
                "Course Statistics",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                null
        );
        if (selectedCourse == null) return;

        String courseCode = courseDAO.getCourseCodeByName(selectedCourse);
        GradeDAO gradeDAO = new GradeDAO();
        String stats = gradeDAO.generateCourseStats(courseCode);
        Map<String, Integer> distribution = gradeDAO.getGradeDistribution(courseCode);

        JDialog dialog = new JDialog(this, "Course Statistics - " + selectedCourse, true);
        dialog.setSize(720, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JTextArea statsArea = new JTextArea(stats);
        statsArea.setEditable(false);
        statsArea.setWrapStyleWord(true);
        statsArea.setLineWrap(true);
        statsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statsArea.setBackground(new Color(248, 248, 248));

        JScrollPane statsScroll = new JScrollPane(statsArea);
        statsScroll.setPreferredSize(new Dimension(280, 0));
        dialog.add(statsScroll, BorderLayout.WEST);

        GradePieChartPanel chartPanel = new GradePieChartPanel(distribution);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(chartPanel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void updateCapacity() {
        CourseDAO courseDAO = new CourseDAO();
        List<String> courses = courseDAO.getCoursesByInstructor(instructorEmail);

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses assigned to you.");
            return;
        }

        String selectedCourse = (String) JOptionPane.showInputDialog(
                this,
                "Choose a course:",
                "Update Capacity",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                courses.get(0)
        );

        if (selectedCourse == null) return;

        String courseCode = courseDAO.getCourseCodeByName(selectedCourse);
        MaintenanceDAO mdao = new MaintenanceDAO();
        if (mdao.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this, "Maintenance mode is ON. No changes will be recorded.");
            return;
        }
        String capacityStr = JOptionPane.showInputDialog(this, "Enter new capacity:");

        try {
            int newCap = Integer.parseInt(capacityStr.trim());
            boolean updated = courseDAO.updateCourseCapacity(courseCode, newCap);
            JOptionPane.showMessageDialog(this,
                    updated ? "Capacity updated!" : "Failed to update capacity.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number entered.");
        }
    }

    private void exportGradesCSV() {
        CourseDAO courseDAO = new CourseDAO();
        java.util.List<String> courses = courseDAO.getCoursesByInstructor(instructorEmail);

        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses assigned to you.");
            return;
        }

        String selectedCourse = (String) JOptionPane.showInputDialog(
                this,
                "Select a course to export grades:",
                "Export Grades CSV",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                courses.get(0)
        );

        if (selectedCourse == null) return;

        String courseCode = courseDAO.getCourseCodeByName(selectedCourse);
        GradeDAO gradeDAO = new GradeDAO();
        String path = gradeDAO.exportGradesToCSV(courseCode);
        if (path != null) {
            JOptionPane.showMessageDialog(this, "Grades exported to: " + path, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Export failed or no grade data available.", "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openChangePassword() {
        ChangePasswordDialog.showDialog(this, instructorEmail);
    }

    private static class GradePieChartPanel extends JPanel {
        private final Map<String, Integer> distribution;
        private final Color[] palette = new Color[]{
                new Color(56, 189, 248),
                new Color(14, 165, 233),
                new Color(99, 102, 241),
                new Color(129, 140, 248),
                new Color(248, 113, 113),
                new Color(251, 191, 36),
                new Color(16, 185, 129)
        };

        GradePieChartPanel(Map<String, Integer> distribution) {
            this.distribution = distribution;
            setPreferredSize(new Dimension(400, 320));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (distribution == null || distribution.isEmpty()) {
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(getFont().deriveFont(Font.PLAIN, 15f));
                g2.drawString("No grade distribution data available.", 20, getHeight() / 2);
                g2.dispose();
                return;
            }

            int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
            int diameter = Math.min(getWidth() - 180, getHeight() - 40);
            diameter = Math.max(diameter, 120);
            int x = 20;
            int y = (getHeight() - diameter) / 2;

            int startAngle = 0;
            int idx = 0;
            int entryCount = distribution.size();

            int i = 0;
            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                double pct = (double) entry.getValue() / total;
                int angle = (i == entryCount - 1) ? 360 - startAngle : (int) Math.round(pct * 360);
                if (idx >= palette.length) idx = 0;
                g2.setColor(palette[idx++]);
                g2.fillArc(x, y, diameter, diameter, startAngle, angle);
                startAngle += angle;
                i++;
            }

            int legendX = x + diameter + 20;
            int legendY = y;
            idx = 0;
            g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));

            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                if (idx >= palette.length) idx = 0;
                g2.setColor(palette[idx]);
                g2.fillRect(legendX, legendY, 16, 16);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(legendX, legendY, 16, 16);
                g2.drawString(entry.getKey() + " : " + entry.getValue(), legendX + 24, legendY + 12);
                legendY += 24;
                idx++;
            }

            g2.dispose();
        }
    }
}
