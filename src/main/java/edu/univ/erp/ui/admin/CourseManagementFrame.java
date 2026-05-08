package edu.univ.erp.ui.admin;

import edu.univ.erp.data.CourseDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CourseManagementFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private final CourseDAO dao;

    public CourseManagementFrame() {
        dao = new CourseDAO();

        setTitle("Manage Courses");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Columns MUST match CourseDAO.getAllCourses()
        model = new DefaultTableModel(
            new String[]{
                "Name", "Code", "Credits", "Instructor Email",
                "Capacity", "Quiz%", "Assignment%", "Midsem%", "Endsem%", "Drop Deadline"
            }, 0
        );

        table = new JTable(model);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable();

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Course");
        JButton deleteButton = new JButton("Delete Course");

        addButton.addActionListener(e -> openAddDialog());
        deleteButton.addActionListener(e -> handleDelete());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<String[]> courses = dao.getAllCourses();
        for (String[] c : courses) {
            model.addRow(c);
        }
    }

    private void openAddDialog() {
        JTextField nameField = new JTextField();
        JTextField codeField = new JTextField();
        JTextField creditsField = new JTextField();
        JTextField instructorField = new JTextField();
        JTextField capacityField = new JTextField("40"); // default capacity

        JTextField quizField = new JTextField("15");
        JTextField assignmentField = new JTextField("20");
        JTextField midsemField = new JTextField("30");
        JTextField endsemField = new JTextField("35");
        JTextField dropDeadlineField = new JTextField();

        Object[] form = {
                "Course Name:", nameField,
                "Course Code:", codeField,
                "Credits:", creditsField,
                "Instructor Email:", instructorField,
                "Capacity:", capacityField,
                "Quiz %:", quizField,
                "Assignment %:", assignmentField,
                "Midsem %:", midsemField,
                "Endsem %:", endsemField
                , "Drop Deadline (YYYY-MM-DD HH:MM) - optional:", dropDeadlineField
        };

        int result = JOptionPane.showConfirmDialog(
                this, form, "Add Course", JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                int credits = Integer.parseInt(creditsField.getText().trim());
                int capacity = Integer.parseInt(capacityField.getText().trim());
                int quiz = Integer.parseInt(quizField.getText().trim());
                int assign = Integer.parseInt(assignmentField.getText().trim());
                int mid = Integer.parseInt(midsemField.getText().trim());
                int end = Integer.parseInt(endsemField.getText().trim());

                int total = quiz + assign + mid + end;
                if (total != 100) {
                    JOptionPane.showMessageDialog(this,
                            "Weightages must sum to 100. Current sum: " + total);
                    return;
                }

                String dd = dropDeadlineField.getText().trim();
                if (dd.isBlank()) {
                    dd = "2025-12-05 12:00:00"; // default per requirement
                }

                boolean success = dao.addCourse(
                    nameField.getText().trim(),
                    codeField.getText().trim(),
                    credits,
                    instructorField.getText().trim(),
                    quiz, assign, mid, end, capacity,
                    dd
                );

                if (success) {
                    JOptionPane.showMessageDialog(this, "Course added successfully!");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding course.");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numeric values.");
            }
        }
    }

    private void handleDelete() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to delete.");
            return;
        }

        String code = (String) model.getValueAt(selectedRow, 1);
        if (dao.deleteCourse(code)) {
            JOptionPane.showMessageDialog(this, "Course deleted successfully!");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "Error deleting course.");
        }
    }
}
