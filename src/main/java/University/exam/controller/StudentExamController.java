package University.exam.controller;

import University.exam.Entity.*;
import University.exam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student/exam")
public class StudentExamController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private University.exam.service.SectionService sectionService;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ResultRepository resultRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Since we are mocking login during dev, fallback to a mock student if null
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) {
            enrollmentNo = "STU123";
            session.setAttribute("loggedInStudent", enrollmentNo);
            
            // Create mock student if doesn't exist
            if (studentRepository.findByEnrollmentNo(enrollmentNo).isEmpty()) {
                studentRepository.save(new Student(enrollmentNo, "password"));
            }
        }

        // Clear any previous exam session vars
        session.removeAttribute("currentExamId");
        session.removeAttribute("currentExamType");
        session.removeAttribute("currentAttemptId");

        // Instead of returning a missing template, redirect to the smart rules routing
        return "redirect:/student/rules";
    }

    @GetMapping("/rules/{id}")
    public String rules(@PathVariable("id") Long id, @RequestParam(name = "error", required = false) String error, HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) return "redirect:/login";

        Exam exam = examRepository.findById(id).orElse(null);
        if (exam == null) return "redirect:/student/rules";
        if ("ENDED".equals(exam.getExamStatus())) {
            return "redirect:/student/rules";
        }

        // Semester Access Control
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";
        if (!Student.matchesSemester(studentSem, exam.getSemester())) {
            try {
                return "redirect:/student/rules?error=" + java.net.URLEncoder.encode("Access Denied. This examination is not assigned to your semester.", java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/student/rules?error=access_denied";
            }
        }

        if (error != null) {
            if ("already_submitted".equals(error)) {
                model.addAttribute("error", "You have already completed and submitted this exam.");
            } else if ("terminated_violation".equals(error)) {
                model.addAttribute("error", "This exam session was automatically terminated due to a window switch or tab escape violation.");
            } else {
                model.addAttribute("error", error);
            }
        }

        model.addAttribute("exam", exam);
        return "student/rules";
    }

    @GetMapping("/start/{id}")
    public String startExam(@PathVariable("id") Long id, HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Exam exam = examRepository.findById(id).orElse(null);
        if (exam == null) return "redirect:/student/rules";

        // Semester Access Control
        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";
        if (!Student.matchesSemester(studentSem, exam.getSemester())) {
            try {
                return "redirect:/student/rules?error=" + java.net.URLEncoder.encode("Access Denied. This examination is not assigned to your semester.", java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/student/rules?error=access_denied";
            }
        }

        if (!"ACTIVE".equals(exam.getExamStatus()) && !"PUBLISHED".equals(exam.getExamStatus())) {
            return "redirect:/student/exam/rules/" + id + "?error=Exam is not active yet. Please wait for the administrator to start the exam.";
        }

        ExamAttempt attempt = examAttemptRepository.findByStudentEnrollmentNoAndExamId(enrollmentNo, id)
                .orElseGet(() -> {
                    ExamAttempt newAttempt = new ExamAttempt();
                    newAttempt.setStudent(student);
                    newAttempt.setExam(exam);
                    newAttempt.setStartTime(LocalDateTime.now());
                    newAttempt.setStatus("Ongoing");
                    return examAttemptRepository.save(newAttempt);
                });

        if ("Submitted".equals(attempt.getStatus())) {
            return "redirect:/student/exam/rules/" + id + "?error=already_submitted";
        }
        if ("Terminated".equals(attempt.getStatus())) {
            return "redirect:/student/exam/rules/" + id + "?error=terminated_violation";
        }

        model.addAttribute("exam", exam);
        model.addAttribute("student", student);
        model.addAttribute("attemptId", attempt.getId());
        model.addAttribute("type", "exam");
        
        long elapsedSeconds = java.time.Duration.between(attempt.getStartTime(), LocalDateTime.now()).getSeconds();
        long totalSeconds = exam.getExamDuration() != null ? exam.getExamDuration() * 60 : 3600;
        long remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
        
        session.setAttribute("currentExamId", id);
        session.setAttribute("currentExamType", "exam");
        session.setAttribute("currentAttemptId", attempt.getId());

        return "redirect:/student/exam/section/0";
    }

    // View: Rules for PDF-based paper
    @GetMapping("/paper-rules/{id}")
    public String paperRules(@PathVariable("id") Long id, @RequestParam(name = "error", required = false) String error, HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") == null) return "redirect:/login";

        Paper paper = paperRepository.findById(id).orElse(null);
        if (paper == null) return "redirect:/student/rules";
        if ("ENDED".equals(paper.getExamStatus())) {
            return "redirect:/student/rules";
        }

        // Semester Access Control
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";
        if (!Student.matchesSemester(studentSem, paper.getSemester())) {
            try {
                return "redirect:/student/rules?error=" + java.net.URLEncoder.encode("Access Denied. This examination is not assigned to your semester.", java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/student/rules?error=access_denied";
            }
        }
        
        List<Question> questions = questionRepository.findByPaperId(id);
        if (questions == null || questions.isEmpty()) {
            return "redirect:/student/rules?error=Exam is not available yet";
        }

        if (error != null) {
            if ("already_submitted".equals(error)) {
                model.addAttribute("error", "You have already completed and submitted this exam.");
            } else if ("terminated_violation".equals(error)) {
                model.addAttribute("error", "This exam session was automatically terminated due to a window switch or tab escape violation.");
            } else {
                model.addAttribute("error", error);
            }
        }

        model.addAttribute("paper", paper);
        return "student/paper_rules";
    }

    // View: Start PDF-based exam (writing answers while viewing PDF)
    @GetMapping("/confirm-paper/{id}")
    public String confirmPaperExam(@PathVariable("id") Long id, HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("enrollment_no");
        if (enrollmentNo == null) {
            enrollmentNo = (String) session.getAttribute("loggedInStudent");
        }
        if (enrollmentNo == null) return "redirect:/login";

        Paper paper = paperRepository.findById(id).orElse(null);
        if (paper == null) return "redirect:/student/rules";

        // Semester Access Control
        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";
        if (!Student.matchesSemester(studentSem, paper.getSemester())) {
            try {
                return "redirect:/student/rules?error=" + java.net.URLEncoder.encode("Access Denied. This examination is not assigned to your semester.", java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/student/rules?error=access_denied";
            }
        }

        if (!"ACTIVE".equals(paper.getExamStatus()) && !"PUBLISHED".equals(paper.getExamStatus())) {
            return "redirect:/student/exam/paper-rules/" + id + "?error=Exam is not active yet. Please wait for the administrator to start the exam.";
        }

        if (student == null) {
            throw new RuntimeException("Student not found for enrollment: " + enrollmentNo);
        }

        // Debug prints to verify DB data
        System.out.println("DEBUG: Student Name: " + student.getStudentName());
        System.out.println("DEBUG: Division: " + student.getDivision());

        model.addAttribute("paper", paper);
        model.addAttribute("student", student);
        
        return "student/confirm_paper";
    }

    @GetMapping("/start-paper/{id}")
    public String startPaperExam(@PathVariable("id") Long id, HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Paper paper = paperRepository.findById(id).orElse(null);
        if (paper == null) return "redirect:/student/rules";

        // Semester Access Control
        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";
        if (!Student.matchesSemester(studentSem, paper.getSemester())) {
            try {
                return "redirect:/student/rules?error=" + java.net.URLEncoder.encode("Access Denied. This examination is not assigned to your semester.", java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/student/rules?error=access_denied";
            }
        }

        if (!"ACTIVE".equals(paper.getExamStatus()) && !"PUBLISHED".equals(paper.getExamStatus())) {
            return "redirect:/student/exam/paper-rules/" + id + "?error=Exam is not active yet. Please wait for the administrator to start the exam.";
        }

        List<Question> questions = questionRepository.findByPaperId(id);
        if (questions == null || questions.isEmpty()) {
            return "redirect:/student/rules?error=Exam is not available yet";
        }

        // Use specific repository method for better performance
        Submission submission = submissionRepository.findByStudentEnrollmentNoAndPaperId(enrollmentNo, id)
                .orElseGet(() -> {
                    Submission s = new Submission();
                    s.setStudent(student);
                    s.setPaper(paper);
                    s.setSubmittedAt(LocalDateTime.now()); // Acts as startTime here
                    s.setStatus("Ongoing");
                    return submissionRepository.save(s);
                });

        System.out.println("Starting Paper Exam - Student: " + enrollmentNo + ", Paper ID: " + id + ", Submission ID: " + submission.getId());

        if ("Submitted".equals(submission.getStatus())) {
            return "redirect:/student/exam/paper-rules/" + id + "?error=already_submitted";
        }
        if ("Terminated".equals(submission.getStatus())) {
            return "redirect:/student/exam/paper-rules/" + id + "?error=terminated_violation";
        }

        model.addAttribute("questions", questions);
        model.addAttribute("paper", paper);
        model.addAttribute("exam", paper); // Use 'exam' key to avoid changing template logic
        model.addAttribute("student", student); // Expose student info to Thymeleaf
        model.addAttribute("attemptId", submission.getId()); // Use 'attemptId' key for template
        model.addAttribute("type", "paper");
        
        long elapsedSeconds = java.time.Duration.between(submission.getSubmittedAt(), LocalDateTime.now()).getSeconds();
        long totalSeconds = (paper.getExamDuration() != null ? paper.getExamDuration() : 120) * 60;
        long remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
        
        session.setAttribute("currentExamId", id);
        session.setAttribute("currentExamType", "paper");
        session.setAttribute("currentAttemptId", submission.getId());

        return "redirect:/student/exam/section/0";
    }

    @GetMapping("/section/{index}")
    public String loadSection(@PathVariable("index") int index, HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Long examId = (Long) session.getAttribute("currentExamId");
        String type = (String) session.getAttribute("currentExamType");
        Long attemptId = (Long) session.getAttribute("currentAttemptId");

        if (examId == null || type == null || attemptId == null) {
            return "redirect:/student/rules";
        }

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        model.addAttribute("student", student);
        model.addAttribute("type", type);
        model.addAttribute("attemptId", attemptId);

        List<University.exam.dto.Section> sections;
        long remainingSeconds = 0;
        
        if ("paper".equals(type)) {
            Paper paper = paperRepository.findById(examId).orElse(null);
            if (paper == null) return "redirect:/student/rules";
            if (!"ACTIVE".equals(paper.getExamStatus()) && !"PUBLISHED".equals(paper.getExamStatus())) {
                return "redirect:/student/exam/paper-rules/" + examId + "?error=Exam is not active yet.";
            }
            sections = sectionService.getSectionsByPaperId(examId);
            model.addAttribute("exam", paper);

            
            Submission submission = submissionRepository.findById(attemptId).orElse(null);
            if (submission != null) {
                if ("Submitted".equals(submission.getStatus())) {
                    return "redirect:/student/exam/paper-rules/" + examId + "?error=already_submitted";
                }
                if ("Terminated".equals(submission.getStatus())) {
                    return "redirect:/student/exam/paper-rules/" + examId + "?error=terminated_violation";
                }
                long elapsedSeconds = java.time.Duration.between(submission.getSubmittedAt(), LocalDateTime.now()).getSeconds();
                long totalSeconds = (paper.getExamDuration() != null ? paper.getExamDuration() : 120) * 60;
                remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
            }
        } else {
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam == null) return "redirect:/student/rules";
            if (!"ACTIVE".equals(exam.getExamStatus()) && !"PUBLISHED".equals(exam.getExamStatus())) {
                return "redirect:/student/exam/rules/" + examId + "?error=Exam is not active yet.";
            }
            sections = sectionService.getSectionsByExamId(examId);
            model.addAttribute("exam", exam);

            
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt != null) {
                if ("Submitted".equals(attempt.getStatus())) {
                    return "redirect:/student/exam/rules/" + examId + "?error=already_submitted";
                }
                if ("Terminated".equals(attempt.getStatus())) {
                    return "redirect:/student/exam/rules/" + examId + "?error=terminated_violation";
                }
                long elapsedSeconds = java.time.Duration.between(attempt.getStartTime(), LocalDateTime.now()).getSeconds();
                long totalSeconds = exam.getExamDuration() != null ? exam.getExamDuration() * 60 : 3600;
                remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
            }
        }

        // Apply Section-Level Shuffling with fixed naming per deterministic seed (studentId + examId)
        if (sections != null && !sections.isEmpty() && enrollmentNo != null && examId != null) {
            java.util.List<String> originalNames = new java.util.ArrayList<>();
            for (University.exam.dto.Section s : sections) {
                originalNames.add(s.getName());
            }

            long seed = (enrollmentNo.hashCode() * 31L) + examId;
            java.util.Random rand = new java.util.Random(seed);
            java.util.Collections.shuffle(sections, rand);

            for (int i = 0; i < sections.size(); i++) {
                sections.get(i).setName(originalNames.get(i));
            }
        }

        if (index < 0 || index >= sections.size()) {
            return "redirect:/student/rules";
        }

        University.exam.dto.Section currentSection = sections.get(index);
        
        java.util.Map<Long, String> savedAnswers = new java.util.HashMap<>();
        for (Question q : currentSection.getQuestions()) {
            if ("paper".equals(type)) {
                answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, q.getId())
                    .ifPresent(ans -> savedAnswers.put(q.getId(), ans.getStudentAnswer()));
            } else {
                answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, q.getId())
                    .ifPresent(ans -> savedAnswers.put(q.getId(), ans.getStudentAnswer()));
            }
        }

        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("section", currentSection);
        model.addAttribute("currentIndex", index);
        model.addAttribute("totalPages", sections.size());
        model.addAttribute("savedAnswers", savedAnswers);

        return "student/exam_section";
    }

    @GetMapping("/feedback")
    public String feedbackForm(HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Long examId = (Long) session.getAttribute("currentExamId");
        String type = (String) session.getAttribute("currentExamType");

        if (examId == null || type == null) {
            return "redirect:/student/rules";
        }

        // Check if feedback already submitted
        boolean alreadySubmitted = feedbackRepository.findByStudentEnrollmentNoAndExamIdAndExamType(enrollmentNo, examId, type).isPresent();
        if (alreadySubmitted) {
            return "redirect:/student/exam/result-summary";
        }

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        model.addAttribute("student", student);
        
        String examName = "Theory Examination";
        if ("paper".equals(type)) {
            Paper paper = paperRepository.findById(examId).orElse(null);
            if (paper != null) examName = paper.getSubject();
        } else {
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam != null) examName = exam.getExamName() != null ? exam.getExamName() : exam.getSubject();
        }
        
        model.addAttribute("examName", examName);
        model.addAttribute("examId", examId);
        model.addAttribute("examType", type);

        return "student/feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam("rating") Integer rating,
            @RequestParam(name = "comments", required = false) String comments,
            @RequestParam(name = "systemEasyToUse", required = false) String systemEasyToUse,
            @RequestParam(name = "paperClear", required = false) String paperClear,
            @RequestParam(name = "technicalIssues", required = false) String technicalIssues,
            HttpSession session) {
            
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Long examId = (Long) session.getAttribute("currentExamId");
        String type = (String) session.getAttribute("currentExamType");

        if (examId == null || type == null) {
            return "redirect:/student/rules";
        }

        boolean alreadySubmitted = feedbackRepository.findByStudentEnrollmentNoAndExamIdAndExamType(enrollmentNo, examId, type).isPresent();
        if (alreadySubmitted) {
            return "redirect:/student/exam/result-summary";
        }

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        if (student != null) {
            Feedback feedback = new Feedback();
            feedback.setStudent(student);
            feedback.setExamId(examId);
            feedback.setExamType(type);
            feedback.setRating(rating);
            feedback.setComments(comments);
            feedback.setSystemEasyToUse(systemEasyToUse);
            feedback.setPaperClear(paperClear);
            feedback.setTechnicalIssues(technicalIssues);
            feedback.setSubmittedAt(java.time.LocalDateTime.now());
            
            feedbackRepository.save(feedback);
        }

        return "redirect:/student/exam/result-summary";
    }

    @GetMapping("/result-summary")
    public String resultSummary(HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        Long examId = (Long) session.getAttribute("currentExamId");
        String type = (String) session.getAttribute("currentExamType");
        Long attemptId = (Long) session.getAttribute("currentAttemptId");

        if (examId == null || type == null || attemptId == null) {
            return "redirect:/student/rules";
        }

        // Verify feedback is submitted to prevent skipping feedback
        boolean feedbackSubmitted = feedbackRepository.findByStudentEnrollmentNoAndExamIdAndExamType(enrollmentNo, examId, type).isPresent();
        if (!feedbackSubmitted) {
            return "redirect:/student/exam/feedback";
        }

        int totalQuestions = 0;
        int attemptedQuestions = 0;
        
        List<Question> questions = "paper".equals(type) ? questionRepository.findByPaperId(examId) : questionRepository.findByExamId(examId);
        
        if (questions != null) {
            totalQuestions = questions.size();
            for (Question q : questions) {
                Answer ans = null;
                if ("paper".equals(type)) {
                    ans = answerRepository.findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, q.getId()).orElse(null);
                } else {
                    ans = answerRepository.findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(attemptId, q.getId()).orElse(null);
                }
                
                if (ans != null && ans.getStudentAnswer() != null && !ans.getStudentAnswer().trim().isEmpty()) {
                    attemptedQuestions++;
                }
            }
        }

        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("attemptedQuestions", attemptedQuestions);
        model.addAttribute("unattemptedQuestions", totalQuestions - attemptedQuestions);
        
        return "student/result_summary";
    }

    @PostMapping("/auto-submit")
    @ResponseBody
    public ResponseEntity<?> autoSubmitExam(HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Long examId = (Long) session.getAttribute("currentExamId");
        String type = (String) session.getAttribute("currentExamType");
        Long attemptId = (Long) session.getAttribute("currentAttemptId");

        System.out.println("Auto-submit requested. Student: " + enrollmentNo + ", Type: " + type + ", AttemptId: " + attemptId);

        if (examId == null || type == null || attemptId == null) {
            return ResponseEntity.badRequest().body("No active exam session found");
        }

        if ("paper".equals(type)) {
            Submission submission = submissionRepository.findById(attemptId).orElse(null);
            if (submission != null && !"Submitted".equals(submission.getStatus())) {
                // Auto-create missing/skipped Answers with 0 marks
                if (submission.getPaper() != null) {
                    List<Question> questions = questionRepository.findByPaperId(submission.getPaper().getId());
                    List<Answer> existingAnswers = answerRepository.findBySubmissionId(submission.getId());
                    java.util.Set<Long> answeredQuestionIds = new java.util.HashSet<>();
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

                submission.setStatus("Terminated");
                submission.setSubmittedAt(LocalDateTime.now());
                submissionRepository.save(submission);
                System.out.println("Submission status marked as Terminated: " + attemptId);

                // Auto-create Result record for terminated paper exam
                Result result = resultRepository.findBySubmissionId(submission.getId()).orElse(new Result());
                result.setSubmission(submission);
                result.setObtainedMarks(0.0);
                result.setTotalMarks(submission.getPaper() != null && submission.getPaper().getTotalMarks() != null ? submission.getPaper().getTotalMarks() : 100.0);
                result.setResultStatus("TERMINATED");
                result.setPercentage(0.0);
                result.setGrade("F");
                result.setTerminationReason("Proctoring violation (tab switch or blur) during exam");
                result.setTerminatedAt(LocalDateTime.now());
                
                if (submission.getStudent() != null) {
                    result.setEnrollmentNo(submission.getStudent().getEnrollmentNo());
                    result.setStudentName(submission.getStudent().getStudentName());
                    result.setDivision(submission.getStudent().getDivision());
                    result.setSemester(submission.getStudent().getSemester());
                    result.setRollNo(submission.getStudent().getRollNo());
                }
                if (submission.getPaper() != null) {
                    result.setSubjectName(submission.getPaper().getSubject());
                    result.setExamName(submission.getPaper().getCourse() != null ? submission.getPaper().getCourse() : "Theory Exam");
                }
                resultRepository.saveAndFlush(result);
            }
        } else {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt != null && !"Submitted".equals(attempt.getStatus())) {
                // Auto-create missing/skipped Answers with 0 marks
                if (attempt.getExam() != null) {
                    List<Question> questions = questionRepository.findByExamId(attempt.getExam().getId());
                    List<Answer> existingAnswers = answerRepository.findByExamAttemptId(attempt.getId());
                    java.util.Set<Long> answeredQuestionIds = new java.util.HashSet<>();
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

                attempt.setStatus("Terminated");
                attempt.setEndTime(LocalDateTime.now());
                examAttemptRepository.save(attempt);
                System.out.println("ExamAttempt status marked as Terminated: " + attemptId);
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/terminated")
    public String terminated(HttpSession session, Model model) {
        // Clear current exam session details since it's terminated
        session.removeAttribute("currentExamId");
        session.removeAttribute("currentExamType");
        session.removeAttribute("currentAttemptId");
        return "student/terminated";
    }

    @GetMapping("/resume")
    public String resumeExam(HttpSession session, Model model) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        // Check if there is an ongoing ExamAttempt
        java.util.List<ExamAttempt> attempts = examAttemptRepository.findByStudentEnrollmentNo(enrollmentNo);
        ExamAttempt ongoingAttempt = null;
        if (attempts != null) {
            for (ExamAttempt attempt : attempts) {
                if ("Ongoing".equals(attempt.getStatus())) {
                    ongoingAttempt = attempt;
                    break;
                }
            }
        }

        // Check if there is an ongoing Submission
        java.util.List<Submission> submissions = submissionRepository.findByStudentEnrollmentNo(enrollmentNo);
        Submission ongoingSubmission = null;
        if (submissions != null) {
            for (Submission sub : submissions) {
                if ("Ongoing".equals(sub.getStatus())) {
                    ongoingSubmission = sub;
                    break;
                }
            }
        }

        if (ongoingAttempt == null && ongoingSubmission == null) {
            return "redirect:/student/rules";
        }

        String examName = "";
        String lastSaved = "N/A";
        long remainingSeconds = 0;
        Long attemptId = null;
        String type = "";

        if (ongoingAttempt != null) {
            examName = ongoingAttempt.getExam().getExamName() != null ? ongoingAttempt.getExam().getExamName() : ongoingAttempt.getExam().getSubject();
            if (ongoingAttempt.getLastSavedAt() != null) {
                lastSaved = ongoingAttempt.getLastSavedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                lastSaved = ongoingAttempt.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            remainingSeconds = ongoingAttempt.getRemainingTimeSeconds() != null ? ongoingAttempt.getRemainingTimeSeconds() : (ongoingAttempt.getExam().getExamDuration() * 60);
            attemptId = ongoingAttempt.getId();
            type = "exam";
            
            // Mark attempt as paused if not already
            if (!Boolean.TRUE.equals(ongoingAttempt.getIsPaused())) {
                ongoingAttempt.setIsPaused(true);
                ongoingAttempt.setPausedAt(LocalDateTime.now());
                ongoingAttempt.setPauseCount((ongoingAttempt.getPauseCount() != null ? ongoingAttempt.getPauseCount() : 0) + 1);
                examAttemptRepository.save(ongoingAttempt);
            }
        } else {
            examName = ongoingSubmission.getPaper().getSubject();
            if (ongoingSubmission.getLastSavedAt() != null) {
                lastSaved = ongoingSubmission.getLastSavedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                lastSaved = ongoingSubmission.getSubmittedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            remainingSeconds = ongoingSubmission.getRemainingTimeSeconds() != null ? ongoingSubmission.getRemainingTimeSeconds() : (ongoingSubmission.getPaper().getExamDuration() * 60);
            attemptId = ongoingSubmission.getId();
            type = "paper";

            // Mark submission as paused if not already
            if (!Boolean.TRUE.equals(ongoingSubmission.getIsPaused())) {
                ongoingSubmission.setIsPaused(true);
                ongoingSubmission.setPausedAt(LocalDateTime.now());
                ongoingSubmission.setPauseCount((ongoingSubmission.getPauseCount() != null ? ongoingSubmission.getPauseCount() : 0) + 1);
                submissionRepository.save(ongoingSubmission);
            }
        }

        long hrs = remainingSeconds / 3600;
        long mins = (remainingSeconds % 3600) / 60;
        long secs = remainingSeconds % 60;
        String remainingTimeFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs);

        model.addAttribute("examName", examName);
        model.addAttribute("lastSaved", lastSaved);
        model.addAttribute("remainingTimeFormatted", remainingTimeFormatted);
        model.addAttribute("attemptId", attemptId);
        model.addAttribute("type", type);

        return "student/resume";
    }

    @PostMapping("/resume")
    public String performResume(
            @RequestParam("attemptId") Long attemptId,
            @RequestParam("type") String type,
            @RequestParam("reason") String reason,
            HttpSession session) {
        
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) return "redirect:/login";

        if ("exam".equals(type)) {
            ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElse(null);
            if (attempt != null && "Ongoing".equals(attempt.getStatus())) {
                long totalSeconds = attempt.getExam().getExamDuration() != null ? attempt.getExam().getExamDuration() * 60 : 3600;
                long remaining = attempt.getRemainingTimeSeconds() != null ? attempt.getRemainingTimeSeconds() : totalSeconds;
                
                attempt.setStartTime(LocalDateTime.now().minusSeconds(totalSeconds - remaining));
                attempt.setIsPaused(false);
                attempt.setResumedAt(LocalDateTime.now());
                attempt.setResumeCount((attempt.getResumeCount() != null ? attempt.getResumeCount() : 0) + 1);
                attempt.setInterruptionReason(reason);
                examAttemptRepository.save(attempt);

                session.setAttribute("currentExamId", attempt.getExam().getId());
                session.setAttribute("currentExamType", "exam");
                session.setAttribute("currentAttemptId", attempt.getId());

                int lastSec = attempt.getLastActiveSection() != null ? attempt.getLastActiveSection() : 0;
                return "redirect:/student/exam/section/" + lastSec;
            }
        } else {
            Submission submission = submissionRepository.findById(attemptId).orElse(null);
            if (submission != null && "Ongoing".equals(submission.getStatus())) {
                long totalSeconds = (submission.getPaper().getExamDuration() != null ? submission.getPaper().getExamDuration() : 120) * 60;
                long remaining = submission.getRemainingTimeSeconds() != null ? submission.getRemainingTimeSeconds() : totalSeconds;

                submission.setSubmittedAt(LocalDateTime.now().minusSeconds(totalSeconds - remaining));
                submission.setIsPaused(false);
                submission.setResumedAt(LocalDateTime.now());
                submission.setResumeCount((submission.getResumeCount() != null ? submission.getResumeCount() : 0) + 1);
                submission.setInterruptionReason(reason);
                submissionRepository.save(submission);

                session.setAttribute("currentExamId", submission.getPaper().getId());
                session.setAttribute("currentExamType", "paper");
                session.setAttribute("currentAttemptId", submission.getId());

                int lastSec = submission.getLastActiveSection() != null ? submission.getLastActiveSection() : 0;
                return "redirect:/student/exam/section/" + lastSec;
            }
        }

        return "redirect:/student/rules";
    }
}
