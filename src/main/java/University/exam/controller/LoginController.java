package University.exam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.PaperRepository paperRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.StudentRepository studentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.ExamRepository examRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.StudentActiveSessionRepository studentActiveSessionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.AdminRepository adminRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.QuestionRepository questionRepository;

    @GetMapping("/")
    public String login() {
        return "auth/student_login";
    }

    @GetMapping("/login")
    public String loginGetFallback() {
        return "redirect:/";
    }

    @org.springframework.web.bind.annotation.PostMapping("/login")
    public String performLogin(String enrollmentNo, String password, jakarta.servlet.http.HttpSession session) {
        if (enrollmentNo == null || password == null) {
            return "redirect:/?error=invalid_credentials";
        }
        String trimmedEnrollment = enrollmentNo.trim();
        String trimmedPassword = password.trim();

        if (trimmedEnrollment.isEmpty() || trimmedPassword.isEmpty()) {
            return "redirect:/?error=invalid_credentials";
        }

        java.util.Optional<University.exam.Entity.Student> studentOpt = studentRepository.findByEnrollmentNo(trimmedEnrollment);
        if (studentOpt.isEmpty()) {
            return "redirect:/?error=invalid_credentials";
        }
        University.exam.Entity.Student student = studentOpt.get();
        String dbPassword = student.getPassword();
        if (dbPassword == null || (!dbPassword.equals(password) && !dbPassword.equals(trimmedPassword))) {
            return "redirect:/?error=invalid_credentials";
        }

        // Enforce single active session per student account
        java.util.Optional<University.exam.Entity.StudentActiveSession> activeSessionOpt = 
            studentActiveSessionRepository.findByStudentIdAndIsActiveTrue(trimmedEnrollment);
             
        if (activeSessionOpt.isPresent()) {
            University.exam.Entity.StudentActiveSession activeSession = activeSessionOpt.get();
            // If there's an active session under a different session ID, check if it's stale
            if (!activeSession.getSessionId().equals(session.getId())) {
                java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(3);
                if (activeSession.getLastActivity() != null && activeSession.getLastActivity().isBefore(cutoff)) {
                    // Stale session: delete it and flush to allow the new login
                    studentActiveSessionRepository.delete(activeSession);
                    studentActiveSessionRepository.flush();
                } else {
                    // Active session: block access
                    return "redirect:/?error=already_logged_in";
                }
            }
        }

        // Clean up any existing session record with the same session ID to prevent unique constraint violation
        studentActiveSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
            studentActiveSessionRepository.delete(s);
            studentActiveSessionRepository.flush();
        });

        // Clean up any orphaned session records for this student before starting a new one
        java.util.List<University.exam.Entity.StudentActiveSession> existing = studentActiveSessionRepository.findByStudentId(trimmedEnrollment);
        if (existing != null && !existing.isEmpty()) {
            studentActiveSessionRepository.deleteAll(existing);
            studentActiveSessionRepository.flush();
        }

        // Register the new active session
        University.exam.Entity.StudentActiveSession newSession = new University.exam.Entity.StudentActiveSession(trimmedEnrollment, session.getId());
        studentActiveSessionRepository.save(newSession);

        // Mock authentication
        session.setAttribute("loggedInStudent", trimmedEnrollment);
        session.setAttribute("enrollment_no", trimmedEnrollment);
        

        
        // Redirect to smart routing page which validates semester and redirects appropriately
        return "redirect:/student/rules";
    }

    @GetMapping("/student/logout")
    public String studentLogout(jakarta.servlet.http.HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

 
    @GetMapping("/student/rules")
    public String genericRules(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model, @org.springframework.web.bind.annotation.RequestParam(name = "error", required = false) String error) {
        if (session.getAttribute("loggedInStudent") == null) return "redirect:/";

        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        University.exam.Entity.Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        String studentSem = student != null ? student.getSemester() : "Semester 3";

        // Check if admin has uploaded a paper now (filtered by semester)
        java.util.List<University.exam.Entity.Paper> papers = paperRepository.findAll();
        java.util.List<University.exam.Entity.Paper> matchingPapers = new java.util.ArrayList<>();
        if (papers != null) {
            for (University.exam.Entity.Paper p : papers) {
                if (University.exam.Entity.Student.matchesSemester(studentSem, p.getSemester()) && !"ENDED".equals(p.getExamStatus())) {
                    // Avoid redirect loops by making sure the paper has questions
                    java.util.List<University.exam.Entity.Question> questions = questionRepository.findByPaperId(p.getId());
                    if (questions != null && !questions.isEmpty()) {
                        matchingPapers.add(p);
                    }
                }
            }
        }

        if (!matchingPapers.isEmpty()) {
            matchingPapers.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
            return "redirect:/student/exam/paper-rules/" + matchingPapers.get(0).getId();
        }

        // Fallback: If no papers, find the latest traditional exam matching their semester
        java.util.List<University.exam.Entity.Exam> exams = examRepository.findAll();
        java.util.List<University.exam.Entity.Exam> matchingExams = new java.util.ArrayList<>();
        if (exams != null) {
            for (University.exam.Entity.Exam e : exams) {
                if (University.exam.Entity.Student.matchesSemester(studentSem, e.getSemester()) && !"ENDED".equals(e.getExamStatus())) {
                    // Avoid redirect loops by making sure the exam has questions
                    java.util.List<University.exam.Entity.Question> questions = questionRepository.findByExamId(e.getId());
                    if (questions != null && !questions.isEmpty()) {
                        matchingExams.add(e);
                    }
                }
            }
        }

        if (!matchingExams.isEmpty()) {
            matchingExams.sort((e1, e2) -> e2.getId().compareTo(e1.getId()));
            return "redirect:/student/exam/rules/" + matchingExams.get(0).getId();
        }

        // Create a mock exam so the rules.html template doesn't crash
        University.exam.Entity.Exam mockExam = new University.exam.Entity.Exam();
        mockExam.setId(0L); // Use 0 to indicate it's a mock
        mockExam.setExamName("Waiting for Exam...");
        mockExam.setSubject("Please wait for the admin to upload the paper.");
        mockExam.setExamDuration(120);
        mockExam.setTotalMarks(100.0);
        
        if (error != null) {
            model.addAttribute("error", error);
        }
        
        model.addAttribute("exam", mockExam);
        model.addAttribute("isFallback", true);
        return "student/rules";
    }

    @GetMapping("/student/rules/start")
    public String startFallbackExam(jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("loggedInStudent") == null) return "redirect:/";

        // Check if a paper has been uploaded
        java.util.List<University.exam.Entity.Paper> papers = paperRepository.findAll();
        if (papers != null && !papers.isEmpty()) {
            papers.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
            return "redirect:/student/exam/confirm-paper/" + papers.get(0).getId();
        }

        // If still no paper, redirect back with error
        return "redirect:/student/rules?error=Exam is not available yet. Please wait.";
    }

    @GetMapping("/admin-login")
    public String adminLogin() {
        return "auth/admin_login";
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin-login")
    public String performAdminLogin(String adminName, String password, jakarta.servlet.http.HttpSession session) {
        System.out.println("DEBUG: performAdminLogin called with adminName=[" + adminName + "], password=[" + password + "]");
        if (adminName == null || password == null) {
            System.out.println("DEBUG: adminName or password is null!");
            return "redirect:/admin-login?error=invalid_credentials";
        }
        
        try {
            long count = adminRepository.count();
            System.out.println("DEBUG: Admin count in DB = " + count);
            
            java.util.List<University.exam.Entity.Admin> allAdmins = adminRepository.findAll();
            for (University.exam.Entity.Admin a : allAdmins) {
                System.out.println("DEBUG: DB Admin: id=" + a.getId() + ", adminName=[" + a.getAdminName() + "], password=[" + a.getPassword() + "]");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception reading admins from DB: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Auto-seed admin if no admin exists in the database
        if (adminRepository.count() == 0) {
            adminRepository.save(new University.exam.Entity.Admin(null, "admin", "admin", "admin@ljku.edu.in"));
        }

        String trimmedAdminName = adminName.trim();
        String trimmedPassword = password.trim();
        
        java.util.List<University.exam.Entity.Admin> admins = adminRepository.findByAdminNameIgnoreCase(trimmedAdminName);
        if (admins != null && !admins.isEmpty()) {
            for (University.exam.Entity.Admin admin : admins) {
                System.out.println("DEBUG: Found admin in DB: " + admin.getAdminName() + " with password: " + admin.getPassword());
                if (admin.getPassword().equals(password) || admin.getPassword().equals(trimmedPassword)) {
                    System.out.println("DEBUG: Password matched! Logging in as: " + admin.getAdminName());
                    session.setAttribute("loggedInAdmin", admin.getAdminName());
                    return "redirect:/admin/dashboard";
                } else {
                    System.out.println("DEBUG: Password mismatch for admin: " + admin.getAdminName() + "! Input password=[" + password + "] (trimmed=[" + trimmedPassword + "]), DB password=[" + admin.getPassword() + "]");
                }
            }
            // If admin exists in database but password check fails, reject directly
            System.out.println("DEBUG: Login failed due to password mismatch (admin exists in DB).");
            return "redirect:/admin-login?error=invalid_credentials";
        } else {
            System.out.println("DEBUG: Admin not found in DB for input: " + trimmedAdminName);
        }



        System.out.println("DEBUG: Login failed, redirecting back with error.");
        return "redirect:/admin-login?error=invalid_credentials";
    } 
}