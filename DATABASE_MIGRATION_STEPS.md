# Database Migration Instructions

## Step 1: Run the Migration SQL

Open your MySQL client and run the SQL from `MIGRATION_ENROLLMENTS.sql`:

```sql
-- Create the enrollments table
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

-- Migrate existing single-course data to enrollments table
INSERT INTO enrollments (student_roll_no, course_code)
SELECT roll_no, course_code FROM students 
WHERE course_code IS NOT NULL AND course_code != '';

-- Verify migration
SELECT 'Students enrolled in multiple courses:' AS info;
SELECT student_roll_no, COUNT(*) as course_count, GROUP_CONCAT(course_code) as courses
FROM enrollments
GROUP BY student_roll_no
HAVING course_count > 1;
```

## Step 2: Verify in MySQL

Run this to check if the table exists and has data:
```sql
SELECT * FROM enrollments LIMIT 5;
SELECT COUNT(*) FROM enrollments;
```

## Step 3: Test the Application

Once the table is created:
```powershell
mvn clean compile exec:java
```

Login as a student and try enrolling in multiple courses — they should all persist now!

## Optional: Drop the old course_code column (after confirming)

If you want to fully migrate away from the old system:
```sql
ALTER TABLE students DROP COLUMN course_code;
```

This will force the app to use only the `enrollments` table going forward.
