package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.Result;
import University.exam.repository.AdminRepository;
import University.exam.service.ResultPdfService;
import University.exam.service.ResultService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin/results")
public class ResultController {

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultPdfService resultPdfService;

    @Autowired
    private AdminRepository adminRepository;

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

    @GetMapping
    public String viewResults(
            @RequestParam(value = "division", required = false) String division,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            Model model) {
            
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }

        model.addAttribute("adminName", admin.getAdminName());
        model.addAttribute("logoUrl", "/images/logo.png");
        
        List<Result> results = resultService.getFilteredResults(admin.getId(), division, semester, subject, status);
        model.addAttribute("results", results != null ? results : java.util.Collections.emptyList());
        
        model.addAttribute("selectedDivision", division);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedStatus", status);

        List<String> distinctSubjects = resultService.getDistinctSubjects(admin.getId());
        model.addAttribute("distinctSubjects", distinctSubjects != null ? distinctSubjects : java.util.Collections.emptyList());
        
        List<String> distinctSemesters = resultService.getDistinctSemesters(admin.getId());
        model.addAttribute("distinctSemesters", distinctSemesters != null ? distinctSemesters : java.util.Collections.emptyList());
        
        List<String> distinctDivisions = resultService.getDistinctDivisions(admin.getId());
        model.addAttribute("distinctDivisions", distinctDivisions != null ? distinctDivisions : java.util.Collections.emptyList());

        return "admin/view_results";
    }

    @PostMapping("/filter")
    public String filterResults(
            @RequestParam(value = "division", required = false) String division,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            Model model) {
        
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }

        model.addAttribute("adminName", admin.getAdminName());
        model.addAttribute("logoUrl", "/images/logo.png");
        
        List<Result> results = resultService.getFilteredResults(admin.getId(), division, semester, subject, status);
        model.addAttribute("results", results != null ? results : java.util.Collections.emptyList());
        
        model.addAttribute("selectedDivision", division);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedStatus", status);

        List<String> distinctSubjects = resultService.getDistinctSubjects(admin.getId());
        model.addAttribute("distinctSubjects", distinctSubjects != null ? distinctSubjects : java.util.Collections.emptyList());
        
        List<String> distinctSemesters = resultService.getDistinctSemesters(admin.getId());
        model.addAttribute("distinctSemesters", distinctSemesters != null ? distinctSemesters : java.util.Collections.emptyList());
        
        List<String> distinctDivisions = resultService.getDistinctDivisions(admin.getId());
        model.addAttribute("distinctDivisions", distinctDivisions != null ? distinctDivisions : java.util.Collections.emptyList());
        
        return "admin/view_results";
    }

    @GetMapping("/pdf")
    public void downloadPdf(
            @RequestParam(value = "division", required = false) String division,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            response.sendRedirect("/admin-login");
            return;
        }

        List<Result> results = resultService.getFilteredResults(admin.getId(), division, semester, subject, status);

        response.setContentType("application/pdf");
        String filename = "results";
        if (division != null && !division.isEmpty()) filename += "_" + division;
        if (semester != null && !semester.isEmpty()) filename += "_sem" + semester;
        filename += ".pdf";

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + filename;
        response.setHeader(headerKey, headerValue);

        resultPdfService.generateResultPdf(response, results, subject, semester, division);
    }
}
