package edu.univ.erp.ui.student;

import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.ui.common.ChangePasswordDialog;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class StudentDashboard extends JFrame {

    private final String studentRollNo;
    private final String loginUsername;

    public StudentDashboard(String username, String studentRollNo, String loginUsername) {
        this.studentRollNo = studentRollNo;
        this.loginUsername = loginUsername;

        setTitle("Student Dashboard - " + username);
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel heading = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(heading, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // 🔹 View Grades Button
        JButton gradesButton = new JButton("View Grades");
        gradesButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gradesButton.addActionListener(e -> viewGrades());
        buttonPanel.add(gradesButton);

        // 🔹 Course Registration Button
        JButton chooseCourseButton = new JButton("Choose / Register Courses");
        chooseCourseButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        chooseCourseButton.addActionListener(e -> openCourseRegistration());
        buttonPanel.add(chooseCourseButton);

        // 🔹 View Results Button
        JButton viewResultsBtn = new JButton("View Results");
        viewResultsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        viewResultsBtn.addActionListener(e -> viewResults());
        buttonPanel.add(viewResultsBtn);

        // 🔹 View Timetable Button
        JButton timetableBtn = new JButton("View Timetable");
        timetableBtn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        timetableBtn.addActionListener(e -> {
            TimetableFrame tf = new TimetableFrame(studentRollNo);
            tf.setVisible(true);
        });
        buttonPanel.add(timetableBtn);

        // 🔹 Print/Export Grades Button
        JButton printButton = new JButton("Print/Export Grades");
        printButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        printButton.addActionListener(e -> chooseExportFormat());
        buttonPanel.add(printButton);

        // 🔹 Change Password Button
        JButton changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        changePasswordBtn.addActionListener(e -> ChangePasswordDialog.showDialog(this, loginUsername));
        buttonPanel.add(changePasswordBtn);

        // 🔹 Logout Button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        logoutButton.setBackground(new Color(220, 53, 69));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> {
            dispose();
            JOptionPane.showMessageDialog(null, "Logged out successfully.");
            System.exit(0);
        });

        buttonPanel.add(logoutButton);
        add(buttonPanel, BorderLayout.CENTER);
    }

    private void viewGrades() {
        GradeDAO gradeDAO = new GradeDAO();
        List<String[]> data = gradeDAO.getResultsByStudent(studentRollNo);

        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No grades found.");
            return;
        }

        String[] columns = {"Course", "Total Marks (out of 100)", "Final Grade"};
        String[][] tableData = data.toArray(new String[0][]);

        JTable table = new JTable(tableData, columns);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 300));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Your Grades", JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewResults() {
        viewGrades(); // Same function works for results
    }

    private void openCourseRegistration() {
        new ChooseCoursesFrame(studentRollNo).setVisible(true);
    }

    private void downloadGradesCsv() {
        try {
            GradeDAO dao = new GradeDAO();
            java.util.List<String[]> data = dao.getResultsByStudent(studentRollNo);
            if (data.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No grades found to export.");
                return;
            }
            File reportsDir = new File("reports");
            if (!reportsDir.exists()) reportsDir.mkdirs();
            String fileName = String.format("reports/%s_grades.csv", studentRollNo);
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                pw.println("Course,Total Marks (out of 100),Final Grade");
                for (String[] row : data) {
                    // Quote values to support commas
                    for (int i = 0; i < row.length; i++) {
                        pw.print('"' + row[i].replace("\"", "\"\"") + '"');
                        if (i < row.length - 1) pw.print(",");
                    }
                    pw.println();
                }
                pw.flush();
            }
            String abs = new File(fileName).getAbsolutePath();
            JOptionPane.showMessageDialog(this, "Your grades have been exported to: " + abs, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export grades: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseExportFormat() {
        String[] options = {"PDF", "CSV", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Choose format to download your grades:",
                "Export Grades", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                options, options[0]);
        if (choice == 0) exportGradesPdf();
        if (choice == 1) downloadGradesCsv();
    }

    private void exportGradesPdf() {
        try {
            GradeDAO dao = new GradeDAO();
            java.util.List<String[]> data = dao.getResultsByStudent(studentRollNo);
            if (data.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No grades found to export.");
                return;
            }
            String filePath = String.format("reports/%s_grades.pdf", studentRollNo);
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.setFont(PDType1Font.HELVETICA_BOLD, 16);
            content.beginText();
            content.newLineAtOffset(50, 750);
            content.showText("Grade Report");
            content.endText();
            content.setFont(PDType1Font.HELVETICA, 12);
            int y = 720;
            content.beginText();
            content.newLineAtOffset(50, y);
            content.showText("Course                       Total Marks    Final Grade");
            content.endText();
            y -= 20;
            for (String[] row : data) {
                String line = String.format("%-28s %-14s %s", row[0], row[1], row[2]);
                content.beginText();
                content.newLineAtOffset(50, y);
                content.showText(line);
                content.endText();
                y -= 18;
                if (y < 50) {
                    content.close();
                    page = new PDPage();
                    doc.addPage(page);
                    content = new PDPageContentStream(doc, page);
                    y = 750;
                }
            }
            content.close();
            doc.save(filePath);
            doc.close();
            JOptionPane.showMessageDialog(this, "Your grades have been exported to PDF:\n" + new java.io.File(filePath).getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export grades as PDF: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
