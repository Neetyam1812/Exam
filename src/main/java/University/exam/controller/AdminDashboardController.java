package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.Paper;
import University.exam.Entity.Question;
import University.exam.Entity.Submission;
import University.exam.Entity.Answer;
import University.exam.Entity.Result;
import University.exam.Entity.Student;
import University.exam.repository.AdminRepository;
import University.exam.repository.PaperRepository;
import University.exam.repository.SubmissionRepository;
import University.exam.repository.AnswerRepository;
import University.exam.repository.ResultRepository;
import University.exam.repository.QuestionRepository;
import University.exam.repository.StudentRepository;
import University.exam.service.PaperParsingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PaperParsingService paperParsingService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private University.exam.repository.ExamRepository examRepository;

    // Helper method to simulate/retrieve a logged-in admin
    private void addAdminAttributes(HttpSession session, Model model) {
        String adminName = (String) session.getAttribute("loggedInAdmin");
        model.addAttribute("adminName", adminName != null ? adminName : "Super Admin");
        model.addAttribute("logoUrl", "/images/logo.png");
    }

    private Admin getLoggedInAdmin(HttpSession session) {
        if (session == null) {
            return null;
        }
        String adminName = (String) session.getAttribute("loggedInAdmin");
        if (adminName == null) {
            return null;
        }
        List<Admin> admins = adminRepository.findByAdminNameIgnoreCase(adminName.trim());
        if (admins != null && !admins.isEmpty()) {
            return admins.get(0);
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        addAdminAttributes(session, model);
        
        // Auto-terminate active exams whose duration has expired on page load
        List<Paper> papers = paperRepository.findByAdminId(admin.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean paperChanged = false;
        for (Paper paper : papers) {
            if ("ACTIVE".equals(paper.getExamStatus()) && paper.getActivationTime() != null && paper.getExamDuration() != null) {
                LocalDateTime endTime = paper.getActivationTime().plusMinutes(paper.getExamDuration());
                if (now.isAfter(endTime)) {
                    paper.setExamStatus("ENDED");
                    paperRepository.save(paper);
                    paperChanged = true;
                    
                    // Auto-terminate all ONGOING submissions for this paper!
                    terminateOngoingSubmissionsForPaper(paper);
                }
            }
        }
        if (paperChanged) {
            papers = paperRepository.findByAdminId(admin.getId());
        }
        // Sort by id descending to show latest first
        papers.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
        
        long totalPapers = paperRepository.countByAdminId(admin.getId());
        long totalSubmissions = submissionRepository.countByPaperAdminId(admin.getId());
        long pendingEvaluations = submissionRepository.countByStatusAndPaperAdminId("Pending", admin.getId());
        
        List<University.exam.Entity.Exam> exams = examRepository.findAll();
        exams.sort((e1, e2) -> e2.getId().compareTo(e1.getId()));

        // Calculate dynamic dashboard stats per paper (eligible students and submissions attempts)
        List<Student> allStudents = studentRepository.findAll();
        List<Submission> allSubmissions = submissionRepository.findByPaperAdminId(admin.getId());

        Map<Long, Long> eligibleStudentsMap = new HashMap<>();
        Map<Long, Long> attemptsMap = new HashMap<>();

        for (Paper p : papers) {
            String paperSem = p.getSemester();
            
            // Count eligible students matching this paper's semester
            long eligibleCount = 0;
            for (Student student : allStudents) {
                if (Student.matchesSemester(student.getSemester(), paperSem)) {
                    eligibleCount++;
                }
            }
            eligibleStudentsMap.put(p.getId(), eligibleCount);

            // Count attempts (submissions) for this paper
            long attemptsCount = 0;
            for (Submission sub : allSubmissions) {
                if (sub.getPaper() != null && sub.getPaper().getId().equals(p.getId())) {
                    attemptsCount++;
                }
            }
            attemptsMap.put(p.getId(), attemptsCount);
        }

        // Additional stats for the enhanced Result Management module
        long totalStudents = studentRepository.count();
        long passedCount = resultRepository.countByAdminIdAndResultStatus(admin.getId(), "PASSED");
        long failedCount = resultRepository.countByAdminIdAndResultStatus(admin.getId(), "FAILED");
        long terminatedCount = resultRepository.countByAdminIdAndResultStatus(admin.getId(), "TERMINATED");
        long disqualifiedCount = resultRepository.countByAdminIdAndResultStatus(admin.getId(), "DISQUALIFIED");
        long absentCount = resultRepository.countByAdminIdAndResultStatus(admin.getId(), "ABSENT");
        
        model.addAttribute("totalPapers", totalPapers);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("pendingEvaluations", pendingEvaluations);
        model.addAttribute("papers", papers);
        model.addAttribute("exams", exams);
        model.addAttribute("eligibleStudentsMap", eligibleStudentsMap);
        model.addAttribute("attemptsMap", attemptsMap);
        model.addAttribute("submissionsList", allSubmissions);

        model.addAttribute("totalStudentsCount", totalStudents);
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("terminatedCount", terminatedCount);
        model.addAttribute("disqualifiedCount", disqualifiedCount);
        model.addAttribute("absentCount", absentCount);
        
        return "admin/dashboard";
    }

    @GetMapping("/paper/{id}/activate")
    public String activatePaper(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        paperRepository.findById(id).ifPresent(paper -> {
            if (paper.getAdmin() != null && paper.getAdmin().getId().equals(admin.getId())) {
                LocalDateTime now = LocalDateTime.now();
                paper.setExamStatus("ACTIVE");
                paper.setActivationTime(now);
                paper.setActivatedTime(now);
                if (paper.getPublishedTime() == null) {
                    paper.setPublishedTime(now);
                }
                paperRepository.save(paper);
            }
        });
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/exam/{id}/activate")
    public String activateExam(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        examRepository.findById(id).ifPresent(exam -> {
            LocalDateTime now = LocalDateTime.now();
            exam.setExamStatus("ACTIVE");
            exam.setActivationTime(now);
            exam.setActivatedTime(now);
            if (exam.getPublishedTime() == null) {
                exam.setPublishedTime(now);
            }
            examRepository.save(exam);
        });
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/paper/{id}/end")
    @org.springframework.web.bind.annotation.ResponseBody
    public String endPaper(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "unauthorized";
        }
        Optional<Paper> paperOpt = paperRepository.findById(id);
        if (paperOpt.isPresent()) {
            Paper paper = paperOpt.get();
            if (paper.getAdmin() != null && paper.getAdmin().getId().equals(admin.getId())) {
                paper.setExamStatus("ENDED");
                paperRepository.save(paper);
                terminateOngoingSubmissionsForPaper(paper);
                return "success";
            }
        }
        return "failure";
    }

    @GetMapping("/paper/{id}/end-dashboard")
    public String endPaperDashboard(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        paperRepository.findById(id).ifPresent(paper -> {
            if (paper.getAdmin() != null && paper.getAdmin().getId().equals(admin.getId())) {
                paper.setExamStatus("ENDED");
                paperRepository.save(paper);
                terminateOngoingSubmissionsForPaper(paper);
            }
        });
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/exam/{id}/end")
    @org.springframework.web.bind.annotation.ResponseBody
    public String endExam(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "unauthorized";
        }
        examRepository.findById(id).ifPresent(exam -> {
            exam.setExamStatus("ENDED");
            examRepository.save(exam);
        });
        return "success";
    }

    @GetMapping("/upload-paper")
    public String uploadPaper(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        addAdminAttributes(session, model);
        return "admin/upload_paper";
    }

    @GetMapping("/submissions")
    public String viewSubmissions(
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "division", required = false) String division,
            HttpSession session,
            Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        addAdminAttributes(session, model);
        
        List<Submission> allSubmissions = submissionRepository.findByPaperAdminId(admin.getId());
        List<Submission> filteredSubmissions = new ArrayList<>();
        
        for (Submission sub : allSubmissions) {
            boolean matches = true;
            
            if (course != null && !course.trim().isEmpty()) {
                if (sub.getPaper() == null || !course.equalsIgnoreCase(sub.getPaper().getCourse())) {
                    matches = false;
                }
            }
            if (semester != null && !semester.trim().isEmpty()) {
                if (sub.getPaper() == null || !semester.equalsIgnoreCase(sub.getPaper().getSemester())) {
                    matches = false;
                }
            }
            if (division != null && !division.trim().isEmpty()) {
                if (sub.getStudent() == null || !division.equalsIgnoreCase(sub.getStudent().getDivision())) {
                    matches = false;
                }
            }
            
            if (matches) {
                filteredSubmissions.add(sub);
            }
        }
        
        model.addAttribute("submissions", filteredSubmissions);
        model.addAttribute("selectedCourse", course);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedDivision", division);
        
        return "admin/view_submissions";
    }

    @GetMapping("/evaluate")
    public String evaluatePaper(@RequestParam(value = "id", required = false) Long submissionId, HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        addAdminAttributes(session, model);
        if (submissionId != null) {
            Submission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission != null && submission.getPaper() != null && submission.getPaper().getAdmin() != null
                    && submission.getPaper().getAdmin().getId().equals(admin.getId())) {
                model.addAttribute("submission", submission);
                List<Answer> answers = answerRepository.findBySubmissionId(submissionId);
                
                // Sort answers by Question ID in ascending order to prevent shuffling
                if (answers != null) {
                    answers.sort(new Comparator<Answer>() {
                        @Override
                        public int compare(Answer a1, Answer a2) {
                            Long id1 = (a1.getQuestion() != null) ? a1.getQuestion().getId() : 0L;
                            Long id2 = (a2.getQuestion() != null) ? a2.getQuestion().getId() : 0L;
                            return id1.compareTo(id2);
                        }
                    });
                }
                
                // Group answers by question group/section
                Map<String, List<Answer>> groupedAnswers = new LinkedHashMap<>();
                for (Answer ans : answers) {
                    String group = "Q1"; // Default fallback section name
                    if (ans.getQuestion() != null && ans.getQuestion().getQuestionGroup() != null && !ans.getQuestion().getQuestionGroup().isEmpty()) {
                        group = ans.getQuestion().getQuestionGroup();
                    }
                    groupedAnswers.computeIfAbsent(group, k -> new ArrayList<>()).add(ans);
                }
                
                // Sort sections numerically (Q1, Q2, Q3...)
                Map<String, List<Answer>> sortedGroupedAnswers = new TreeMap<>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        try {
                            int n1 = Integer.parseInt(o1.replaceAll("\\D+", ""));
                            int n2 = Integer.parseInt(o2.replaceAll("\\D+", ""));
                            return Integer.compare(n1, n2);
                        } catch (Exception e) {
                            return o1.compareTo(o2);
                        }
                    }
                });
                sortedGroupedAnswers.putAll(groupedAnswers);
                
                model.addAttribute("groupedAnswers", sortedGroupedAnswers);
                model.addAttribute("answers", answers);
            } else {
                return "redirect:/admin/submissions?error=unauthorized";
            }
        }
        return "admin/evaluate_paper";
    }

    @PostMapping("/upload-paper")
    public String handleUploadPaper(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "manualContent", required = false) String manualContent,
            @RequestParam("subject") String subject,
            @RequestParam("course") String course,
            @RequestParam("semester") String semester,
            @RequestParam("duration") Integer duration,
            @RequestParam("totalMarks") Double totalMarks,
            HttpSession session) {

        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }

        try {
            Paper paper = new Paper();
            paper.setSubject(subject);  
            paper.setCourse(course);
            paper.setSemester(semester);
            paper.setUploadedAt(LocalDateTime.now());
            paper.setExamDuration(duration);
            paper.setTotalMarks(totalMarks);
            paper.setAdmin(admin);

            boolean isManual = (manualContent != null && !manualContent.trim().isEmpty());

            if (!isManual && (file == null || file.isEmpty())) {
                return "redirect:/admin/upload-paper?error=" + URLEncoder.encode("Please upload a paper file or enter the paper manually", StandardCharsets.UTF_8);
            }

            if (!isManual) {
                // Save the file locally to an external directory
                String uploadDir = "C:/uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String originalFilename = file.getOriginalFilename();
                String originalExtension = ".pdf"; // Default fallback
                if (originalFilename != null && originalFilename.lastIndexOf(".") > -1) {
                    originalExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                
                String safeFilename = UUID.randomUUID().toString() + originalExtension;
                String filePath = uploadDir + safeFilename;
                
                File destFile = new File(filePath);
                file.transferTo(destFile);

                paper.setFilePath("/uploads/" + safeFilename);
                paper = paperRepository.save(paper);

                // EXTRACT QUESTIONS
                try {
                    List<Question> questions = paperParsingService.parsePaper(destFile, paper);
                    if (!questions.isEmpty()) {
                        session.setAttribute("previewQuestions_" + paper.getId(), questions);
                        return "redirect:/admin/paper/" + paper.getId() + "/preview";
                    }
                } catch (Exception e) {
                    String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Extraction failed";
                    return "redirect:/admin/dashboard?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
                }
            } else {
                // Manual Entry
                paper.setManualContent(manualContent);
                paper = paperRepository.save(paper);

                // EXTRACT QUESTIONS FROM MANUAL CONTENT
                try {
                    List<Question> questions = paperParsingService.structureQuestions(manualContent, paper);
                    if (!questions.isEmpty()) {
                        session.setAttribute("previewQuestions_" + paper.getId(), questions);
                        return "redirect:/admin/paper/" + paper.getId() + "/preview";
                    }
                } catch (Exception e) {
                    String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Parsing failed";
                    return "redirect:/admin/dashboard?error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/admin/dashboard?error=" + URLEncoder.encode("File upload failed", StandardCharsets.UTF_8);
        }

        return "redirect:/admin/dashboard";
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/paper/{id}/preview")
    public String previewQuestions(@PathVariable("id") Long id, HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        Paper paper = paperRepository.findById(id).orElse(null);
        if (paper == null || paper.getAdmin() == null || !paper.getAdmin().getId().equals(admin.getId())) {
            return "redirect:/admin/dashboard?error=unauthorized";
        }

        addAdminAttributes(session, model);

        List<Question> questions = (List<Question>) session.getAttribute("previewQuestions_" + id);
        
        if (questions == null) {
            questions = questionRepository.findByPaperId(id);
            if(questions == null || questions.isEmpty()) {
                return "redirect:/admin/dashboard";
            }
        }

        model.addAttribute("paper", paper);
        model.addAttribute("questions", questions);
        return "admin/preview_questions";
    }

    @GetMapping("/paper/{id}/confirm-questions")
    public String confirmQuestionsGetFallback(@PathVariable("id") Long id, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        return "redirect:/admin/paper/" + id + "/preview";
    }

    @PostMapping("/paper/{id}/confirm-questions")
    public String confirmQuestions(@PathVariable("id") Long id, 
                                 @RequestParam Map<String, String> formData,
                                 HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        Paper paper = paperRepository.findById(id).orElse(null);
        if (paper == null || paper.getAdmin() == null || !paper.getAdmin().getId().equals(admin.getId())) {
            return "redirect:/admin/dashboard?error=unauthorized";
        }
        
        List<Question> finalQuestions = new ArrayList<>();
        int index = 0;
        while (formData.containsKey("q_" + index + "_text")) {
            Question q = new Question();
            q.setPaper(paper);
            q.setText(formData.get("q_" + index + "_text"));
            q.setMarks(Double.parseDouble(formData.getOrDefault("q_" + index + "_marks", "1.0")));
            q.setQuestionGroup(formData.getOrDefault("q_" + index + "_group", "Q1"));
            q.setOptional(formData.containsKey("q_" + index + "_optional"));
            q.setPairId(formData.get("q_" + index + "_pair_id"));
            finalQuestions.add(q);
            index++;
        }
        
        if (!finalQuestions.isEmpty()) {
            questionRepository.saveAll(finalQuestions);
        }
        LocalDateTime now = LocalDateTime.now();
        paper.setExamStatus("PUBLISHED");
        paper.setPublishedTime(now);
        paper.setActivatedTime(null);
        paper.setActivationTime(null);
        paperRepository.save(paper);
        session.removeAttribute("previewQuestions_" + id);

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/submit-evaluation")
    public String submitEvaluationGetFallback(HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        return "redirect:/admin/submissions";
    }

    @PostMapping("/submit-evaluation")
    public String handleSubmitEvaluation(@RequestParam("submissionId") Long submissionId, 
                                       @RequestParam Map<String, String> formData,
                                       HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission != null && submission.getPaper() != null && submission.getPaper().getAdmin() != null
                && submission.getPaper().getAdmin().getId().equals(admin.getId())) {
            List<Answer> answers = answerRepository.findBySubmissionId(submissionId);
            double totalObtained = 0;
            double totalMax = 0;
            Set<String> seenPairs = new HashSet<>();

            for (Answer ans : answers) {
                String marksStr = formData.get("marks_" + ans.getId());
                String feedback = formData.get("feedback_" + ans.getId());
                
                if (marksStr != null && !marksStr.trim().isEmpty()) {
                    double marks = Double.parseDouble(marksStr);
                    ans.setMarksObtained(marks);
                    ans.setFeedback(feedback);
                    answerRepository.save(ans);
                    totalObtained += marks;
                }

                Question q = ans.getQuestion();
                if (q != null && q.getPairId() != null && !q.getPairId().isEmpty()) {
                    if (!seenPairs.contains(q.getPairId())) {
                        totalMax += (ans.getMaxMarks() != null ? ans.getMaxMarks() : (q.getMarks() != null ? q.getMarks() : 0));
                        seenPairs.add(q.getPairId());
                    }
                } else {
                    totalMax += (ans.getMaxMarks() != null ? ans.getMaxMarks() : (q != null && q.getMarks() != null ? q.getMarks() : 0));
                }
            }

            double paperMaxMarks = (submission.getPaper() != null && submission.getPaper().getTotalMarks() != null)
                    ? submission.getPaper().getTotalMarks()
                    : totalMax;

            Result result = resultRepository.findBySubmissionId(submissionId)
                    .orElse(new Result());
            
            result.setSubmission(submission);
            result.setObtainedMarks(totalObtained);
            result.setTotalMarks(paperMaxMarks);
            result.setEvaluatedAt(LocalDateTime.now());
            
            // Set the new required fields
            if (submission.getStudent() != null) {
                result.setEnrollmentNo(submission.getStudent().getEnrollmentNo());
                result.setStudentName(submission.getStudent().getName());
                result.setSemester(submission.getStudent().getSemester());
                result.setDivision(submission.getStudent().getDivision());
            }
            if (submission.getPaper() != null) {
                result.setSubjectName(submission.getPaper().getSubject());
                // Attempt to retrieve an exam name, fallback to course or subject
                String examName = submission.getPaper().getCourse() != null ? submission.getPaper().getCourse() : "Mid/End Term Exam";
                result.setExamName(examName);
                
                // Fallback for semester if not set in student
                if (result.getSemester() == null || result.getSemester().isEmpty()) {
                    result.setSemester(submission.getPaper().getSemester());
                }
            }
            
            // Pass/Fail status: Pass if obtained marks >= 40% of paper max marks
            double passThreshold = paperMaxMarks * 0.40;
            if (totalObtained >= passThreshold) { 
                result.setResultStatus("PASS");
            } else {
                result.setResultStatus("FAIL");
            }
            
            resultRepository.save(result);

            submission.setStatus("Evaluated");
            submissionRepository.save(submission);
        } else {
            return "redirect:/admin/submissions?error=unauthorized";
        }

        return "redirect:/admin/submissions";
    }

    private void terminateOngoingSubmissionsForPaper(Paper paper) {
        List<Submission> submissions = submissionRepository.findByPaperId(paper.getId());
        if (submissions != null) {
            for (Submission sub : submissions) {
                if ("Ongoing".equals(sub.getStatus())) {
                    sub.setStatus("Terminated");
                    sub.setSubmittedAt(LocalDateTime.now());
                    submissionRepository.save(sub);

                    // Auto-create missing/skipped Answers with 0 marks
                    List<Question> questions = questionRepository.findByPaperId(paper.getId());
                    List<Answer> existingAnswers = answerRepository.findBySubmissionId(sub.getId());
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
                                ans.setSubmission(sub);
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

                    // Create Result record for terminated exam
                    Result result = resultRepository.findBySubmissionId(sub.getId()).orElse(new Result());
                    result.setSubmission(sub);
                    result.setObtainedMarks(0.0);
                    result.setTotalMarks(paper.getTotalMarks() != null ? paper.getTotalMarks() : 100.0);
                    result.setResultStatus("TERMINATED");
                    result.setPercentage(0.0);
                    result.setGrade("F");
                    result.setTerminationReason("Exam ended by administrator or recovery period expired");
                    result.setTerminatedAt(LocalDateTime.now());

                    if (sub.getStudent() != null) {
                        result.setEnrollmentNo(sub.getStudent().getEnrollmentNo());
                        result.setStudentName(sub.getStudent().getStudentName());
                        result.setDivision(sub.getStudent().getDivision());
                        result.setSemester(sub.getStudent().getSemester());
                        result.setRollNo(sub.getStudent().getRollNo());
                    }
                    result.setSubjectName(paper.getSubject());
                    result.setExamName(paper.getCourse() != null ? paper.getCourse() : "Theory Exam");

                    resultRepository.save(result);
                }
            }
            resultRepository.flush();
        }
    }

}
