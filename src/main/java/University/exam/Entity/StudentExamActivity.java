package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_exam_activity")
public class StudentExamActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "current_section")
    private String currentSection;

    @Column(name = "current_question_no")
    private Integer currentQuestionNo;

    @Column(name = "time_remaining")
    private String timeRemaining;

    @Column(name = "status")
    private String status; // Active, Inactive, Submitted, Terminated

    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentExamActivity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(String currentSection) {
        this.currentSection = currentSection;
    }

    public Integer getCurrentQuestionNo() {
        return currentQuestionNo;
    }

    public void setCurrentQuestionNo(Integer currentQuestionNo) {
        this.currentQuestionNo = currentQuestionNo;
    }

    public String getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(String timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
