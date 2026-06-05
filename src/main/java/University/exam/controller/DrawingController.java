package University.exam.controller;

import University.exam.Entity.Answer;
import University.exam.Entity.CanvasDataEntity;
import University.exam.Entity.ExamAttempt;
import University.exam.Entity.Question;
import University.exam.Entity.Submission;
import University.exam.repository.AnswerRepository;
import University.exam.repository.ExamAttemptRepository;
import University.exam.repository.QuestionRepository;
import University.exam.repository.SubmissionRepository;
import University.exam.service.DrawingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/drawing")
public class DrawingController {

    @Autowired
    private DrawingService drawingService;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @PostMapping("/save")
    public ResponseEntity<?> saveDrawing(@RequestBody Map<String, Object> payload, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Long attemptId = payload.get("attemptId") != null ? Long.valueOf(payload.get("attemptId").toString()) : null;
        Long submissionId = payload.get("submissionId") != null ? Long.valueOf(payload.get("submissionId").toString()) : null;
        Long questionId = payload.get("questionId") != null ? Long.valueOf(payload.get("questionId").toString()) : null;
        String canvasJson = payload.get("canvasJson") != null ? payload.get("canvasJson").toString() : "";
        String canvasImage = payload.get("canvasImage") != null ? payload.get("canvasImage").toString() : "";

        if (questionId == null) {
            return ResponseEntity.badRequest().body("Missing Question ID");
        }

        // Find or create the corresponding Answer entity
        Answer answer;
        if (attemptId != null) {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt == null || "Submitted".equals(attempt.getStatus())) {
                return ResponseEntity.badRequest().body("Invalid attempt");
            }
            Question question = questionRepository.findById(questionId).orElse(null);
            answer = answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, questionId)
                    .orElseGet(() -> {
                        Answer a = new Answer();
                        a.setExamAttempt(attempt);
                        a.setQuestion(question);
                        a.setQuestionText(question != null ? question.getText() : "Theory Answer");
                        a.setMaxMarks(question != null ? question.getMarks() : 0.0);
                        a.setStudentAnswer("");
                        a.setUpdatedAt(LocalDateTime.now());
                        return answerRepository.save(a);
                    });
        } else if (submissionId != null) {
            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission == null || "Submitted".equals(submission.getStatus())) {
                return ResponseEntity.badRequest().body("Invalid submission");
            }
            Question question = questionRepository.findById(questionId).orElse(null);
            answer = answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(submissionId, questionId)
                    .orElseGet(() -> {
                        Answer a = new Answer();
                        a.setSubmission(submission);
                        a.setQuestion(question);
                        a.setQuestionText(question != null ? question.getText() : "Paper Answer");
                        a.setMaxMarks(question != null ? question.getMarks() : 0.0);
                        a.setStudentAnswer("");
                        a.setUpdatedAt(LocalDateTime.now());
                        return answerRepository.save(a);
                    });
        } else {
            return ResponseEntity.badRequest().body("Missing Attempt/Submission ID");
        }

        drawingService.saveDrawing(answer, canvasJson, canvasImage);

        Map<String, String> response = new HashMap<>();
        response.put("status", "saved");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getDrawing(
            @RequestParam(name = "attemptId", required = false) Long attemptId,
            @RequestParam(name = "submissionId", required = false) Long submissionId,
            @RequestParam("questionId") Long questionId,
            HttpSession session) {
        
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<Answer> answerOpt = Optional.empty();
        if (attemptId != null) {
            answerOpt = answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, questionId);
        } else if (submissionId != null) {
            answerOpt = answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(submissionId, questionId);
        }

        if (answerOpt.isPresent()) {
            Optional<CanvasDataEntity> drawingOpt = drawingService.getDrawingByAnswerId(answerOpt.get().getId());
            if (drawingOpt.isPresent()) {
                CanvasDataEntity cd = drawingOpt.get();
                Map<String, String> response = new HashMap<>();
                response.put("canvasJson", cd.getCanvasJson());
                response.put("canvasImage", cd.getCanvasImage());
                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.ok(Collections.emptyMap());
    }
}
