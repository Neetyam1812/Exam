package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.Student;
import University.exam.Entity.StudentActiveSession;
import University.exam.Entity.StudentLoginAttempt;
import University.exam.Entity.ExamViolation;
import University.exam.repository.AdminRepository;
import University.exam.repository.StudentRepository;
import University.exam.repository.StudentActiveSessionRepository;
import University.exam.repository.StudentLoginAttemptRepository;
import University.exam.repository.ExamViolationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminSessionController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentActiveSessionRepository studentActiveSessionRepository;

    @Autowired
    private StudentLoginAttemptRepository studentLoginAttemptRepository;

    @Autowired
    private ExamViolationRepository examViolationRepository;

    private Admin getLoggedInAdmin(HttpSession session) {
        if (session == null) return null;
        String adminName = (String) session.getAttribute("loggedInAdmin");
        if (adminName == null) return null;
        List<Admin> admins = adminRepository.findByAdminNameIgnoreCase(adminName.trim());
        if (admins != null && !admins.isEmpty()) {
            return admins.get(0);
        }
        return null;
    }

    private void addAdminAttributes(HttpSession session, Model model) {
        String adminName = (String) session.getAttribute("loggedInAdmin");
        model.addAttribute("adminName", adminName != null ? adminName : "Super Admin");
        model.addAttribute("logoUrl", "/images/logo.png");
    }

    @GetMapping("/sessions")
    public String viewSessions(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        addAdminAttributes(session, model);
        model.addAttribute("activeMenu", "sessions");

        List<Student> students = studentRepository.findAll();
        List<Map<String, Object>> studentSessionsList = new ArrayList<>();

        for (Student student : students) {
            Map<String, Object> data = new HashMap<>();
            data.put("studentName", student.getStudentName());
            data.put("enrollmentNo", student.getEnrollmentNo());

            // Get latest session
            List<StudentActiveSession> sHistory = studentActiveSessionRepository.findByStudentId(student.getEnrollmentNo());
            StudentActiveSession latestSession = null;
            if (sHistory != null && !sHistory.isEmpty()) {
                sHistory.sort((s1, s2) -> s2.getLoginTime().compareTo(s1.getLoginTime()));
                latestSession = sHistory.get(0);
            }

            String activeSessionStatus = "No Session";
            if (latestSession != null) {
                activeSessionStatus = latestSession.getStatus(); // ACTIVE, COMPLETED, TERMINATED
            }
            data.put("activeSession", activeSessionStatus);
            data.put("session", latestSession);

            // Login attempts
            List<StudentLoginAttempt> attempts = studentLoginAttemptRepository.findByStudentIdOrderByAttemptTimeDesc(student.getEnrollmentNo());
            int attemptsCount = attempts != null ? attempts.size() : 0;
            data.put("loginAttempts", attemptsCount);

            String lastAttemptTime = "N/A";
            if (attempts != null && !attempts.isEmpty()) {
                LocalDateTime lat = attempts.get(0).getAttemptTime();
                lastAttemptTime = lat.format(DateTimeFormatter.ofPattern("hh:mm a"));
            }
            data.put("lastAttempt", lastAttemptTime);

            // Determine status
            long blockedCount = studentLoginAttemptRepository.countByStudentIdAndResult(student.getEnrollmentNo(), "BLOCKED");
            String status = "SECURE";
            if (blockedCount > 0) {
                status = "⚠ Suspicious";
            }
            data.put("status", status);
            data.put("blockedAttemptsCount", blockedCount);

            // Fetch proctoring violations
            List<ExamViolation> violations = examViolationRepository.findByStudentIdOrderByTimeDesc(student.getEnrollmentNo());
            int violationCount = violations != null ? violations.size() : 0;
            data.put("violationCount", violationCount);

            String lastViolationTime = "N/A";
            String lastViolationType = "N/A";
            if (violations != null && !violations.isEmpty()) {
                LocalDateTime lvt = violations.get(0).getTime();
                lastViolationTime = lvt.format(DateTimeFormatter.ofPattern("hh:mm a"));
                lastViolationType = violations.get(0).getEventType();
            }
            data.put("lastViolationTime", lastViolationTime);
            data.put("lastViolationType", lastViolationType);

            studentSessionsList.add(data);
        }

        // Get blocked attempts for the alerts list
        List<StudentLoginAttempt> blockedAttempts = studentLoginAttemptRepository.findByResultOrderByAttemptTimeDesc("BLOCKED");
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (StudentLoginAttempt blocked : blockedAttempts) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("enrollment", blocked.getStudentId());
            alert.put("time", blocked.getAttemptTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
            alert.put("ip", blocked.getIpAddress());
            alert.put("browser", blocked.getBrowserInfo());
            alert.put("device", blocked.getDeviceInfo());
            
            String name = "Unknown Student";
            Optional<Student> stOpt = studentRepository.findByEnrollmentNo(blocked.getStudentId());
            if (stOpt.isPresent()) {
                name = stOpt.get().getStudentName();
            }
            alert.put("studentName", name);
            alerts.add(alert);
        }

        model.addAttribute("studentSessions", studentSessionsList);
        model.addAttribute("suspiciousAlerts", alerts);

        return "admin/sessions";
    }

    @PostMapping("/sessions/terminate/{enrollmentNo}")
    public String terminateSession(@PathVariable("enrollmentNo") String enrollmentNo, HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        Optional<StudentActiveSession> activeSessionOpt = studentActiveSessionRepository.findByStudentIdAndStatus(enrollmentNo, "ACTIVE");
        if (activeSessionOpt.isPresent()) {
            StudentActiveSession activeSession = activeSessionOpt.get();
            activeSession.setStatus("TERMINATED");
            activeSession.setLogoutTime(LocalDateTime.now());
            studentActiveSessionRepository.save(activeSession);
            redirectAttributes.addFlashAttribute("successMessage", "Active session for enrollment " + enrollmentNo + " has been manually terminated.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "No active session found for enrollment " + enrollmentNo);
        }

        return "redirect:/admin/sessions";
    }

    @GetMapping("/sessions/detail/{enrollmentNo}")
    @ResponseBody
    public ResponseEntity<?> getSessionDetail(@PathVariable("enrollmentNo") String enrollmentNo, HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Map<String, Object> detail = new HashMap<>();
        
        // Fetch student info
        Optional<Student> studentOpt = studentRepository.findByEnrollmentNo(enrollmentNo);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = studentOpt.get();
        detail.put("studentName", student.getStudentName());
        detail.put("enrollmentNo", student.getEnrollmentNo());

        // Fetch session history
        List<StudentActiveSession> sessions = studentActiveSessionRepository.findByStudentId(enrollmentNo);
        sessions.sort((s1, s2) -> s2.getLoginTime().compareTo(s1.getLoginTime()));

        List<Map<String, Object>> loginHistory = new ArrayList<>();
        Set<String> ipHistory = new LinkedHashSet<>();
        Set<String> deviceHistory = new LinkedHashSet<>();

        for (StudentActiveSession s : sessions) {
            Map<String, Object> map = new HashMap<>();
            map.put("loginTime", s.getLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
            map.put("logoutTime", s.getLogoutTime() != null ? s.getLogoutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) : "Active");
            map.put("ip", s.getIpAddress() != null ? s.getIpAddress() : "Unknown");
            map.put("browser", s.getBrowserInfo() != null ? s.getBrowserInfo() : "Unknown");
            map.put("device", s.getDeviceInfo() != null ? s.getDeviceInfo() : "Unknown");
            map.put("status", s.getStatus());
            loginHistory.add(map);

            if (s.getIpAddress() != null) ipHistory.add(s.getIpAddress());
            if (s.getDeviceInfo() != null && s.getBrowserInfo() != null) {
                deviceHistory.add(s.getDeviceInfo() + " (" + s.getBrowserInfo() + ")");
            }
        }

        // Fetch attempt history
        List<StudentLoginAttempt> attempts = studentLoginAttemptRepository.findByStudentIdOrderByAttemptTimeDesc(enrollmentNo);
        List<Map<String, Object>> attemptHistory = new ArrayList<>();
        for (StudentLoginAttempt a : attempts) {
            Map<String, Object> map = new HashMap<>();
            map.put("time", a.getAttemptTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
            map.put("ip", a.getIpAddress() != null ? a.getIpAddress() : "Unknown");
            map.put("browser", a.getBrowserInfo() != null ? a.getBrowserInfo() : "Unknown");
            map.put("device", a.getDeviceInfo() != null ? a.getDeviceInfo() : "Unknown");
            map.put("result", a.getResult());
            attemptHistory.add(map);

            if (a.getIpAddress() != null) ipHistory.add(a.getIpAddress());
            if (a.getDeviceInfo() != null && a.getBrowserInfo() != null) {
                deviceHistory.add(a.getDeviceInfo() + " (" + a.getBrowserInfo() + ")");
            }
        }

        // Fetch violation history
        List<ExamViolation> violations = examViolationRepository.findByStudentIdOrderByTimeDesc(enrollmentNo);
        List<Map<String, Object>> violationHistory = new ArrayList<>();
        for (ExamViolation v : violations) {
            Map<String, Object> map = new HashMap<>();
            map.put("time", v.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
            map.put("eventType", v.getEventType());
            map.put("examId", v.getExamId() != null ? v.getExamId() : 0L);
            map.put("examType", v.getExamType() != null ? v.getExamType() : "Unknown");
            violationHistory.add(map);
        }

        detail.put("loginHistory", loginHistory);
        detail.put("ipHistory", new ArrayList<>(ipHistory));
        detail.put("deviceHistory", new ArrayList<>(deviceHistory));
        detail.put("attemptHistory", attemptHistory);
        detail.put("violationHistory", violationHistory);

        return ResponseEntity.ok(detail);
    }
}
