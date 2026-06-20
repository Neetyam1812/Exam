package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.StudentExamActivity;
import University.exam.repository.AdminRepository;
import University.exam.Entity.StudentActiveSession;
import University.exam.repository.StudentActiveSessionRepository;
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

        List<StudentExamActivity> activities = studentExamActivityService.getAllActivities();

        // Filter list in memory
        List<StudentExamActivity> filteredActivities = activities.stream().filter(act -> {
            if (act == null || act.getStudent() == null) {
                return false;
            }
            boolean matches = true;
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                matches = act.getStudent().getEnrollmentNo() != null && 
                          act.getStudent().getEnrollmentNo().toLowerCase().contains(q);
            }
            if (matches && division != null && !division.trim().isEmpty()) {
                matches = division.equalsIgnoreCase(act.getStudent().getDivision());
            }
            if (matches && status != null && !status.trim().isEmpty()) {
                matches = status.equalsIgnoreCase(act.getStatus());
            }
            return matches;
        }).collect(Collectors.toList());

        // Sort by last activity time descending
        filteredActivities.sort((a1, a2) -> {
            if (a1.getLastActivityTime() == null) return 1;
            if (a2.getLastActivityTime() == null) return -1;
            return a2.getLastActivityTime().compareTo(a1.getLastActivityTime());
        });

        if (isJson) {
            List<Map<String, Object>> jsonResponse = filteredActivities.stream().map(act -> {
                Map<String, Object> map = new HashMap<>();
                map.put("studentName", act.getStudent().getStudentName());
                map.put("enrollmentNo", act.getStudent().getEnrollmentNo());
                map.put("division", act.getStudent().getDivision());
                
                // Fetch student active/latest login session IP to derive room and computer numbers
                List<StudentActiveSession> sHistory = studentActiveSessionRepository.findByStudentId(act.getStudent().getEnrollmentNo());
                String ipAddress = "N/A";
                if (sHistory != null && !sHistory.isEmpty()) {
                    sHistory.sort((s1, s2) -> s2.getLoginTime().compareTo(s1.getLoginTime()));
                    ipAddress = sHistory.get(0).getIpAddress();
                }
                map.put("roomNo", getRoomNoFromIp(ipAddress));
                map.put("computerNo", getComputerNoFromIp(ipAddress));
                
                map.put("currentSection", act.getCurrentSection() != null ? act.getCurrentSection() : "N/A");
                map.put("currentQuestionNo", act.getCurrentQuestionNo() != null ? act.getCurrentQuestionNo() : "N/A");
                map.put("timeRemaining", act.getTimeRemaining() != null ? act.getTimeRemaining() : "N/A");
                map.put("status", act.getStatus());
                map.put("lastActivity", act.getLastActivityTime() != null ? act.getLastActivityTime().toString() : "N/A");
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(jsonResponse);
        }

        addAdminAttributes(session, model);
        model.addAttribute("activeMenu", "live-monitor");
        model.addAttribute("activities", filteredActivities);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedDivision", division);
        model.addAttribute("selectedStatus", status);

        // Fetch distinct divisions of all registered student activities for dropdown dynamic filter options
        Set<String> divisions = activities.stream()
                .filter(act -> act != null && act.getStudent() != null)
                .map(act -> act.getStudent().getDivision())
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
