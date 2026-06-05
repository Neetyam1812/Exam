package University.exam.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;

import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "answers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"submission_id", "question_id"}),
    @UniqueConstraint(columnNames = {"exam_attempt_id", "question_id"})
})
public class Answer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;
    
    @ManyToOne
    @JoinColumn(name = "exam_attempt_id")
    private ExamAttempt examAttempt;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
    
    private String questionText;
    
    @Column(columnDefinition = "TEXT")
    private String studentAnswer;
    
    private Double maxMarks;
    private Double marksObtained;
    private String feedback;
    private java.time.LocalDateTime updatedAt;

    public Answer() {}

    public Answer(Long id, Submission submission, String questionText, String studentAnswer, Double maxMarks, Double marksObtained, String feedback) {
        this.id = id;
        this.submission = submission;
        this.questionText = questionText;
        this.studentAnswer = studentAnswer;
        this.maxMarks = maxMarks;
        this.marksObtained = marksObtained;
        this.feedback = feedback;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }
    public ExamAttempt getExamAttempt() { return examAttempt; }
    public void setExamAttempt(ExamAttempt examAttempt) { this.examAttempt = examAttempt; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public Double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Double maxMarks) { this.maxMarks = maxMarks; }
    public Double getMarksObtained() { return marksObtained; }
    public void setMarksObtained(Double marksObtained) { this.marksObtained = marksObtained; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @OneToOne(mappedBy = "answer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CanvasDataEntity canvasData;

    public CanvasDataEntity getCanvasData() { return canvasData; }
    public void setCanvasData(CanvasDataEntity canvasData) { this.canvasData = canvasData; }
}
