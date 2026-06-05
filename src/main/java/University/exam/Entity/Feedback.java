package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_enrollment_no", referencedColumnName = "enrollment_no")
    private Student student;

    // Use exam_id or paper_id depending on the type of exam
    private Long examId;
    
    // "exam" or "paper"
    private String examType;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private String systemEasyToUse;
    private String paperClear;
    private String technicalIssues;

    private LocalDateTime submittedAt;

    public Feedback() {}

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

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSystemEasyToUse() {
        return systemEasyToUse;
    }

    public void setSystemEasyToUse(String systemEasyToUse) {
        this.systemEasyToUse = systemEasyToUse;
    }

    public String getPaperClear() {
        return paperClear;
    }

    public void setPaperClear(String paperClear) {
        this.paperClear = paperClear;
    }

    public String getTechnicalIssues() {
        return technicalIssues;
    }

    public void setTechnicalIssues(String technicalIssues) {
        this.technicalIssues = technicalIssues;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
