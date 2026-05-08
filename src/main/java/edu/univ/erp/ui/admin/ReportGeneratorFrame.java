package edu.univ.erp.ui.admin;

import edu.univ.erp.data.*;
import com.opencsv.CSVWriter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportGeneratorFrame extends JFrame {

    private StudentDAO studentDAO;
    private InstructorDAO instructorDAO;
    private CourseDAO courseDAO;

    public ReportGeneratorFrame() {
        studentDAO = new StudentDAO();
        instructorDAO = new InstructorDAO();
        courseDAO = new CourseDAO();

        setTitle("Generate Reports");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 1, 10, 10));

        JButton studentReportBtn = new JButton("Export Students (CSV)");
        JButton instructorReportBtn = new JButton("Export Instructors (CSV)");
        JButton courseReportBtn = new JButton("Export Courses (PDF)");
        JButton closeBtn = new JButton("Close");

        studentReportBtn.addActionListener(e -> exportStudentsToCSV());
        instructorReportBtn.addActionListener(e -> exportInstructorsToCSV());
        courseReportBtn.addActionListener(e -> exportCoursesToPDF());
        closeBtn.addActionListener(e -> dispose());

        add(studentReportBtn);
        add(instructorReportBtn);
        add(courseReportBtn);
        add(closeBtn);
    }

    private void exportStudentsToCSV() {
        try {
            String filePath = "reports/students_report.csv";
            List<String[]> students = studentDAO.getAllStudents();

            CSVWriter writer = new CSVWriter(new FileWriter(filePath));
            writer.writeNext(new String[]{"Name", "Roll No", "Course", "Year"});
            for (String[] s : students) writer.writeNext(s);
            writer.close();

            JOptionPane.showMessageDialog(this, "Students report saved at:\n" + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating student report.");
        }
    }

    private void exportInstructorsToCSV() {
        try {
            String filePath = "reports/instructors_report.csv";
            List<String[]> instructors = instructorDAO.getAllInstructors();

            CSVWriter writer = new CSVWriter(new FileWriter(filePath));
            writer.writeNext(new String[]{"Name", "Department", "Email"});
            for (String[] i : instructors) writer.writeNext(i);
            writer.close();

            JOptionPane.showMessageDialog(this, "Instructors report saved at:\n" + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating instructor report.");
        }
    }

    private void exportCoursesToPDF() {
        try {
            String filePath = "reports/courses_report.pdf";
            List<String[]> courses = courseDAO.getAllCourses();

            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            content.setFont(PDType1Font.HELVETICA_BOLD, 16);
            content.beginText();
            content.newLineAtOffset(50, 750);
            content.showText("Courses Report");
            content.endText();

            content.setFont(PDType1Font.HELVETICA, 12);
            int y = 720;
            for (String[] c : courses) {
                String line = String.format("%s | %s | Credits: %s | Instructor: %s",
                        c[0], c[1], c[2], c[3]);
                content.beginText();
                content.newLineAtOffset(50, y);
                content.showText(line);
                content.endText();
                y -= 20;
                if (y < 50) {
                    content.close();
                    page = new PDPage();
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = 750;
                }
            }

            content.close();
            document.save(filePath);
            document.close();

            JOptionPane.showMessageDialog(this, "Courses PDF saved at:\n" + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating course report.");
        }
    }
}
