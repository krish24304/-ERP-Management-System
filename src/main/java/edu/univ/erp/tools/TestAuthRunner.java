package edu.univ.erp.tools;

import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.service.AuthService;

public class TestAuthRunner {
    public static void main(String[] args) {
        InstructorDAO dao = new InstructorDAO();
        String name = "Test Instructor";
        String dept = "Test Dept";
        String email = "test.instructor+autotest@univ.edu"; // unique test email

        System.out.println("[TestAuthRunner] Adding instructor (if not exists): " + email);
        boolean added = dao.addInstructor(name, dept, email);
        if (added) {
            System.out.println("[TestAuthRunner] Instructor added (or already existed).");
        } else {
            System.out.println("[TestAuthRunner] Instructor insertion failed or already exists; proceeding to authentication test.");
        }

        AuthService auth = new AuthService();
        System.out.println("[TestAuthRunner] Attempting authenticate with username=email and password=inst123 (plaintext not shown)...");
        String role = auth.authenticate(email, "inst123");
        if (role != null) {
            System.out.println("[TestAuthRunner] Authentication SUCCESS. Role=" + role);
        } else {
            System.out.println("[TestAuthRunner] Authentication FAILED.");
        }

        System.out.println("[TestAuthRunner] Done. No password hashes printed.");
    }
}
