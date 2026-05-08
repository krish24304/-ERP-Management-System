package edu.univ.erp.ui.student;

import edu.univ.erp.data.CourseDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CourseRegistrationFrame extends JFrame {

    private final String studentRollNo;
    private JTable courseTable;
    private DefaultTableModel model;
    private final CourseDAO dao = new CourseDAO();

    public CourseRegistrationFrame(String studentRollNo) {
        this.studentRollNo = studentRollNo;

        setTitle("Course Registration - " + studentRollNo);
        setSize(600, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Course Name", "Course Code", "Instructor", "Seats Left"}, 0);
        courseTable = new JTable(model);
        add(new JScrollPane(courseTable), BorderLayout.CENTER);

        JButton registerBtn = new JButton("Register for Selected Course");
        registerBtn.addActionListener(e -> registerStudent());
        add(registerBtn, BorderLayout.SOUTH);

        refreshTable();
    }

    // 🔹 Load courses with available seats
    private void refreshTable() {
        model.setRowCount(0);
        List<String[]> courses = dao.getAvailableCourses();

        for (String[] c : courses) {
            model.addRow(c);
        }
    }

    // 🔹 Register student to course
    private void registerStudent() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course first.");
            return;
        }

        String courseCode = (String) courseTable.getValueAt(selectedRow, 1);

        boolean success = dao.registerStudentToCourse(studentRollNo, courseCode);

        if (success) {
            JOptionPane.showMessageDialog(this, "🎉 Registration Successful!");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "⚠ Registration Failed. No seats available.");
        }
    }
}
