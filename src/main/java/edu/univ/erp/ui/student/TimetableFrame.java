package edu.univ.erp.ui.student;

import edu.univ.erp.data.TimetableDAO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TimetableFrame extends JFrame {

    public TimetableFrame(String studentRollNo) {
        setTitle("Your Timetable");
        setSize(650, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        TimetableDAO dao = new TimetableDAO();
        List<String[]> rows = dao.getTimetableForStudent(studentRollNo);

        String[] cols = {"Course", "Course Code", "Day", "Start Time", "Duration (mins)"};

        String[][] tableData = rows.toArray(new String[0][]);

        JTable table = new JTable(tableData, cols);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(26);

        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);

        if (rows.isEmpty()) {
            JLabel lbl = new JLabel("No timetable entries found. Administrator may not have generated slots yet.", SwingConstants.CENTER);
            add(lbl, BorderLayout.SOUTH);
        }
    }
}
