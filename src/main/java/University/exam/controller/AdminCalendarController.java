package University.exam.controller;

import University.exam.Entity.Admin;
import University.exam.Entity.CalendarEvent;
import University.exam.repository.AdminRepository;
import University.exam.repository.CalendarEventRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/calendar")
public class AdminCalendarController {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private AdminRepository adminRepository;

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
        model.addAttribute("activeMenu", "calendar");
    }

    @GetMapping
    public String showCalendar(HttpSession session, Model model) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/admin-login";
        }
        addAdminAttributes(session, model);
        
        // Load all admin accounts for task assignment selector
        List<String> adminNames = adminRepository.findAll().stream()
                .map(Admin::getAdminName)
                .collect(Collectors.toList());
        model.addAttribute("allAdmins", adminNames);
        
        return "admin/calendar";
    }

    @GetMapping("/events")
    @ResponseBody
    public List<CalendarEvent> getEvents(HttpSession session) {
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) return List.of();
        
        List<CalendarEvent> events = new java.util.ArrayList<>(calendarEventRepository.findVisibleEvents(admin.getAdminName(), admin.getId().toString()));
        
        // Auto-update overdue events
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (CalendarEvent event : events) {
            if ("TASK".equals(event.getEventType()) && 
                !"Completed".equals(event.getStatus()) && 
                !"Overdue".equals(event.getStatus()) && 
                event.getEndDatetime() != null && 
                now.isAfter(event.getEndDatetime())) {
                event.setStatus("Overdue");
                calendarEventRepository.save(event);
                changed = true;
            }
        }
        if (changed) {
            events = new java.util.ArrayList<>(calendarEventRepository.findVisibleEvents(admin.getAdminName(), admin.getId().toString()));
        }
        
        // Inject dynamic festival and public holidays
        events.addAll(getHolidayEvents());
        
        return events;
    }

    private List<CalendarEvent> getHolidayEvents() {
        List<CalendarEvent> holidays = new java.util.ArrayList<>();
        
        // 2026 Festival and Public Holidays
        addHoliday(holidays, 1L, "🎉 New Year's Day", "National Holiday", 2026, 1, 1);
        addHoliday(holidays, 2L, "🇮🇳 Republic Day", "National Holiday", 2026, 1, 26);
        addHoliday(holidays, 3L, "🎨 Holi Festival", "Festival Holiday", 2026, 3, 4);
        addHoliday(holidays, 4L, "🌙 Eid-ul-Fitr", "Festival Holiday", 2026, 3, 20);
        addHoliday(holidays, 5L, "✝️ Good Friday", "Gazetted Holiday", 2026, 4, 3);
        addHoliday(holidays, 6L, "🌸 Ambedkar Jayanti", "Gazetted Holiday", 2026, 4, 14);
        addHoliday(holidays, 7L, "🦚 Independence Day", "National Holiday", 2026, 8, 15);
        addHoliday(holidays, 8L, "🤝 Raksha Bandhan", "Festival Holiday", 2026, 8, 28);
        addHoliday(holidays, 9L, "🥛 Janmashtami", "Festival Holiday", 2026, 9, 4);
        addHoliday(holidays, 10L, "🌸 Ganesh Chaturthi", "Festival Holiday", 2026, 9, 15);
        addHoliday(holidays, 11L, "👓 Gandhi Jayanti", "National Holiday", 2026, 10, 2);
        addHoliday(holidays, 12L, "🏹 Dussehra", "Festival Holiday", 2026, 10, 20);
        addHoliday(holidays, 13L, "🪔 Diwali Festival", "Festival Holiday", 2026, 11, 8);
        addHoliday(holidays, 14L, "🎄 Christmas Day", "Festival Holiday", 2026, 12, 25);
        
        // 2027 Festival and Public Holidays
        addHoliday(holidays, 101L, "🎉 New Year's Day", "National Holiday", 2027, 1, 1);
        addHoliday(holidays, 102L, "🇮🇳 Republic Day", "National Holiday", 2027, 1, 26);
        addHoliday(holidays, 103L, "🎨 Holi Festival", "Festival Holiday", 2027, 3, 22);
        addHoliday(holidays, 104L, "🌙 Eid-ul-Fitr", "Festival Holiday", 2027, 3, 9);
        addHoliday(holidays, 105L, "✝️ Good Friday", "Gazetted Holiday", 2027, 3, 26);
        addHoliday(holidays, 106L, "🌸 Ambedkar Jayanti", "Gazetted Holiday", 2027, 4, 14);
        addHoliday(holidays, 107L, "🦚 Independence Day", "National Holiday", 2027, 8, 15);
        addHoliday(holidays, 108L, "🤝 Raksha Bandhan", "Festival Holiday", 2027, 8, 18);
        addHoliday(holidays, 109L, "🥛 Janmashtami", "Festival Holiday", 2027, 8, 25);
        addHoliday(holidays, 110L, "🌸 Ganesh Chaturthi", "Festival Holiday", 2027, 9, 4);
        addHoliday(holidays, 111L, "👓 Gandhi Jayanti", "National Holiday", 2027, 10, 2);
        addHoliday(holidays, 112L, "🏹 Dussehra", "Festival Holiday", 2027, 10, 9);
        addHoliday(holidays, 113L, "🪔 Diwali Festival", "Festival Holiday", 2027, 10, 29);
        addHoliday(holidays, 114L, "🎄 Christmas Day", "Festival Holiday", 2027, 12, 25);

        return holidays;
    }

    private void addHoliday(List<CalendarEvent> list, Long id, String title, String desc, int year, int month, int day) {
        CalendarEvent h = new CalendarEvent();
        h.setId(-id); // Use negative ids to guarantee no clashes with DB
        h.setTitle(title);
        h.setDescription(desc);
        h.setCategory("Holiday");
        h.setEventType("HOLIDAY");
        h.setPriority("LOW");
        h.setStatus("Completed");
        h.setStartDatetime(LocalDateTime.of(year, month, day, 0, 0));
        h.setEndDatetime(LocalDateTime.of(year, month, day, 23, 59));
        h.setCreatedBy("System");
        h.setAssignedTo("All");
        h.setVisibility("PUBLIC");
        list.add(h);
    }

    @GetMapping("/admins")
    @ResponseBody
    public List<String> getAdmins(HttpSession session) {
        return adminRepository.findAll().stream()
                .map(Admin::getAdminName)
                .collect(Collectors.toList());
    }

    @GetMapping("/users")
    @ResponseBody
    public List<Map<String, Object>> getUsers(HttpSession session) {
        Admin current = getLoggedInAdmin(session);
        if (current == null) return List.of();
        
        List<Admin> allAdmins = adminRepository.findAll();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (Admin a : allAdmins) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("username", a.getAdminName());
            map.put("email", a.getEmail());
            
            String role = "Teacher";
            String displayName = a.getAdminName();
            
            if ("superadmin".equalsIgnoreCase(a.getAdminName())) {
                role = "Super Admin";
                displayName = "Super Admin";
            } else if ("examadmin".equalsIgnoreCase(a.getAdminName())) {
                role = "Exam Admin";
                displayName = "Exam Admin";
            } else if ("facultyadmin".equalsIgnoreCase(a.getAdminName())) {
                role = "Faculty Admin";
                displayName = "Faculty Admin";
            } else if ("controller".equalsIgnoreCase(a.getAdminName())) {
                role = "Controller";
                displayName = "Controller";
            } else {
                role = "Teacher";
                displayName = a.getAdminName() + " (Teacher)";
            }
            
            map.put("role", role);
            map.put("displayName", displayName);
            result.add(map);
        }
        return result;
    }

    @PostMapping("/event/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addEvent(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", defaultValue = "Other") String category,
            @RequestParam(value = "eventType", defaultValue = "TASK") String eventType,
            @RequestParam(value = "priority", defaultValue = "LOW") String priority,
            @RequestParam(value = "startDatetime", required = false) String startDatetimeStr,
            @RequestParam(value = "endDatetime", required = false) String endDatetimeStr,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "startTime", required = false) String startTimeStr,
            @RequestParam(value = "endTime", required = false) String endTimeStr,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @RequestParam(value = "visibility", defaultValue = "SHARED") String visibility,
            @RequestParam(value = "sharedWith", required = false) String sharedWith,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            response.put("status", "error");
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        try {
            CalendarEvent event = new CalendarEvent();
            event.setTitle(title);
            event.setDescription(description);
            event.setCategory(category);
            event.setEventType(eventType);
            event.setPriority(priority);
            event.setVisibility(visibility);
            if (sharedWith != null) {
                event.setSharedWith(sharedWith);
            }
            event.setCreatedBy(admin.getAdminName());
            event.setStatus("Pending");

            if (assignedTo != null && !assignedTo.trim().isEmpty()) {
                event.setAssignedTo(assignedTo.trim());
            } else {
                event.setAssignedTo(admin.getAdminName());
            }

            // Parsing Dates
            LocalDateTime start = null;
            LocalDateTime end = null;

            if (startDatetimeStr != null && !startDatetimeStr.isEmpty()) {
                start = LocalDateTime.parse(startDatetimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (dateStr != null && !dateStr.isEmpty() && startTimeStr != null && !startTimeStr.isEmpty()) {
                LocalDate d = LocalDate.parse(dateStr);
                LocalTime t = LocalTime.parse(startTimeStr);
                start = LocalDateTime.of(d, t);
            }

            if (endDatetimeStr != null && !endDatetimeStr.isEmpty()) {
                end = LocalDateTime.parse(endDatetimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (dateStr != null && !dateStr.isEmpty() && endTimeStr != null && !endTimeStr.isEmpty()) {
                LocalDate d = LocalDate.parse(dateStr);
                LocalTime t = LocalTime.parse(endTimeStr);
                end = LocalDateTime.of(d, t);
            }

            if (start == null) {
                start = LocalDateTime.now();
            }
            if (end == null) {
                end = start.plusHours(1);
            }

            event.setStartDatetime(start);
            event.setEndDatetime(end);

            CalendarEvent saved = calendarEventRepository.save(event);
            response.put("status", "success");
            response.put("event", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to save event: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/event/toggle-complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleComplete(
            @RequestParam("id") Long id,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            response.put("status", "error");
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        Optional<CalendarEvent> eventOpt = calendarEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            CalendarEvent event = eventOpt.get();
            // Check authorization: must be creator, assignee, assigned to everyone/all, or shared to update
            boolean isAssignee = admin.getAdminName().equalsIgnoreCase(event.getAssignedTo());
            boolean isEveryone = "Everyone".equalsIgnoreCase(event.getAssignedTo()) || "All".equalsIgnoreCase(event.getAssignedTo());
            boolean isShared = "SHARED".equalsIgnoreCase(event.getVisibility());
            boolean isPublic = "PUBLIC".equalsIgnoreCase(event.getVisibility());
            if (!admin.getAdminName().equalsIgnoreCase(event.getCreatedBy()) && !isAssignee && !isEveryone && !isShared && !isPublic) {
                response.put("status", "error");
                response.put("message", "Permission denied");
                return ResponseEntity.status(403).body(response);
            }

            if ("Completed".equals(event.getStatus())) {
                event.setStatus("Pending");
            } else {
                event.setStatus("Completed");
            }
            event.setUpdatedAt(LocalDateTime.now());
            CalendarEvent saved = calendarEventRepository.save(event);
            response.put("status", "success");
            response.put("event", saved);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Event not found");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/event/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") String status,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            response.put("status", "error");
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        Optional<CalendarEvent> eventOpt = calendarEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            CalendarEvent event = eventOpt.get();
            boolean isAssignee = admin.getAdminName().equalsIgnoreCase(event.getAssignedTo());
            boolean isEveryone = "Everyone".equalsIgnoreCase(event.getAssignedTo()) || "All".equalsIgnoreCase(event.getAssignedTo());
            boolean isShared = "SHARED".equalsIgnoreCase(event.getVisibility());
            boolean isPublic = "PUBLIC".equalsIgnoreCase(event.getVisibility());
            if (!admin.getAdminName().equalsIgnoreCase(event.getCreatedBy()) && !isAssignee && !isEveryone && !isShared && !isPublic) {
                response.put("status", "error");
                response.put("message", "Permission denied");
                return ResponseEntity.status(403).body(response);
            }

            event.setStatus(status);
            event.setUpdatedAt(LocalDateTime.now());
            CalendarEvent saved = calendarEventRepository.save(event);
            response.put("status", "success");
            response.put("event", saved);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Event not found");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/event/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteEvent(
            @RequestParam("id") Long id,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Admin admin = getLoggedInAdmin(session);
        if (admin == null) {
            response.put("status", "error");
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        Optional<CalendarEvent> eventOpt = calendarEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            CalendarEvent event = eventOpt.get();
            if (!admin.getAdminName().equalsIgnoreCase(event.getCreatedBy())) {
                response.put("status", "error");
                response.put("message", "Only the creator can delete this event");
                return ResponseEntity.status(403).body(response);
            }

            calendarEventRepository.delete(event);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Event not found");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
