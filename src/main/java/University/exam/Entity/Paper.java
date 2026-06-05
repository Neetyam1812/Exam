package University.exam.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "papers")
public class Paper {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String subject;
    private String course;
    private String semester;
    private String filePath;
    private LocalDateTime uploadedAt;
    @jakarta.persistence.Column(name = "exam_duration")
    private Integer examDuration;
    private Double totalMarks;

    @jakarta.persistence.Column(name = "exam_status")
    private String examStatus = "DRAFT"; // DRAFT, PUBLISHED, ACTIVE, ENDED

    @jakarta.persistence.Column(name = "activation_time")
    private LocalDateTime activationTime;

    @jakarta.persistence.Column(name = "published_time")
    private LocalDateTime publishedTime;

    @jakarta.persistence.Column(name = "activated_time")
    private LocalDateTime activatedTime;

    @jakarta.persistence.Column(name = "manual_content", columnDefinition = "TEXT")
    private String manualContent;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "admin_id")
    private Admin admin;

    public Paper() {}


    public Paper(Long id, String subject, String course, String semester, String filePath, LocalDateTime uploadedAt) {
        this.id = id;
        this.subject = subject;
        this.course = course;
        this.semester = semester;
        this.filePath = filePath;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getExamName() { return subject; } // Alias for unified template support
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public Integer getExamDuration() { return examDuration; }
    public void setExamDuration(Integer examDuration) { this.examDuration = examDuration; }
    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }
    public String getManualContent() { return manualContent; }
    public void setManualContent(String manualContent) { this.manualContent = manualContent; }
    
    public String getExamStatus() { return examStatus; }
    public void setExamStatus(String examStatus) { this.examStatus = examStatus; }
    public LocalDateTime getActivationTime() { return activationTime; }
    public void setActivationTime(LocalDateTime activationTime) { this.activationTime = activationTime; }

    public LocalDateTime getPublishedTime() { return publishedTime; }
    public void setPublishedTime(LocalDateTime publishedTime) { this.publishedTime = publishedTime; }
    public LocalDateTime getActivatedTime() { return activatedTime; }
    public void setActivatedTime(LocalDateTime activatedTime) { this.activatedTime = activatedTime; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}

