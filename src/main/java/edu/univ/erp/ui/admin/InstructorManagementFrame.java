package edu.univ.erp.ui.admin;

import edu.univ.erp.data.InstructorDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InstructorManagementFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private InstructorDAO dao;

    public InstructorManagementFrame() {
        dao = new InstructorDAO();
        setTitle("Manage Instructors");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"Name", "Department", "Email"}, 0);
        table = new JTable(model);
        refreshTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Instructor");
        JButton deleteButton = new JButton("Delete Instructor");

        addButton.addActionListener(e -> openAddDialog());
        deleteButton.addActionListener(e -> handleDelete());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<String[]> instructors = dao.getAllInstructors();
        for (String[] s : instructors) model.addRow(s);
    }

    private void openAddDialog() {
        JTextField nameField = new JTextField();
        JTextField deptField = new JTextField();
        JTextField emailField = new JTextField();

        Object[] form = {
            "Name:", nameField,
            "Department:", deptField,
            "Email:", emailField
        };

        int result = JOptionPane.showConfirmDialog(this, form, "Add Instructor", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            boolean success = dao.addInstructor(
                nameField.getText(),
                deptField.getText(),
                emailField.getText()
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Instructor added successfully!");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Error adding instructor.");
            }
        }
    }

    private void handleDelete() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an instructor to delete.");
            return;
        }

        String email = (String) model.getValueAt(selectedRow, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete instructor with Email: " + email + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = dao.deleteInstructor(email);
            if (success) {
                JOptionPane.showMessageDialog(this, "Instructor deleted successfully!");
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Error deleting instructor.");
            }
        }
    }
}
