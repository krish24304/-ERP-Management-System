package edu.univ.erp.tools;

import edu.univ.erp.data.GradeDAO;

public class ExportCsvRunner {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -cp target/classes edu.univ.erp.tools.ExportCsvRunner <COURSE_CODE>");
            return;
        }
        String course = args[0];
        GradeDAO dao = new GradeDAO();
        String path = dao.exportGradesToCSV(course);
        if (path != null) {
            System.out.println("Export successful: " + path);
        } else {
            System.out.println("Export failed or no data for course: " + course);
        }
    }
}
