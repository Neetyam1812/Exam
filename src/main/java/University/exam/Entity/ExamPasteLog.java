package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_paste_logs")
public class ExamPasteLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private Long questionId;
    private LocalDateTime time;
    private String pasteAttempt;
    private String sourceType;

    public ExamPasteLog() {}

    public ExamPasteLog(String studentId, Long questionId, LocalDateTime time, String pasteAttempt, String sourceType) {
        this.studentId = studentId;
        this.questionId = questionId;
        this.time = time;
        this.pasteAttempt = pasteAttempt;
        this.sourceType = sourceType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public String getPasteAttempt() { return pasteAttempt; }
    public void setPasteAttempt(String pasteAttempt) { this.pasteAttempt = pasteAttempt; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
