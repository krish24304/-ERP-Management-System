AP ERP PROJECT
│
├── reports/                 # Generated Reports (CSV/PDF)
│   ├── courses_report.pdf
│   ├── CS301_grades.csv
│   ├── instructors_report.csv
│   └── students_report.csv
│
├── src/main/java/edu/univ/erp/
│   ├── data/                # DAO Classes - Database Access
│   │   ├── CourseDAO.java
│   │   ├── StudentDAO.java
│   │   ├── GradeDAO.java
│   │   ├── InstructorDAO.java
│   │   ├── MaintenanceDAO.java
│   │   ├── DBConnection.java
│   │   └── TimetableDAO.java
│   │
│   ├── service/             # Business Logic
│   │   └── AuthService.java
│   │
│   ├── tools/               # CSV Auth Testing Tools
│   │   ├── ExportCsvRunner.java
│   │   ├── TestAuthRunner.java
│   │   └── VerifyPasswordStorage.java
│   │
│   ├── ui/                  # GUI (Swing)
│   │   ├── admin/
│   │   │   ├── AdminDashboard.java
│   │   │   ├── CourseManagementFrame.java
│   │   │   ├── InstructorManagementFrame.java
│   │   │   ├── ReportGeneratorFrame.java
│   │   │   └── StudentManagementFrame.java
│   │   │
│   │   ├── auth/
│   │   │   └── LoginFrame.java
│   │   │
│   │   ├── instructor/
│   │   │   └── InstructorDashboard.java
│   │   │
│   │   ├── student/
│   │   │   ├── ChooseCoursesFrame.java
│   │   │   ├── CourseRegistrationFrame.java
│   │   │   ├── StudentDashboard.java
│   │   │   └── TimetableFrame.java
│   │   └── common/
│   │       └── UIHelper.java
│   │
│   ├── util/
│   │   ├── DatabaseMigration.java
│   │   └── InspectDB.java
│   │
│   └── Main.java
│
├── Submissions/
│   ├── ERP_PROJECT_REPORT.docx
│   ├── HowtoRun.txt
│   ├── Sampledata.txt
│   └── Test_Plan_and_Summary.txt
│
├── DATABASE_MIGRATION_STEPS.md
├── MIGRATION_ENROLLMENTS.sql
├── pom.xml
└── target/

⭐ Features Implemented
🔹 Admin Dashboard

✔ Add / Delete Courses
✔ Manage Students and Instructors
✔ Run Maintenance Mode (Block edits, allow login)
✔ Generate Reports (CSV & PDF)

🔹 Instructor Dashboard

✔ View assigned courses
✔ Update course capacity
✔ Manage Student Grades
✔ Generate Course Statistics (marks, avg, pass/fail)
✔ View enrolled students list

🔹 Student Dashboard

✔ Register multiple courses (Enrollment Table)
✔ View Grades and Final Results
✔ View Timetable
✔ View enrolled courses


🛡 Security

✔ BCrypt password hashing
✔ Maintenance Mode Access Limiting
✔ Prevent duplicate enrollments
✔ Role-based access (Admin/Instructor/Student)

🧪 Testing & Validation

✔ SQL test dataset using Sampledata.txt
✔ Authentication test using TestAuthRunner.java
✔ Table inspection via InspectDB.java

🎯 Future Enhancements

🔹 Automated timetable generation
🔹 OTP-based password recovery
🔹 Admin notifications dashboard
🔹 Semester-wise transcript generation



🔹 Maintenance Mode
Feature	Behavior
Admin	Can perform changes
Instructor	Can login but cannot update data
Student	Can login but cannot register courses
Messages	“Maintenance Mode Enabled – Changes Disabled” warning
📊 Database Tables Used
Table	Description
users	Login credentials with BCrypt
courses	Course data, weightages, deadlines
students	Student profiles
enrollments	Dynamic student-course mapping
grades	Marks and final grades
maintenance_mode	Maintenance status
📄 Reports Generated (in /reports/)
File	Description
students_report.csv	List of students
instructors_report.csv	Faculty report
courses_report.pdf	Course details PDF
CS301_grades.csv	Course-wise grade export


 hoe to run -   cd "C:\Users\Nikhil\OneDrive\Desktop\AP ERP PROJECT" ; mvn clean compile exec:java PROJECT>