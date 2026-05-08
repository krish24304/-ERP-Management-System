package edu.univ.erp.ui.auth;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.admin.AdminDashboard;
import edu.univ.erp.ui.instructor.InstructorDashboard;
import edu.univ.erp.ui.student.StudentDashboard;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private final AuthService authService;
    private JLabel statusLabel;
    private javax.swing.Timer lockTimer;

    public LoginFrame() {
        authService = new AuthService();

        setTitle("University ERP - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        final char defaultEcho = passwordField.getEchoChar();
        JPanel passwordWrapper = new JPanel(new BorderLayout());
        passwordWrapper.add(passwordField, BorderLayout.CENTER);
        JCheckBox showPasswordToggle = new JCheckBox("Show");
        showPasswordToggle.setFocusable(false);
        showPasswordToggle.addActionListener(e -> togglePasswordVisibility(showPasswordToggle.isSelected(), defaultEcho));
        passwordWrapper.add(showPasswordToggle, BorderLayout.EAST);
        panel.add(passwordWrapper);

        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        loginButton.addActionListener(e -> handleLogin());

        panel.add(new JLabel());  // Empty placeholder
        panel.add(loginButton);

        add(panel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String role = authService.authenticate(username, password);

        if (role == null) {
            int remaining = authService.getRemainingAttempts(username);
            StringBuilder msg = new StringBuilder("❌ Invalid username or password.");
            if (remaining > 0 && remaining < authService.getMaxAttempts()) {
                msg.append("\nAttempts remaining before lock: ").append(remaining);
                stopLockCountdown();
            } else if (remaining == 0) {
                long seconds = authService.getLockRemainingSeconds(username);
                if (seconds > 0) {
                    long minutes = Math.max(1, (seconds + 59) / 60);
                    msg.append("\nAccount locked for approximately ").append(minutes).append(" minute(s).");
                    startLockCountdown(seconds);
                } else {
                    msg.append("\nAccount will lock on the next incorrect attempt.");
                    stopLockCountdown();
                }
            } else {
                stopLockCountdown();
            }
            JOptionPane.showMessageDialog(this,
                    msg.toString(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);

        } else if ("ACCOUNT_LOCKED".equals(role)) {
            long seconds = authService.getLockRemainingSeconds(username);
            long minutes = Math.max(1, (seconds + 59) / 60);
            JOptionPane.showMessageDialog(this,
                    "🚫 Too many failed attempts.\nPlease wait about " + minutes + " minute(s) before trying again.",
                    "Account Temporarily Locked",
                    JOptionPane.WARNING_MESSAGE);
            startLockCountdown(seconds);

        } else if (role.equals("MAINTENANCE_BLOCK")) {
            JOptionPane.showMessageDialog(this,
                    "🚧 ERP is currently under maintenance.\nOnly Admin can login.",
                    "Maintenance Mode Active",
                    JOptionPane.WARNING_MESSAGE);

        } else {
            // Successful login
            stopLockCountdown();
            dispose();

            switch (role) {
                case "Admin" -> new AdminDashboard(username).setVisible(true);

                case "Instructor" -> new InstructorDashboard(
                        username,
                        authService.getLoggedInInstructorEmail()
                ).setVisible(true);

                case "Student" -> new StudentDashboard(
                        username,
                        authService.getLoggedInStudentRollNo(),  // Pass Roll No properly
                        authService.getLoggedInUsername()
                ).setVisible(true);

                default -> JOptionPane.showMessageDialog(this,
                        "Unknown role detected: " + role);
            }
        }
    }

    private void startLockCountdown(long seconds) {
        if (seconds <= 0) {
            stopLockCountdown();
            statusLabel.setText(" ");
            return;
        }
        final long[] remaining = {seconds};
        statusLabel.setText(formatCountdown(remaining[0]));
        if (lockTimer != null) lockTimer.stop();
        lockTimer = new javax.swing.Timer(1000, e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                ((javax.swing.Timer) e.getSource()).stop();
                statusLabel.setText("You can try logging in again now.");
            } else {
                statusLabel.setText(formatCountdown(remaining[0]));
            }
        });
        lockTimer.start();
    }

    private void stopLockCountdown() {
        if (lockTimer != null) {
            lockTimer.stop();
            lockTimer = null;
        }
        statusLabel.setText(" ");
    }

    private String formatCountdown(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("Account locked. Try again in %02d:%02d (mm:ss).", mins, secs);
    }

    private void togglePasswordVisibility(boolean visible, char defaultEcho) {
        passwordField.setEchoChar(visible ? (char) 0 : defaultEcho);
    }
}
