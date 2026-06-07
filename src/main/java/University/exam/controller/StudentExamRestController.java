package University.exam.controller;

import University.exam.Entity.*;
import University.exam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class StudentExamRestController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamPasteLogRepository examPasteLogRepository;

    @GetMapping("/exam/{id}/status")
    public ResponseEntity<?> getExamStatus(@PathVariable("id") Long id, @RequestParam(name = "type", required = false) String type) {
        String status = "DRAFT";
        if ("exam".equals(type)) {
            Optional<Exam> examOpt = examRepository.findById(id);
            if (examOpt.isPresent()) {
                status = examOpt.get().getExamStatus();
            }
        } else {
            Optional<Paper> paperOpt = paperRepository.findById(id);
            if (paperOpt.isPresent()) {
                status = paperOpt.get().getExamStatus();
            } else {
                Optional<Exam> examOpt = examRepository.findById(id);
                if (examOpt.isPresent()) {
                    status = examOpt.get().getExamStatus();
                }
            }
        }
        if (status == null) status = "DRAFT";
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams")
    public ResponseEntity<?> getExams() {
        return ResponseEntity.ok(examRepository.findAll());
    }


    @GetMapping("/exam/{id}")
    public ResponseEntity<?> getExamDetails(@PathVariable("id") Long id) {
        return examRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/exam/{id}/questions")
    public ResponseEntity<?> getShuffledQuestions(@PathVariable("id") Long id, @RequestParam(name = "type", required = false) String type, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";

        String examSem = "Semester 3";
        if ("paper".equals(type)) {
            Paper paper = paperRepository.findById(id).orElse(null);
            if (paper != null) examSem = paper.getSemester();
        } else {
            Exam exam = examRepository.findById(id).orElse(null);
            if (exam != null) examSem = exam.getSemester();
        }

        if (!Student.matchesSemester(studentSem, examSem)) {
            return ResponseEntity.status(403).body("Access Denied. This examination is not assigned to your semester.");
        }

        List<Question> questions = "paper".equals(type) ? 
            questionRepository.findByPaperId(id) : questionRepository.findByExamId(id);
        
        System.out.println("API Request - Type: " + type + ", ID: " + id + ", Questions Found: " + (questions != null ? questions.size() : 0));

        if (questions == null || questions.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        long seed = (enrollmentNo.hashCode() * 31L) + id;

        List<Map<String, Object>> result = new ArrayList<>();

        if (seed != 0) {
            // Group by dynamic questionGroup and preserve order of appearance initially
            List<String> originalSectionNames = new ArrayList<>();
            Map<String, List<Question>> groupedQuestions = new LinkedHashMap<>();
            
            for (Question q : questions) {
                String group = q.getQuestionGroup() != null ? q.getQuestionGroup() : "Q1";
                if (!groupedQuestions.containsKey(group)) {
                    originalSectionNames.add(group);
                }
                groupedQuestions.computeIfAbsent(group, k -> new ArrayList<>()).add(q);
            }
            
            List<List<Question>> sections = new ArrayList<>();
            for (String groupName : originalSectionNames) {
                sections.add(new ArrayList<>(groupedQuestions.get(groupName)));
            }
            
            // Shuffle sections (blocks) as a whole unit
            Random rand = new Random(seed);
            Collections.shuffle(sections, rand);
            
            // Map the shuffled questions to the original section names (fixed names, shifted content)
            for (int i = 0; i < sections.size(); i++) {
                String assignedSectionName = originalSectionNames.get(i);
                List<Question> sectionList = sections.get(i);
                
                for (Question q : sectionList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", q.getId());
                    map.put("text", q.getText());
                    map.put("marks", q.getMarks());
                    map.put("questionGroup", assignedSectionName); // Reassigned fixed section name
                    map.put("isOptional", q.isOptional());
                    
                    if ("paper".equals(type)) {
                        submissionRepository.findByStudentEnrollmentNoAndPaperId(enrollmentNo, id)
                            .flatMap(sub -> answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(sub.getId(), q.getId()))
                            .ifPresent(ans -> map.put("savedAnswer", ans.getStudentAnswer()));
                    } else {
                        examAttemptRepository.findByStudentEnrollmentNoAndExamId(enrollmentNo, id)
                            .flatMap(attempt -> answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attempt.getId(), q.getId()))
                            .ifPresent(ans -> map.put("savedAnswer", ans.getStudentAnswer()));
                    }
                    result.add(map);
                }
            }
        } else {
            // No seed or shuffle disabled - return original section names
            for (Question q : questions) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", q.getId());
                map.put("text", q.getText());
                map.put("marks", q.getMarks());
                map.put("questionGroup", q.getQuestionGroup());
                map.put("isOptional", q.isOptional());
                
                if ("paper".equals(type)) {
                    submissionRepository.findByStudentEnrollmentNoAndPaperId(enrollmentNo, id)
                        .flatMap(sub -> answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(sub.getId(), q.getId()))
                        .ifPresent(ans -> map.put("savedAnswer", ans.getStudentAnswer()));
                } else {
                    examAttemptRepository.findByStudentEnrollmentNoAndExamId(enrollmentNo, id)
                        .flatMap(attempt -> answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attempt.getId(), q.getId()))
                        .ifPresent(ans -> map.put("savedAnswer", ans.getStudentAnswer()));
                }
                result.add(map);
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/answer/save")
    public ResponseEntity<?> saveAnswer(@RequestBody Map<String, Object> payload, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Long attemptId = payload.get("attemptId") != null ? Long.valueOf(payload.get("attemptId").toString()) : null;
        Long submissionId = payload.get("submissionId") != null ? Long.valueOf(payload.get("submissionId").toString()) : null;
        Long questionId = payload.get("questionId") != null ? Long.valueOf(payload.get("questionId").toString()) : null;
        String answerText = payload.get("answer") != null ? payload.get("answer").toString() : "";

        // OR Question Logic: If this is an OR question and answer is not empty, ensure partner is cleared
        Question currentQuestion = questionRepository.findById(questionId).orElse(null);
        if (currentQuestion != null && currentQuestion.getPairId() != null && !currentQuestion.getPairId().isEmpty() && !answerText.trim().isEmpty()) {
            List<Question> partners = questionRepository.findByPairId(currentQuestion.getPairId());
            for (Question partner : partners) {
                if (!partner.getId().equals(questionId)) {
                    // Find and clear partner answer
                    if (attemptId != null) {
                        answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, partner.getId())
                            .ifPresent(ans -> {
                                ans.setStudentAnswer("");
                                ans.setUpdatedAt(LocalDateTime.now());
                                answerRepository.save(ans);
                            });
                    } else if (submissionId != null) {
                        answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(submissionId, partner.getId())
                            .ifPresent(ans -> {
                                ans.setStudentAnswer("");
                                ans.setUpdatedAt(LocalDateTime.now());
                                answerRepository.save(ans);
                            });
                    }
                }
            }
        }

        // Read tracking parameters
        Integer remainingSeconds = payload.get("remainingSeconds") != null ? Integer.valueOf(payload.get("remainingSeconds").toString()) : null;
        Integer currentSection = payload.get("currentSection") != null ? Integer.valueOf(payload.get("currentSection").toString()) : null;
        Long currentQuestionId = payload.get("currentQuestionId") != null ? Long.valueOf(payload.get("currentQuestionId").toString()) : null;

        Answer answer;
        if (attemptId != null) {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt == null || "Submitted".equals(attempt.getStatus())) return ResponseEntity.badRequest().body("Invalid attempt");
            
            if (remainingSeconds != null) attempt.setRemainingTimeSeconds(remainingSeconds);
            if (currentSection != null) attempt.setLastActiveSection(currentSection);
            if (currentQuestionId != null) attempt.setLastActiveQuestionId(currentQuestionId);
            attempt.setLastSavedAt(LocalDateTime.now());
            examAttemptRepository.save(attempt);

            Question question = questionRepository.findById(questionId).orElse(null);
            answer = answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, questionId)
                    .orElseGet(() -> {
                        Answer a = new Answer();
                        a.setExamAttempt(attempt);
                        a.setQuestion(question);
                        a.setQuestionText(question != null ? question.getText() : "Theory Answer");
                        a.setMaxMarks(question != null ? question.getMarks() : 0.0);
                        return a;
                    });
        } else if (submissionId != null) {
            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission == null || "Submitted".equals(submission.getStatus())) return ResponseEntity.badRequest().body("Invalid submission");
            
            if (remainingSeconds != null) submission.setRemainingTimeSeconds(remainingSeconds);
            if (currentSection != null) submission.setLastActiveSection(currentSection);
            if (currentQuestionId != null) submission.setLastActiveQuestionId(currentQuestionId);
            submission.setLastSavedAt(LocalDateTime.now());
            submissionRepository.save(submission);

            Question question = questionRepository.findById(questionId).orElse(null);
            answer = answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(submissionId, questionId)
                    .orElseGet(() -> {
                        Answer a = new Answer();
                        a.setSubmission(submission);
                        a.setQuestion(question);
                        a.setQuestionText(question != null ? question.getText() : "Paper Answer");
                        a.setMaxMarks(question != null ? question.getMarks() : 0.0);
                        return a;
                    });
        } else {
            return ResponseEntity.badRequest().body("Missing ID");
        }

        answer.setStudentAnswer(answerText);
        answer.setUpdatedAt(LocalDateTime.now());
        answerRepository.save(answer);

        return ResponseEntity.ok(Collections.singletonMap("status", "saved"));
    }

    @PostMapping("/exam/log-paste")
    public ResponseEntity<?> logPasteAttempt(@RequestBody Map<String, Object> payload, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Long questionId = payload.get("questionId") != null ? Long.valueOf(payload.get("questionId").toString()) : null;
        String pasteAttempt = payload.get("pasteAttempt") != null ? payload.get("pasteAttempt").toString() : "";
        String sourceType = payload.get("sourceType") != null ? payload.get("sourceType").toString() : "";

        ExamPasteLog log = new ExamPasteLog(enrollmentNo, questionId, LocalDateTime.now(), pasteAttempt, sourceType);
        examPasteLogRepository.save(log);

        return ResponseEntity.ok(Collections.singletonMap("status", "logged"));
    }

    @PostMapping("/exam/submit")
    public ResponseEntity<?> submitExam(@RequestBody Map<String, Object> payload, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return ResponseEntity.status(401).body("Unauthorized");

        Long attemptId = payload.get("attemptId") != null ? Long.valueOf(payload.get("attemptId").toString()) : null;
        Long submissionId = payload.get("submissionId") != null ? Long.valueOf(payload.get("submissionId").toString()) : null;

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";

        String examSem = "Semester 3";
        if (attemptId != null) {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt != null && attempt.getExam() != null) {
                examSem = attempt.getExam().getSemester();
            }
        } else if (submissionId != null) {
            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission != null && submission.getPaper() != null) {
                examSem = submission.getPaper().getSemester();
            }
        }

        if (!Student.matchesSemester(studentSem, examSem)) {
            return ResponseEntity.status(403).body("Access Denied. This examination is not assigned to your semester.");
        }

        if (attemptId != null) {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt != null && !"Submitted".equals(attempt.getStatus())) {
                // Auto-create missing/skipped Answers with 0 marks
                if (attempt.getExam() != null) {
                    List<Question> questions = questionRepository.findByExamId(attempt.getExam().getId());
                    List<Answer> existingAnswers = answerRepository.findByExamAttemptId(attemptId);
                    Set<Long> answeredQuestionIds = new java.util.HashSet<>();
                    if (existingAnswers != null) {
                        for (Answer ans : existingAnswers) {
                            if (ans.getQuestion() != null) {
                                answeredQuestionIds.add(ans.getQuestion().getId());
                            }
                        }
                    }
                    if (questions != null) {
                        for (Question q : questions) {
                            if (!answeredQuestionIds.contains(q.getId())) {
                                Answer ans = new Answer();
                                ans.setExamAttempt(attempt);
                                ans.setQuestion(q);
                                ans.setQuestionText(q.getText());
                                ans.setStudentAnswer("");
                                ans.setMaxMarks(q.getMarks() != null ? q.getMarks() : 0.0);
                                ans.setMarksObtained(0.0);
                                ans.setUpdatedAt(LocalDateTime.now());
                                answerRepository.save(ans);
                            }
                        }
                    }
                }
                
                attempt.setStatus("Submitted");
                attempt.setEndTime(LocalDateTime.now());
                examAttemptRepository.save(attempt);
                return ResponseEntity.ok(Collections.singletonMap("status", "submitted"));
            }
        } else if (submissionId != null) {
            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission != null && !"Submitted".equals(submission.getStatus())) {
                // Auto-create missing/skipped Answers with 0 marks
                if (submission.getPaper() != null) {
                    List<Question> questions = questionRepository.findByPaperId(submission.getPaper().getId());
                    List<Answer> existingAnswers = answerRepository.findBySubmissionId(submissionId);
                    Set<Long> answeredQuestionIds = new java.util.HashSet<>();
                    if (existingAnswers != null) {
                        for (Answer ans : existingAnswers) {
                            if (ans.getQuestion() != null) {
                                answeredQuestionIds.add(ans.getQuestion().getId());
                            }
                        }
                    }
                    if (questions != null) {
                        for (Question q : questions) {
                            if (!answeredQuestionIds.contains(q.getId())) {
                                Answer ans = new Answer();
                                ans.setSubmission(submission);
                                ans.setQuestion(q);
                                ans.setQuestionText(q.getText());
                                ans.setStudentAnswer("");
                                ans.setMaxMarks(q.getMarks() != null ? q.getMarks() : 0.0);
                                ans.setMarksObtained(0.0);
                                ans.setUpdatedAt(LocalDateTime.now());
                                answerRepository.save(ans);
                            }
                        }
                    }
                }

                submission.setStatus("Submitted");
                submission.setSubmittedAt(LocalDateTime.now());
                submissionRepository.save(submission);
                return ResponseEntity.ok(Collections.singletonMap("status", "submitted"));
            }
        }
        
        return ResponseEntity.badRequest().body("Invalid or already submitted");
    }
}
