package edu.univ.erp.ui.common;

import javax.swing.*;
import java.awt.*;

public class UIHelper {

    // Method to create a styled title label
    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(new Color(30, 60, 120)); // dark blue
        label.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        return label;
    }

    // Method to set a consistent background color
    public static void applyBackground(JPanel panel) {
        panel.setBackground(new Color(235, 242, 250)); // light blue-gray
    }

    // Method to show a uniform message dialog
    public static void showMessage(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg);
    }
}
