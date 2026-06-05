package University.exam.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import University.exam.Entity.StudentActiveSession;
import University.exam.repository.StudentActiveSessionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ActiveSessionInterceptor implements HandlerInterceptor {

    @Autowired
    private StudentActiveSessionRepository studentActiveSessionRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        String enrollmentNo = (String) session.getAttribute("loggedInStudent");
        if (enrollmentNo == null) {
            return true;
        }

        Optional<StudentActiveSession> activeSessionOpt = 
            studentActiveSessionRepository.findByStudentIdAndIsActiveTrue(enrollmentNo);

        if (activeSessionOpt.isPresent()) {
            StudentActiveSession activeSession = activeSessionOpt.get();
            // If the registered session ID does not match the current HTTP session, block the request
            if (!activeSession.getSessionId().equals(session.getId())) {
                // Invalidate current HTTP session
                session.invalidate();
                
                String uri = request.getRequestURI();
                if (uri != null && uri.contains("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"already_logged_in\", \"message\": \"This student account is already active on another device.\"}");
                } else {
                    response.sendRedirect(request.getContextPath() + "/?error=already_logged_in");
                }
                return false;
            } else {
                // Session matches: update last activity timestamp
                activeSession.setLastActivity(LocalDateTime.now());
                studentActiveSessionRepository.save(activeSession);
            }
        } else {
            // No active session record exists in the database for this student:
            // Clean up any existing session record with the same session ID to prevent unique constraint violation
            studentActiveSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
                studentActiveSessionRepository.delete(s);
                studentActiveSessionRepository.flush();
            });

            // Register a new active session for the current student and session ID
            StudentActiveSession newSession = new StudentActiveSession(enrollmentNo, session.getId());
            studentActiveSessionRepository.save(newSession);
        }

        return true;
    }
}
