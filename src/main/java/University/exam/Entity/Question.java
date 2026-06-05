package University.exam.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Double marks;
    private String questionGroup;
    @Column(name = "is_optional")
    private boolean isOptional;

    @Column(name = "is_or_option")
    private boolean isOrOption;

    @Column(name = "pair_id")
    private String pairId;

    public Question() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }
    public Paper getPaper() { return paper; }
    public void setPaper(Paper paper) { this.paper = paper; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Double getMarks() { return marks; }
    public void setMarks(Double marks) { this.marks = marks; }
    public String getQuestionGroup() { return questionGroup; }
    public void setQuestionGroup(String questionGroup) { this.questionGroup = questionGroup; }
    
    public String getPairId() { return pairId; }
    public void setPairId(String pairId) { this.pairId = pairId; }
    
    public boolean isOptional() { return isOptional; }
    public void setOptional(boolean isOptional) { 
        this.isOptional = isOptional; 
        this.isOrOption = isOptional; // Sync both columns
    }
    
    public boolean isOrOption() { return isOrOption; }
    public void setOrOption(boolean isOrOption) { 
        this.isOrOption = isOrOption; 
        this.isOptional = isOrOption; // Sync both columns
    }
}
