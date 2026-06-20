package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_violations")
public class ExamViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "time", nullable = false)
    private LocalDateTime time;

    @Column(name = "event_type", nullable = false)
    private String eventType; // TAB_SWITCH, WINDOW_SWITCH, FOCUS_LOST

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "exam_type")
    private String examType; // "paper" or "exam"

    public ExamViolation() {}

    public ExamViolation(String studentId, LocalDateTime time, String eventType, Long examId, String examType) {
        this.studentId = studentId;
        this.time = time;
        this.eventType = eventType;
        this.examId = examId;
        this.examType = examType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }
}
