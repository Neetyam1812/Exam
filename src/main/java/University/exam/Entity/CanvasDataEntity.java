package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "canvas_data")
public class CanvasDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", unique = true)
    private Answer answer;

    @Column(columnDefinition = "TEXT")
    private String canvasJson;

    @Column(columnDefinition = "TEXT")
    private String canvasImage; // Base64 PNG representation for static display & PDF

    private LocalDateTime updatedAt;

    public CanvasDataEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }

    public String getCanvasJson() { return canvasJson; }
    public void setCanvasJson(String canvasJson) { this.canvasJson = canvasJson; }

    public String getCanvasImage() { return canvasImage; }
    public void setCanvasImage(String canvasImage) { this.canvasImage = canvasImage; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
