package edu.univ.erp.ui.student;

import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.MaintenanceDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ChooseCoursesFrame extends JFrame {

    private final String studentUsername;
    private final CourseDAO dao;

    private JTable courseTable;
    private DefaultTableModel model;

    public ChooseCoursesFrame(String studentUsername) {
        this.studentUsername = studentUsername;
        this.dao = new CourseDAO();

        setTitle("Course Registration - " + studentUsername);
        setSize(750, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Use tabs: Available Courses | My Courses (to drop)
        JTabbedPane tabs = new JTabbedPane();

        // Available courses panel
        JPanel availPanel = new JPanel(new BorderLayout());
        model = new DefaultTableModel(
                new String[]{"Course Name", "Course Code", "Instructor", "Seats"},
                0
        );
        courseTable = new JTable(model);
        courseTable.setRowHeight(24);
        availPanel.add(new JScrollPane(courseTable), BorderLayout.CENTER);
        JButton registerBtn = new JButton("Register for Selected Course");
        registerBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerBtn.addActionListener(e -> registerStudent());
        availPanel.add(registerBtn, BorderLayout.SOUTH);

        // My courses panel (enrolled)
        JPanel myPanel = new JPanel(new BorderLayout());
        DefaultTableModel enrolledModel = new DefaultTableModel(
                new String[]{"Course Name", "Course Code", "Instructor"}, 0);
        JTable enrolledTable = new JTable(enrolledModel);
        enrolledTable.setRowHeight(24);
        myPanel.add(new JScrollPane(enrolledTable), BorderLayout.CENTER);
        JButton dropBtn = new JButton("Drop Selected Course");
        dropBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        myPanel.add(dropBtn, BorderLayout.SOUTH);

        tabs.addTab("Available Courses", availPanel);
        tabs.addTab("My Courses", myPanel);

        add(tabs, BorderLayout.CENTER);

        // actions
        dropBtn.addActionListener(e -> {
            int sel = enrolledTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Please select a course to drop.");
                return;
            }
            String courseCode = enrolledModel.getValueAt(sel, 1).toString();
            MaintenanceDAO mdao = new MaintenanceDAO();
            if (mdao.isMaintenanceMode()) {
                JOptionPane.showMessageDialog(this, "Maintenance mode is ON. No changes will be recorded.");
                return;
            }
            boolean ok = dao.dropStudentFromCourse(studentUsername, courseCode);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Course dropped successfully. All related data removed.");
                refreshTable();
                // refresh enrolled
                refreshEnrolled(enrolledModel);
            } else {
                JOptionPane.showMessageDialog(this, "Unable to drop course. It may be past the drop deadline or an error occurred.");
            }
        });

        refreshTable();
        refreshEnrolled(enrolledModel);

        MaintenanceDAO mdao = new MaintenanceDAO();
        if (mdao.isMaintenanceMode()) {
            JLabel banner = new JLabel("⚠ Maintenance is ON: No changes will be recorded ⚠", SwingConstants.CENTER);
            banner.setOpaque(true);
            banner.setBackground(new Color(255, 230, 130));
            banner.setForeground(Color.RED);
            banner.setFont(new Font("Segoe UI", Font.BOLD, 16));
            add(banner, BorderLayout.NORTH);
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<String[]> courses = dao.getAvailableCourses();

        for (String[] c : courses) {
            model.addRow(new Object[]{c[0], c[1], c[2], c[3]});
        }
    }

    private void registerStudent() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to register.");
            return;
        }

        // 🔹 studentUsername is actually the roll_no (set by StudentDashboard.studentEmail)
        String courseCode = model.getValueAt(selectedRow, 1).toString();

        MaintenanceDAO mdao = new MaintenanceDAO();
        if (mdao.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this, "Maintenance mode is ON. No changes will be recorded.");
            return;
        }

        boolean success = dao.registerStudentToCourse(studentUsername, courseCode);

        if (success) {
            JOptionPane.showMessageDialog(this, "🎉 Enrollment Successful!");
            refreshTable(); // 🔄 Refresh to show updated seats
        } else {
            JOptionPane.showMessageDialog(this, "⚠ Enrollment Failed!\n\nPossible reasons:\n• No seats available\n• Already enrolled in this course\n• Course not found\n• Student not found");
        }
    }

    private void refreshEnrolled(DefaultTableModel enrolledModel) {
        enrolledModel.setRowCount(0);
        List<String[]> enrolled = dao.getEnrolledCourses(studentUsername);
        for (String[] c : enrolled) {
            enrolledModel.addRow(new Object[]{c[0], c[1], c[2]});
        }
    }
}
