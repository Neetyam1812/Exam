package University.exam.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String examName;
    private String course;
    @Column(name = "target_semester")
    private String semester;
    private String subject;
    private Double totalMarks;
    @Column(name = "exam_duration")
    private Integer examDuration;

    @Column(name = "exam_status")
    private String examStatus = "DRAFT"; // DRAFT, PUBLISHED, ACTIVE, ENDED

    @Column(name = "activation_time")
    private java.time.LocalDateTime activationTime;

    @Column(name = "published_time")
    private java.time.LocalDateTime publishedTime;

    @Column(name = "activated_time")
    private java.time.LocalDateTime activatedTime;

    public Exam() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }
    public Integer getExamDuration() { return examDuration; }
    public void setExamDuration(Integer examDuration) { this.examDuration = examDuration; }

    public String getExamStatus() { return examStatus; }
    public void setExamStatus(String examStatus) { this.examStatus = examStatus; }
    public java.time.LocalDateTime getActivationTime() { return activationTime; }
    public void setActivationTime(java.time.LocalDateTime activationTime) { this.activationTime = activationTime; }

    public java.time.LocalDateTime getPublishedTime() { return publishedTime; }
    public void setPublishedTime(java.time.LocalDateTime publishedTime) { this.publishedTime = publishedTime; }
    public java.time.LocalDateTime getActivatedTime() { return activatedTime; }
    public void setActivatedTime(java.time.LocalDateTime activatedTime) { this.activatedTime = activatedTime; }
}

