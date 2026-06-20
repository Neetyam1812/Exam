package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.StudentExamActivity;
import University.exam.repository.AdminRepository;
import University.exam.Entity.StudentActiveSession;
import University.exam.repository.StudentActiveSessionRepository;
import University.exam.repository.StudentRepository;
import University.exam.Entity.Student;
import University.exam.service.StudentExamActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class LiveMonitorController {

    @Autowired
    private StudentExamActivityService studentExamActivityService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StudentActiveSessionRepository studentActiveSessionRepository;

    private String getRoomNoFromIp(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "Local";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return "Room " + parts[2];
        }
        return "N/A";
    }

    private String getComputerNoFromIp(String ip) {
        if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "PC-Local";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return "PC-" + parts[3];
        }
        return "N/A";
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

    private void addAdminAttributes(HttpSession session, Model model) {
        String adminName = (String) session.getAttribute("loggedInAdmin");
        model.addAttribute("adminName", adminName != null ? adminName : "Super Admin");
        model.addAttribute("logoUrl", "/images/logo.png");
    }

    @GetMapping("/admin/live-monitor")
    public Object liveMonitor(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "division", required = false) String division,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "format", required = false) String format,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        Admin admin = getLoggedInAdmin(session);
        boolean isJson = "json".equalsIgnoreCase(format) || 
                         (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (admin == null) {
            if (isJson) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            return "redirect:/admin-login";
        }

        List<Student> allStudents = studentRepository.findAll();
        List<StudentExamActivity> activities = studentExamActivityService.getAllActivities();
        Map<String, StudentExamActivity> activityMap = activities.stream()
                .filter(act -> act != null && act.getStudent() != null)
                .collect(Collectors.toMap(act -> act.getStudent().getEnrollmentNo(), act -> act, (a1, a2) -> a1));

        List<StudentActiveSession> activeSessions = studentActiveSessionRepository.findByStatus("ACTIVE");
        Set<String> activeSessionStudentIds = activeSessions.stream()
                .map(StudentActiveSession::getStudentId)
                .collect(Collectors.toSet());

        // Construct derived status list for all students
        List<Map<String, Object>> studentDataList = allStudents.stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentName", student.getStudentName());
            map.put("enrollmentNo", student.getEnrollmentNo());
            map.put("division", student.getDivision() != null ? student.getDivision() : "N/A");
            
            // IP and room/computer numbers
            List<StudentActiveSession> sHistory = studentActiveSessionRepository.findByStudentId(student.getEnrollmentNo());
            String ipAddress = "N/A";
            if (sHistory != null && !sHistory.isEmpty()) {
                sHistory.sort((s1, s2) -> s2.getLoginTime().compareTo(s1.getLoginTime()));
                ipAddress = sHistory.get(0).getIpAddress();
            }
            map.put("roomNo", getRoomNoFromIp(ipAddress));
            map.put("computerNo", getComputerNoFromIp(ipAddress));
            
            StudentExamActivity activity = activityMap.get(student.getEnrollmentNo());
            if (activity != null) {
                map.put("currentSection", activity.getCurrentSection() != null ? activity.getCurrentSection() : "N/A");
                map.put("currentQuestionNo", activity.getCurrentQuestionNo() != null ? activity.getCurrentQuestionNo() : "N/A");
                map.put("timeRemaining", activity.getTimeRemaining() != null ? activity.getTimeRemaining() : "N/A");
                map.put("status", activity.getStatus());
                map.put("lastActivity", activity.getLastActivityTime() != null ? activity.getLastActivityTime().toString() : "N/A");
            } else {
                map.put("currentSection", "N/A");
                map.put("currentQuestionNo", "N/A");
                map.put("timeRemaining", "N/A");
                if (activeSessionStudentIds.contains(student.getEnrollmentNo())) {
                    map.put("status", "LoggedInNotStarted");
                } else {
                    map.put("status", "NotLoggedIn");
                }
                map.put("lastActivity", "N/A");
            }
            return map;
        }).collect(Collectors.toList());

        // Filter list in memory
        List<Map<String, Object>> filteredData = studentDataList.stream().filter(map -> {
            boolean matches = true;
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                matches = map.get("enrollmentNo") != null && 
                          ((String) map.get("enrollmentNo")).toLowerCase().contains(q);
            }
            if (matches && division != null && !division.trim().isEmpty()) {
                matches = division.equalsIgnoreCase((String) map.get("division"));
            }
            if (matches && status != null && !status.trim().isEmpty()) {
                matches = status.equalsIgnoreCase((String) map.get("status"));
            }
            return matches;
        }).collect(Collectors.toList());

        // Sort: Active/Inactive/Submitted/Terminated first (by last activity descending if available), then LoggedInNotStarted, then NotLoggedIn
        filteredData.sort((m1, m2) -> {
            String s1 = (String) m1.get("status");
            String s2 = (String) m2.get("status");
            
            int score1 = ("LoggedInNotStarted".equalsIgnoreCase(s1)) ? 1 : (("NotLoggedIn".equalsIgnoreCase(s1)) ? 2 : 0);
            int score2 = ("LoggedInNotStarted".equalsIgnoreCase(s2)) ? 1 : (("NotLoggedIn".equalsIgnoreCase(s2)) ? 2 : 0);
            
            if (score1 != score2) {
                return Integer.compare(score1, score2);
            }
            
            String t1 = (String) m1.get("lastActivity");
            String t2 = (String) m2.get("lastActivity");
            if (t1 != null && !t1.equals("N/A") && t2 != null && !t2.equals("N/A")) {
                return t2.compareTo(t1);
            } else if (t1 != null && !t1.equals("N/A")) {
                return -1;
            } else if (t2 != null && !t2.equals("N/A")) {
                return 1;
            }
            
            String e1 = (String) m1.get("enrollmentNo");
            String e2 = (String) m2.get("enrollmentNo");
            return e1.compareTo(e2);
        });

        if (isJson) {
            return ResponseEntity.ok(filteredData);
        }

        addAdminAttributes(session, model);
        model.addAttribute("activeMenu", "live-monitor");
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedDivision", division);
        model.addAttribute("selectedStatus", status);

        // Fetch distinct divisions of all registered students for dropdown dynamic filter options
        Set<String> divisions = allStudents.stream()
                .map(Student::getDivision)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        model.addAttribute("distinctDivisions", divisions);

        return "admin/live-monitor";
    }

    @PostMapping("/student/update-activity")
    @ResponseBody
    public ResponseEntity<?> updateActivity(@RequestBody Map<String, Object> payload, HttpSession session) {
        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Long examId = payload.get("examId") != null ? Long.valueOf(payload.get("examId").toString()) : null;
        String currentSection = payload.get("currentSection") != null ? payload.get("currentSection").toString() : null;
        Integer currentQuestionNo = payload.get("currentQuestionNo") != null ? Integer.valueOf(payload.get("currentQuestionNo").toString()) : null;
        String timeRemaining = payload.get("timeRemaining") != null ? payload.get("timeRemaining").toString() : null;
        String status = payload.get("status") != null ? payload.get("status").toString() : null;

        studentExamActivityService.updateActivity(enrollmentNo, examId, currentSection, currentQuestionNo, timeRemaining, status);

        return ResponseEntity.ok(Collections.singletonMap("status", "updated"));
    }
}
