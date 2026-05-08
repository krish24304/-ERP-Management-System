package edu.univ.erp.service;

import edu.univ.erp.data.DBConnection;
import edu.univ.erp.data.StudentDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class AuthService {

    private String instructorEmail = null;
    private String studentEmail = null;
    private String studentRollNo = null;   // Store Roll Number for students
    private String lastLoggedInUsername = null;
    private final StudentDAO studentDAO = new StudentDAO();

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MILLIS = 5 * 60 * 1000L; // 5 minutes
    private record AttemptInfo(int failures, long lockUntil) { }

    /**
     * Authenticate user using USERS table with username and password.
     * Username = lowercase name (students), instructor email (instructors)
     * Password = roll number (students), actual password (others)
     */
    public String authenticate(String username, String password) {

        // 🔹 Step 1: Maintenance Mode Check (Allow ONLY Admin to Login)
        String normalized = normalize(username);
        ensureAttemptsTable();
        if (isAccountLocked(normalized)) {
            return "ACCOUNT_LOCKED";
        }
        // Do NOT block anyone from login. Just continue, everyone can login.

        // 🔹 Step 2: Normal authentication
        String query = "SELECT password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, normalized);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password_hash");
                String role = rs.getString("role");

                boolean authenticated = false;

                // BCrypt authentication
                if (storedPassword != null && storedPassword.startsWith("$2")) {
                    authenticated = BCrypt.checkpw(password, storedPassword);

                // Legacy plaintext password — migrate to BCrypt
                } else if (password.equals(storedPassword)) {
                    authenticated = true;

                    try (Connection upd = DBConnection.getAuthConnection();
                         PreparedStatement ups = upd.prepareStatement(
                                 "UPDATE users SET password_hash = ? WHERE username = ?")) {
                        String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                        ups.setString(1, newHash);
                        ups.setString(2, normalized);
                        ups.executeUpdate();
                    } catch (SQLException ue) {
                        ue.printStackTrace();
                    }
                }

                if (authenticated) {
                    clearAttemptInfo(normalized);
                    lastLoggedInUsername = normalized;
                    if ("Instructor".equals(role)) {
                        instructorEmail = normalized;
                    }
                    if ("Student".equals(role)) {
                        studentEmail = normalized;
                        studentRollNo = resolveStudentRoll(normalized);
                        if (studentRollNo == null || studentRollNo.isBlank()) {
                            studentRollNo = password; // fallback
                        }
                    }
                    return role;  // SUCCESS
                }
                registerFailure(normalized);
                return null; // invalid password
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        registerFailure(normalized);
        return null; // User not found
    }

    public String getLoggedInInstructorEmail() {
        return instructorEmail;
    }

    public String getLoggedInStudentEmail() {
        return studentEmail;
    }

    public String getLoggedInStudentRollNo() {
        return studentRollNo;
    }

    public String getLoggedInUsername() {
        return lastLoggedInUsername;
    }

    public int getRemainingAttempts(String username) {
        AttemptInfo info = fetchAttemptInfo(normalize(username));
        if (info == null) return MAX_ATTEMPTS;
        long now = Instant.now().toEpochMilli();
        if (now < info.lockUntil && info.failures >= MAX_ATTEMPTS) {
            return 0;
        }
        return Math.max(0, MAX_ATTEMPTS - info.failures);
    }

    public long getLockRemainingSeconds(String username) {
        AttemptInfo info = fetchAttemptInfo(normalize(username));
        if (info == null) return 0;
        long now = Instant.now().toEpochMilli();
        if (now >= info.lockUntil) {
            return 0;
        }
        long millisRemaining = info.lockUntil - now;
        return (millisRemaining + 999) / 1000; // ceil to seconds
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    public int getLockoutDurationMinutes() {
        return (int) (LOCK_DURATION_MILLIS / 60000L);
    }

    private boolean isAccountLocked(String username) {
        AttemptInfo info = fetchAttemptInfo(username);
        if (info == null) return false;
        long now = Instant.now().toEpochMilli();
        if (info.lockUntil > now) {
            return true;
        }
        if (info.lockUntil > 0 && now >= info.lockUntil) {
            saveAttemptInfo(username, new AttemptInfo(0, 0));
        }
        return false;
    }

    private void registerFailure(String username) {
        AttemptInfo existing = fetchAttemptInfo(username);
        if (existing == null) {
            saveAttemptInfo(username, new AttemptInfo(1, 0));
            return;
        }
        int failures = existing.failures + 1;
        if (failures >= MAX_ATTEMPTS) {
            long lockUntil = Instant.now().toEpochMilli() + LOCK_DURATION_MILLIS;
            saveAttemptInfo(username, new AttemptInfo(failures, lockUntil));
        } else {
            saveAttemptInfo(username, new AttemptInfo(failures, existing.lockUntil));
        }
    }

    private String normalize(String username) {
        return username == null ? "" : username.toLowerCase().replaceAll("\\s+", "");
    }

    private String resolveStudentRoll(String normalizedUsername) {
        return studentDAO.getRollNoByUsername(normalizedUsername);
    }

    private AttemptInfo fetchAttemptInfo(String username) {
        if (username.isBlank()) return null;
        ensureAttemptsTable();
        String sql = "SELECT failures, lock_until FROM login_attempts WHERE username = ?";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AttemptInfo(rs.getInt("failures"), rs.getLong("lock_until"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveAttemptInfo(String username, AttemptInfo info) {
        if (username.isBlank()) return;
        ensureAttemptsTable();
        String update = "UPDATE login_attempts SET failures = ?, lock_until = ? WHERE username = ?";
        String insert = "INSERT INTO login_attempts (failures, lock_until, username) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement up = conn.prepareStatement(update);
             PreparedStatement in = conn.prepareStatement(insert)) {
            up.setInt(1, info.failures);
            up.setLong(2, info.lockUntil);
            up.setString(3, username);
            int rows = up.executeUpdate();
            if (rows == 0) {
                in.setInt(1, info.failures);
                in.setLong(2, info.lockUntil);
                in.setString(3, username);
                in.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearAttemptInfo(String username) {
        if (username.isBlank()) return;
        ensureAttemptsTable();
        String delete = "DELETE FROM login_attempts WHERE username = ?";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(delete)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureAttemptsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS login_attempts (
                    username VARCHAR(255) PRIMARY KEY,
                    failures INT NOT NULL,
                    lock_until BIGINT NOT NULL
                )
                """;
        try (Connection conn = DBConnection.getAuthConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public enum PasswordChangeResult {
        SUCCESS,
        USER_NOT_FOUND,
        INVALID_OLD_PASSWORD,
        ERROR
    }

    public PasswordChangeResult changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.isBlank()) {
            return PasswordChangeResult.USER_NOT_FOUND;
        }
        String normalized = normalize(username);
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalized);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return PasswordChangeResult.USER_NOT_FOUND;
                }
                String stored = rs.getString("password_hash");
                boolean matches = false;
                if (stored != null && stored.startsWith("$2")) {
                    matches = BCrypt.checkpw(oldPassword, stored);
                } else if (stored != null) {
                    matches = oldPassword.equals(stored);
                }
                if (!matches) {
                    return PasswordChangeResult.INVALID_OLD_PASSWORD;
                }
            }

            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET password_hash = ? WHERE username = ?")) {
                update.setString(1, newHash);
                update.setString(2, normalized);
                update.executeUpdate();
            }
            clearAttemptInfo(normalized);
            return PasswordChangeResult.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return PasswordChangeResult.ERROR;
        }
    }
}
