package edu.univ.erp.ui.admin;

import edu.univ.erp.data.StudentDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentManagementFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private StudentDAO dao;

    public StudentManagementFrame() {
        dao = new StudentDAO();
        setTitle("Manage Students");
        setSize(700, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 🆕 Table with ONLY 3 Columns
        model = new DefaultTableModel(
                new String[]{"Name", "Roll No", "Status (Course Enrollment)"}, 0
        );
        table = new JTable(model);
        refreshTable();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Student");
        JButton deleteButton = new JButton("Delete Student");

        addButton.addActionListener(e -> openAddStudentDialog());
        deleteButton.addActionListener(e -> handleDeleteStudent());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // 🆕 Refresh table — now matches 3-column StudentDAO output
    private void refreshTable() {
        model.setRowCount(0);
        List<String[]> students = dao.getAllStudents();
        for (String[] row : students) {
            model.addRow(new Object[]{row[0], row[1], row[2]});
        }
    }

    // 🆕 Add student — only Name & Roll No
    private void openAddStudentDialog() {
        JTextField nameField = new JTextField();
        JTextField rollField = new JTextField();

        Object[] form = {
                "Student Name:", nameField,
                "Roll Number:", rollField,
        };

        int result = JOptionPane.showConfirmDialog(this, form, "Add Student", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            boolean success = dao.addStudent(nameField.getText(), rollField.getText());

            if (success) {
                JOptionPane.showMessageDialog(this, "Student added successfully!");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Error adding student.");
            }
        }
    }

    // 🆕 Delete student
    private void handleDeleteStudent() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.");
            return;
        }

        String rollNo = (String) model.getValueAt(selectedRow, 1);
        dao.deleteStudent(rollNo);

        JOptionPane.showMessageDialog(this, "Student deleted successfully!");
        refreshTable();
    }
}
