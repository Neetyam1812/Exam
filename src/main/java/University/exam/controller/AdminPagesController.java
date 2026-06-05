package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.Student;
import University.exam.Entity.Result;
import University.exam.Entity.Paper;
import University.exam.Entity.Submission;
import University.exam.repository.AdminRepository;
import University.exam.repository.StudentRepository;
import University.exam.repository.ResultRepository;
import University.exam.repository.PaperRepository;
import University.exam.repository.SubmissionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminPagesController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

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

    // ==========================================
    // 1. STUDENTS MANAGEMENT
    // ==========================================
    @GetMapping("/students")
    public String listStudents(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "division", required = false) String division,
            HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        
        addAdminAttributes(session, model);
        
        List<Student> students = studentRepository.findAll();
        
        // Filter list in memory
        List<Student> filteredStudents = students.stream().filter(s -> {
            boolean matches = true;
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean nameMatches = s.getStudentName() != null && s.getStudentName().toLowerCase().contains(q);
                boolean enrollMatches = s.getEnrollmentNo() != null && s.getEnrollmentNo().toLowerCase().contains(q);
                matches = nameMatches || enrollMatches;
            }
            if (matches && semester != null && !semester.trim().isEmpty()) {
                matches = Student.matchesSemester(s.getSemester(), semester);
            }
            if (matches && division != null && !division.trim().isEmpty()) {
                matches = division.equalsIgnoreCase(s.getDivision());
            }
            return matches;
        }).collect(Collectors.toList());
        
        // Sort by enrollment number
        filteredStudents.sort((s1, s2) -> {
            String e1 = s1.getEnrollmentNo() != null ? s1.getEnrollmentNo() : "";
            String e2 = s2.getEnrollmentNo() != null ? s2.getEnrollmentNo() : "";
            return e1.compareToIgnoreCase(e2);
        });
        
        model.addAttribute("students", filteredStudents);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedDivision", division);
        model.addAttribute("searchQuery", search);
        
        // Add distinct semesters and divisions for dropdown filters
        Set<String> semesters = students.stream().map(Student::getSemester).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> divisions = students.stream().map(Student::getDivision).filter(Objects::nonNull).collect(Collectors.toSet());
        model.addAttribute("distinctSemesters", semesters);
        model.addAttribute("distinctDivisions", divisions);
        
        return "admin/view_students";
    }

    @PostMapping("/students/add")
    public String addStudent(
            @RequestParam("studentName") String studentName,
            @RequestParam("enrollmentNo") String enrollmentNo,
            @RequestParam("division") String division,
            @RequestParam("rollNo") Integer rollNo,
            @RequestParam("phoneNo") String phoneNo,
            @RequestParam("semester") String semester,
            @RequestParam("password") String password,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        if (studentRepository.findByEnrollmentNo(enrollmentNo).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Student with Enrollment Number " + enrollmentNo + " already exists!");
            return "redirect:/admin/students";
        }

        Student student = new Student();
        student.setStudentName(studentName);
        student.setEnrollmentNo(enrollmentNo);
        student.setDivision(division);
        student.setRollNo(rollNo);
        student.setPhoneNo(phoneNo);
        student.setSemester(semester);
        student.setPassword(password); // Save plain text credentials to align with existing project database architecture

        studentRepository.save(student);
        redirectAttributes.addFlashAttribute("successMessage", "Student " + studentName + " registered successfully!");
        return "redirect:/admin/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        studentRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student record deleted successfully!");
        return "redirect:/admin/students";
    }

    // ==========================================
    // 2. PERFORMANCE REPORTS
    // ==========================================
    @GetMapping("/reports")
    public String viewReports(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        addAdminAttributes(session, model);

        // Retrieve all papers uploaded by this admin
        List<Paper> papers = paperRepository.findByAdminId(admin.getId());
        
        // Subject Wise stats calculation:
        Map<String, Map<String, Object>> subjectStats = new LinkedHashMap<>();
        
        for (Paper paper : papers) {
            String subject = paper.getSubject();
            if (subject == null) continue;
            
            subjectStats.computeIfAbsent(subject, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("subjectName", k);
                stats.put("courseName", paper.getCourse());
                stats.put("semester", paper.getSemester());
                stats.put("totalSubmissions", 0L);
                stats.put("passedCount", 0L);
                stats.put("failedCount", 0L);
                stats.put("highestMarks", 0.0);
                stats.put("lowestMarks", 1000.0);
                stats.put("totalObtained", 0.0);
                stats.put("totalPaperMarks", paper.getTotalMarks() != null ? paper.getTotalMarks() : 100.0);
                return stats;
            });
            
            Map<String, Object> stats = subjectStats.get(subject);
            
            // Fetch results for this subject/semester
            List<Result> results = resultRepository.findByAdminIdAndSubjectAndSemester(admin.getId(), subject, paper.getSemester());
            if (results != null) {
                for (Result res : results) {
                    stats.put("totalSubmissions", (long)stats.get("totalSubmissions") + 1);
                    double marks = res.getObtainedMarks() != null ? res.getObtainedMarks() : 0.0;
                    stats.put("totalObtained", (double)stats.get("totalObtained") + marks);
                    
                    double highest = (double)stats.get("highestMarks");
                    if (marks > highest) {
                        stats.put("highestMarks", marks);
                    }
                    
                    double lowest = (double)stats.get("lowestMarks");
                    if (marks < lowest) {
                        stats.put("lowestMarks", marks);
                    }
                    
                    if ("PASSED".equalsIgnoreCase(res.getResultStatus()) || "PASS".equalsIgnoreCase(res.getResultStatus())) {
                        stats.put("passedCount", (long)stats.get("passedCount") + 1);
                    } else {
                        stats.put("failedCount", (long)stats.get("failedCount") + 1);
                    }
                }
            }
        }
        
        // Finalize calculations for subjects
        List<Map<String, Object>> subjectStatsList = new ArrayList<>();
        long totalExams = papers.size();
        long totalSubmissions = 0;
        long totalPassed = 0;
        double allObtained = 0.0;
        
        for (Map.Entry<String, Map<String, Object>> entry : subjectStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            long count = (long)stats.get("totalSubmissions");
            if (count > 0) {
                double avg = (double)stats.get("totalObtained") / count;
                stats.put("averageMarks", Math.round(avg * 100.0) / 100.0);
                
                double passRate = ((double)(long)stats.get("passedCount") / count) * 100.0;
                stats.put("passRate", Math.round(passRate * 10.0) / 10.0);
                
                totalSubmissions += count;
                totalPassed += (long)stats.get("passedCount");
                allObtained += (double)stats.get("totalObtained");
            } else {
                stats.put("averageMarks", 0.0);
                stats.put("passRate", 0.0);
                stats.put("lowestMarks", 0.0);
            }
            if ((double)stats.get("lowestMarks") == 1000.0) {
                stats.put("lowestMarks", 0.0);
            }
            
            subjectStatsList.add(stats);
        }
        
        double overallPassRate = totalSubmissions > 0 ? ((double)totalPassed / totalSubmissions) * 100.0 : 0.0;
        double overallAverage = totalSubmissions > 0 ? (allObtained / totalSubmissions) : 0.0;
        
        // Division Wise performance
        List<Student> students = studentRepository.findAll();
        Map<String, Map<String, Object>> divisionStats = new LinkedHashMap<>();
        
        for (Student student : students) {
            String divKey = (student.getSemester() != null ? student.getSemester() : "Sem 3") + " - " + (student.getDivision() != null ? student.getDivision() : "A");
            divisionStats.computeIfAbsent(divKey, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("divisionKey", k);
                stats.put("totalStudents", 0L);
                stats.put("examAttempts", 0L);
                return stats;
            });
            
            Map<String, Object> stats = divisionStats.get(divKey);
            stats.put("totalStudents", (long)stats.get("totalStudents") + 1);
        }
        
        // Count attempts for each student/division
        for (Submission sub : submissionRepository.findByPaperAdminId(admin.getId())) {
            if (sub.getStudent() != null) {
                String divKey = (sub.getStudent().getSemester() != null ? sub.getStudent().getSemester() : "Sem 3") + " - " + (sub.getStudent().getDivision() != null ? sub.getStudent().getDivision() : "A");
                Map<String, Object> stats = divisionStats.get(divKey);
                if (stats != null) {
                    stats.put("examAttempts", (long)stats.get("examAttempts") + 1);
                }
            }
        }
        
        model.addAttribute("subjectStats", subjectStatsList);
        model.addAttribute("divisionStats", new ArrayList<>(divisionStats.values()));
        model.addAttribute("totalExamsCount", totalExams);
        model.addAttribute("totalSubmissionsCount", totalSubmissions);
        model.addAttribute("overallPassRate", Math.round(overallPassRate * 10.0) / 10.0);
        model.addAttribute("overallAverageScore", Math.round(overallAverage * 100.0) / 100.0);
        
        // Distinct filters for PDF export linkage
        model.addAttribute("distinctSubjects", resultRepository.findDistinctSubjectNamesByAdminId(admin.getId()));
        model.addAttribute("distinctSemesters", resultRepository.findDistinctSemestersByAdminId(admin.getId()));
        model.addAttribute("distinctDivisions", resultRepository.findDistinctDivisionsByAdminId(admin.getId()));

        return "admin/reports";
    }

    // ==========================================
    // 3. ACCOUNT SETTINGS
    // ==========================================
    @GetMapping("/settings")
    public String viewSettings(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";
        
        addAdminAttributes(session, model);
        model.addAttribute("admin", admin);
        
        return "admin/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(
            @RequestParam("adminName") String adminName,
            @RequestParam("email") String email,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        // Check if username is already taken by another admin
        List<Admin> matches = adminRepository.findByAdminNameIgnoreCase(adminName.trim());
        if (matches != null && !matches.isEmpty() && !matches.get(0).getId().equals(admin.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username '" + adminName + "' is already taken.");
            return "redirect:/admin/settings";
        }

        admin.setAdminName(adminName.trim());
        admin.setEmail(email.trim());
        adminRepository.save(admin);
        
        // Update session username
        session.setAttribute("loggedInAdmin", admin.getAdminName());
        
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/password")
    public String updatePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/admin-login";

        if (!admin.getPassword().equals(currentPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Incorrect current password!");
            return "redirect:/admin/settings";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match!");
            return "redirect:/admin/settings";
        }

        admin.setPassword(newPassword);
        adminRepository.save(admin);

        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        return "redirect:/admin/settings";
    }
}
