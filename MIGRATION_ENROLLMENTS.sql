-- ============================================================
-- Migration: Add Enrollments Table for Many-to-Many Courses
-- ============================================================
-- This migration creates a separate enrollments table to support
-- students enrolling in multiple courses simultaneously.

-- Step 1: Create the enrollments table
CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_roll_no VARCHAR(50) NOT NULL,
    course_code VARCHAR(10) NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_enrollment (student_roll_no, course_code),
    FOREIGN KEY (student_roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(code) ON DELETE CASCADE,
    INDEX idx_student (student_roll_no),
    INDEX idx_course (course_code)
);

-- Step 2: Migrate existing single-course data to enrollments table
INSERT INTO enrollments (student_roll_no, course_code)
SELECT roll_no, course_code FROM students 
WHERE course_code IS NOT NULL AND course_code != '';

-- Step 3: Drop the course_code column from students table (OPTIONAL - do after verifying)
-- ALTER TABLE students DROP COLUMN course_code;

-- Step 4: Verify migration
SELECT 'Students enrolled in multiple courses:' AS info;
SELECT student_roll_no, COUNT(*) as course_count, GROUP_CONCAT(course_code) as courses
FROM enrollments
GROUP BY student_roll_no
HAVING course_count > 1;

-- ============================================================
-- Run the above steps in your MySQL database
-- ============================================================
