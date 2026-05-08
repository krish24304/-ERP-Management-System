package edu.univ.erp.ui.admin;

import edu.univ.erp.data.MaintenanceDAO;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {

    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();

    public AdminDashboard(String username) {
        setTitle("Admin Dashboard - Logged in as " + username);
        setSize(700, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 🔹 Heading
        JLabel heading = new JLabel("Welcome, Admin!", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(heading, BorderLayout.NORTH);

        // 🔹 Maintenance Toggle – placed below heading
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JCheckBox maintenanceToggle = new JCheckBox("Enable Maintenance Mode");

        maintenanceToggle.setSelected(maintenanceDAO.isMaintenanceMode());
        maintenanceToggle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        maintenanceToggle.addActionListener(e -> {
            boolean isOn = maintenanceToggle.isSelected();
            maintenanceDAO.setMaintenanceMode(isOn);

            JOptionPane.showMessageDialog(this,
                    isOn ? "🚧 ERP Maintenance Mode enabled.\nOnly Admin can login."
                         : "✅ ERP is back online. Students & Instructors can login now."
            );
        });

        topPanel.add(maintenanceToggle);
        add(topPanel, BorderLayout.AFTER_LAST_LINE);

        // 🔸 Main options grid
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        String[] options = {
                "Manage Students",
                "Manage Instructors",
                "Manage Courses",
                "Generate Reports"
        };

        for (String opt : options) {
            JButton btn = new JButton(opt);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setFocusPainted(false);

            switch (opt) {
                case "Manage Students" -> btn.addActionListener(e -> new StudentManagementFrame().setVisible(true));
                case "Manage Instructors" -> btn.addActionListener(e -> new InstructorManagementFrame().setVisible(true));
                case "Manage Courses" -> btn.addActionListener(e -> new CourseManagementFrame().setVisible(true));
                case "Generate Reports" -> btn.addActionListener(e -> new ReportGeneratorFrame().setVisible(true));
            }
            buttonPanel.add(btn);
        }

        add(buttonPanel, BorderLayout.CENTER);

        // 🔹 Logout panel (BOTTOM-RIGHT small button)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.setBackground(new Color(220, 53, 69));
        logoutBtn.setForeground(Color.WHITE);

        logoutBtn.addActionListener(e -> {
            dispose();
            JOptionPane.showMessageDialog(null, "You have been logged out.");
            System.exit(0);
        });

        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}
