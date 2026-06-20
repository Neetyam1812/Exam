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

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.ExamAttemptRepository examAttemptRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.SubmissionRepository submissionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private University.exam.repository.StudentLoginAttemptRepository studentLoginAttemptRepository;

    @GetMapping("/")
    public String login() {
        return "auth/student_login";
    }

    @GetMapping("/login")
    public String loginGetFallback() {
        return "redirect:/";
    }

    @org.springframework.web.bind.annotation.PostMapping("/login")
    public String performLogin(String enrollmentNo, String password, jakarta.servlet.http.HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
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

        // Get request details
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String browserInfo = getBrowserInfo(userAgent);
        String deviceInfo = getDeviceInfo(userAgent);

        // Enforce single active session per student account: Check if an ACTIVE session already exists
        java.util.Optional<University.exam.Entity.StudentActiveSession> activeSessionOpt = 
            studentActiveSessionRepository.findByStudentIdAndStatus(trimmedEnrollment, "ACTIVE");
             
        if (activeSessionOpt.isPresent()) {
            // Block login immediately since an active session already exists
            University.exam.Entity.StudentLoginAttempt attempt = new University.exam.Entity.StudentLoginAttempt(
                trimmedEnrollment, ipAddress, browserInfo, deviceInfo, "BLOCKED"
            );
            studentLoginAttemptRepository.save(attempt);
            return "redirect:/?error=already_logged_in";
        }

        // Log successful login attempt
        University.exam.Entity.StudentLoginAttempt attempt = new University.exam.Entity.StudentLoginAttempt(
            trimmedEnrollment, ipAddress, browserInfo, deviceInfo, "SUCCESS"
        );
        studentLoginAttemptRepository.save(attempt);

        // Clean up any existing session record with the same session ID to prevent unique constraint violation
        studentActiveSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
            studentActiveSessionRepository.delete(s);
            studentActiveSessionRepository.flush();
        });

        // Register the new active session
        University.exam.Entity.StudentActiveSession newSession = new University.exam.Entity.StudentActiveSession(
            trimmedEnrollment, session.getId(), ipAddress, browserInfo, deviceInfo
        );
        newSession.setStatus("ACTIVE");
        studentActiveSessionRepository.save(newSession);

        // Mock authentication
        session.setAttribute("loggedInStudent", trimmedEnrollment);
        session.setAttribute("enrollment_no", trimmedEnrollment);
        
        // Redirect to smart routing page which validates semester and redirects appropriately
        return "redirect:/student/rules";
    }

    private String getBrowserInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("opr") || ua.contains("opera")) return "Opera";
        return "Browser";
    }

    private String getDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) return "Android Device";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS Device";
        if (ua.contains("windows")) return "Windows PC";
        if (ua.contains("macintosh") || ua.contains("mac os x")) return "macOS Device";
        if (ua.contains("linux")) return "Linux PC";
        return "Mobile/Desktop";
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
        
        // Check for ongoing attempt/submission to force resume screen
        java.util.List<University.exam.Entity.ExamAttempt> attempts = examAttemptRepository.findByStudentEnrollmentNo(enrollmentNo);
        if (attempts != null) {
            for (University.exam.Entity.ExamAttempt attempt : attempts) {
                if ("Ongoing".equals(attempt.getStatus())) {
                    return "redirect:/student/exam/resume";
                }
            }
        }
        java.util.List<University.exam.Entity.Submission> submissions = submissionRepository.findByStudentEnrollmentNo(enrollmentNo);
        if (submissions != null) {
            for (University.exam.Entity.Submission sub : submissions) {
                if ("Ongoing".equals(sub.getStatus())) {
                    return "redirect:/student/exam/resume";
                }
            }
        }

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