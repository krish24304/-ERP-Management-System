package edu.univ.erp.ui.common;

import edu.univ.erp.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {

    private final String username;
    private final AuthService authService = new AuthService();

    private final JPasswordField oldPasswordField = new JPasswordField(15);
    private final JPasswordField newPasswordField = new JPasswordField(15);
    private final JPasswordField confirmPasswordField = new JPasswordField(15);
    private final char defaultEchoChar;

    public ChangePasswordDialog(Frame owner, String username) {
        super(owner, "Change Password", true);
        this.username = username;

        setSize(400, 260);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        form.add(new JLabel("Old Password:"));
        form.add(oldPasswordField);
        form.add(new JLabel("New Password:"));
        form.add(newPasswordField);
        form.add(new JLabel("Confirm New Password:"));
        form.add(confirmPasswordField);

        defaultEchoChar = oldPasswordField.getEchoChar();

        add(form, BorderLayout.CENTER);

        JCheckBox showPasswords = new JCheckBox("Show passwords");
        showPasswords.addActionListener(e -> togglePasswords(showPasswords.isSelected()));
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        togglePanel.add(showPasswords);
        add(togglePanel, BorderLayout.NORTH);

        JButton submitBtn = new JButton("Update Password");
        submitBtn.addActionListener(e -> handleSubmit());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(submitBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void handleSubmit() {
        String oldPwd = new String(oldPasswordField.getPassword()).trim();
        String newPwd = new String(newPasswordField.getPassword()).trim();
        String confirmPwd = new String(confirmPasswordField.getPassword()).trim();

        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPwd.length() < 6) {
            JOptionPane.showMessageDialog(this, "New password must be at least 6 characters long.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPwd.equals(oldPwd)) {
            JOptionPane.showMessageDialog(this, "New password must be different from the old password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AuthService.PasswordChangeResult result = authService.changePassword(username, oldPwd, newPwd);
        switch (result) {
            case SUCCESS -> {
                JOptionPane.showMessageDialog(this, "Password updated successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
            case INVALID_OLD_PASSWORD -> JOptionPane.showMessageDialog(this,
                    "Old password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
            case USER_NOT_FOUND -> JOptionPane.showMessageDialog(this,
                    "User account was not found.", "Error", JOptionPane.ERROR_MESSAGE);
            default -> JOptionPane.showMessageDialog(this,
                    "Failed to update password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showDialog(Frame owner, String username) {
        if (username == null || username.isBlank()) {
            JOptionPane.showMessageDialog(owner, "Unable to determine account for password change.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ChangePasswordDialog dialog = new ChangePasswordDialog(owner, username);
        dialog.setVisible(true);
    }

    private void togglePasswords(boolean visible) {
        char echo = visible ? 0 : defaultEchoChar;
        oldPasswordField.setEchoChar(echo);
        newPasswordField.setEchoChar(echo);
        confirmPasswordField.setEchoChar(echo);
    }
}

