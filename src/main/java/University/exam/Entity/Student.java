package University.exam.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

@Entity
@Table(name = "students")
@ToString(exclude = "password")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "enrollment_no", unique = true, length = 20)
    private String enrollmentNo;

    @Column(name = "division", length = 20)
    private String division;

    @Column(name = "roll_no")
    private Integer rollNo;

    @Column(name = "phone_no", length = 15)
    private String phoneNo;

    @JsonIgnore
    @Column(name = "password", length = 100)
    private String password;

    // Persistent column for semester-based access control
    @jakarta.persistence.Column(name = "semester", length = 50)
    private String semester = "Semester 3";

    // Default Constructor
    public Student() {}

    // Legacy constructor compatibility for existing controllers
    public Student(String enrollmentNo, String password) {
        this.enrollmentNo = enrollmentNo;
        this.password = password;
        this.studentName = "Unknown Student";
        this.division = "Unknown Division";
        this.semester = "Unknown Semester";
    }

    // Parameterized Constructor without ID (for saving new students)
    public Student(String studentName, String enrollmentNo, String division, Integer rollNo, String phoneNo, String password) {
        this.studentName = studentName;
        this.enrollmentNo = enrollmentNo;
        this.division = division;
        this.rollNo = rollNo;
        this.phoneNo = phoneNo;
        this.password = password;
    }

    // Fully Parameterized Constructor
    public Student(Long id, String studentName, String enrollmentNo, String division, Integer rollNo, String phoneNo, String password) {
        this.id = id;
        this.studentName = studentName;
        this.enrollmentNo = enrollmentNo;
        this.division = division;
        this.rollNo = rollNo;
        this.phoneNo = phoneNo;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    // Legacy Support alias for getStudentName
    public String getName() {
        return studentName;
    }

    // Legacy Support alias for setStudentName
    public void setName(String name) {
        this.studentName = name;
    }

    public String getEnrollmentNo() {
        return enrollmentNo;
    }

    public void setEnrollmentNo(String enrollmentNo) {
        this.enrollmentNo = enrollmentNo;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public Integer getRollNo() {
        return rollNo;
    }

    public void setRollNo(Integer rollNo) {
        this.rollNo = rollNo;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Persistent semester getter & setter with default fallback
    public String getSemester() {
        return (semester == null || semester.trim().isEmpty()) ? "Semester 3" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    // Static helper to match semesters robustly (e.g. comparing "3" and "Semester 3")
    public static boolean matchesSemester(String studentSem, String examSem) {
        if (studentSem == null || examSem == null) return false;
        String s1 = studentSem.trim().toLowerCase().replaceAll("\\s+", "");
        String s2 = examSem.trim().toLowerCase().replaceAll("\\s+", "");
        if (s1.equals(s2)) return true;
        String d1 = s1.replaceAll("\\D+", "");
        String d2 = s2.replaceAll("\\D+", "");
        if (!d1.isEmpty() && !d2.isEmpty() && d1.equals(d2)) return true;
        return s1.contains(s2) || s2.contains(s1);
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentName='" + studentName + '\'' +
                ", enrollmentNo='" + enrollmentNo + '\'' +
                ", division='" + division + '\'' +
                ", rollNo=" + rollNo +
                ", phoneNo='" + phoneNo + '\'' +
                ", semester='" + semester + '\'' +
                '}';
    }
}
