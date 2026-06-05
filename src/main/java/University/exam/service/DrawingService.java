package University.exam.service;

import University.exam.Entity.CanvasDataEntity;
import University.exam.Entity.Answer;
import University.exam.repository.DrawingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DrawingService {

    @Autowired
    private DrawingRepository drawingRepository;

    @Transactional
    public CanvasDataEntity saveDrawing(Answer answer, String canvasJson, String canvasImage) {
        CanvasDataEntity canvasData = drawingRepository.findByAnswerId(answer.getId())
                .orElseGet(() -> {
                    CanvasDataEntity cd = new CanvasDataEntity();
                    cd.setAnswer(answer);
                    return cd;
                });
        canvasData.setCanvasJson(canvasJson);
        canvasData.setCanvasImage(canvasImage);
        canvasData.setUpdatedAt(LocalDateTime.now());
        return drawingRepository.save(canvasData);
    }

    public Optional<CanvasDataEntity> getDrawingByAnswerId(Long answerId) {
        return drawingRepository.findByAnswerId(answerId);
    }
}
