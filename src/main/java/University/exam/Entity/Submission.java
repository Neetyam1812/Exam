package University.exam.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class Submission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "student_enrollment_no", referencedColumnName = "enrollment_no")
    private Student student;
    
    @ManyToOne
    @JoinColumn(name = "paper_id")
    private Paper paper;
    
    private String status; // Pending, Checked
    private LocalDateTime submittedAt;

    private Boolean isPaused = false;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private Integer remainingTimeSeconds;
    private LocalDateTime lastSavedAt;
    private Integer resumeCount = 0;
    private Integer pauseCount = 0;
    private String interruptionReason;
    private Integer lastActiveSection = 0;
    private Long lastActiveQuestionId;

    public Submission() {}

    public Submission(Long id, Student student, Paper paper, String status, LocalDateTime submittedAt) {
        this.id = id;
        this.student = student;
        this.paper = paper;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Paper getPaper() { return paper; }
    public void setPaper(Paper paper) { this.paper = paper; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Boolean getIsPaused() { return isPaused; }
    public void setIsPaused(Boolean paused) { isPaused = paused; }
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    public LocalDateTime getResumedAt() { return resumedAt; }
    public void setResumedAt(LocalDateTime resumedAt) { this.resumedAt = resumedAt; }
    public Integer getRemainingTimeSeconds() { return remainingTimeSeconds; }
    public void setRemainingTimeSeconds(Integer remainingTimeSeconds) { this.remainingTimeSeconds = remainingTimeSeconds; }
    public LocalDateTime getLastSavedAt() { return lastSavedAt; }
    public void setLastSavedAt(LocalDateTime lastSavedAt) { this.lastSavedAt = lastSavedAt; }
    public Integer getResumeCount() { return resumeCount; }
    public void setResumeCount(Integer resumeCount) { this.resumeCount = resumeCount; }
    public Integer getPauseCount() { return pauseCount; }
    public void setPauseCount(Integer pauseCount) { this.pauseCount = pauseCount; }
    public String getInterruptionReason() { return interruptionReason; }
    public void setInterruptionReason(String interruptionReason) { this.interruptionReason = interruptionReason; }
    public Integer getLastActiveSection() { return lastActiveSection; }
    public void setLastActiveSection(Integer lastActiveSection) { this.lastActiveSection = lastActiveSection; }
    public Long getLastActiveQuestionId() { return lastActiveQuestionId; }
    public void setLastActiveQuestionId(Long lastActiveQuestionId) { this.lastActiveQuestionId = lastActiveQuestionId; }
}
