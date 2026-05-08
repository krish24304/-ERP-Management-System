package edu.univ.erp.ui;

import javax.swing.*;
import edu.univ.erp.ui.auth.LoginFrame; // import the LoginFrame
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.TimetableDAO;
import edu.univ.erp.data.InstructorDAO;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class Main {
    public static void main(String[] args) {
        // Set FlatLaf Look and Feel for modern UI
        try {
            FlatLaf.registerCustomDefaultsSource("edu.univ.erp.ui.common");
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }

        // Convert any old numeric grades to letter format on startup
        GradeDAO gradeDAO = new GradeDAO();
        gradeDAO.convertNumericGradesToLetters();

        // Ensure courses have drop_deadline column and defaults, then timetables
        edu.univ.erp.data.CourseDAO courseDAO = new edu.univ.erp.data.CourseDAO();
        courseDAO.ensureDropDeadlineColumnExists();
        courseDAO.setDefaultDeadlinesIfMissing();

        // Ensure timetables exist (generate simple random slots if missing)
        TimetableDAO tt = new TimetableDAO();
        tt.generateTimetablesIfMissing();

        // Migrate plaintext passwords to bcrypt
        InstructorDAO.migratePasswordsToBcrypt();

        // If exportCourse system property is provided, run CSV export and exit (CLI test mode)
        String exportCourse = System.getProperty("exportCourse");
        if (exportCourse != null && !exportCourse.isBlank()) {
            GradeDAO gdao = new GradeDAO();
            String path = gdao.exportGradesToCSV(exportCourse.trim());
            if (path != null) {
                System.out.println("Exported CSV to: " + path);
                System.exit(0);
            } else {
                System.err.println("Export failed or no data for course: " + exportCourse);
                System.exit(2);
            }
        }

        SwingUtilities.invokeLater(() -> {
            // open the login window instead of a blank frame
            new LoginFrame().setVisible(true);
        });
    }
}
