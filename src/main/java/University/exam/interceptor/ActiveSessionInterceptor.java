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

    @Autowired
    private University.exam.repository.ExamAttemptRepository examAttemptRepository;

    @Autowired
    private University.exam.repository.SubmissionRepository submissionRepository;

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

        String uri = request.getRequestURI();
        if (uri.contains("/student/exam/resume") || uri.contains("/student/logout") || uri.contains("/logout") || uri.contains("/api/drawing/save")) {
            return true;
        }

        // Find ongoing attempts/submissions
        University.exam.Entity.ExamAttempt ongoingAttempt = null;
        java.util.List<University.exam.Entity.ExamAttempt> attempts = examAttemptRepository.findByStudentEnrollmentNo(enrollmentNo);
        if (attempts != null) {
            for (University.exam.Entity.ExamAttempt a : attempts) {
                if ("Ongoing".equals(a.getStatus())) {
                    ongoingAttempt = a;
                    break;
                }
            }
        }

        University.exam.Entity.Submission ongoingSubmission = null;
        java.util.List<University.exam.Entity.Submission> submissions = submissionRepository.findByStudentEnrollmentNo(enrollmentNo);
        if (submissions != null) {
            for (University.exam.Entity.Submission s : submissions) {
                if ("Ongoing".equals(s.getStatus())) {
                    ongoingSubmission = s;
                    break;
                }
            }
        }

        if (ongoingAttempt != null || ongoingSubmission != null) {
            boolean isPaused = ongoingAttempt != null ? Boolean.TRUE.equals(ongoingAttempt.getIsPaused()) : Boolean.TRUE.equals(ongoingSubmission.getIsPaused());
            if (isPaused) {
                if (uri.contains("/api/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"status\": \"paused\"}");
                } else {
                    response.sendRedirect(request.getContextPath() + "/student/exam/resume");
                }
                return false;
            }

            // Check for inactivity/offline gap since last request
            Optional<StudentActiveSession> activeSessionOpt = 
                studentActiveSessionRepository.findByStudentIdAndIsActiveTrue(enrollmentNo);

            if (activeSessionOpt.isPresent()) {
                StudentActiveSession activeSession = activeSessionOpt.get();
                if (activeSession.getSessionId().equals(session.getId())) {
                    LocalDateTime lastAct = activeSession.getLastActivity();
                    if (lastAct != null) {
                        long gap = java.time.Duration.between(lastAct, LocalDateTime.now()).getSeconds();
                        if (gap > 15) {
                            if (ongoingAttempt != null) {
                                ongoingAttempt.setIsPaused(true);
                                ongoingAttempt.setPausedAt(lastAct);
                                ongoingAttempt.setPauseCount((ongoingAttempt.getPauseCount() != null ? ongoingAttempt.getPauseCount() : 0) + 1);
                                
                                long totalSeconds = ongoingAttempt.getExam().getExamDuration() != null ? ongoingAttempt.getExam().getExamDuration() * 60 : 3600;
                                long elapsed = java.time.Duration.between(ongoingAttempt.getStartTime(), lastAct).getSeconds();
                                int remaining = (int) Math.max(0, totalSeconds - elapsed);
                                ongoingAttempt.setRemainingTimeSeconds(remaining);
                                
                                examAttemptRepository.save(ongoingAttempt);
                            } else {
                                ongoingSubmission.setIsPaused(true);
                                ongoingSubmission.setPausedAt(lastAct);
                                ongoingSubmission.setPauseCount((ongoingSubmission.getPauseCount() != null ? ongoingSubmission.getPauseCount() : 0) + 1);

                                long totalSeconds = (ongoingSubmission.getPaper().getExamDuration() != null ? ongoingSubmission.getPaper().getExamDuration() : 120) * 60;
                                long elapsed = java.time.Duration.between(ongoingSubmission.getSubmittedAt(), lastAct).getSeconds();
                                int remaining = (int) Math.max(0, totalSeconds - elapsed);
                                ongoingSubmission.setRemainingTimeSeconds(remaining);

                                submissionRepository.save(ongoingSubmission);
                            }
                            
                            if (uri.contains("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("{\"status\": \"paused\"}");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/student/exam/resume");
                            }
                            return false;
                        }
                    }
                }
            }
        }

        Optional<StudentActiveSession> activeSessionOpt = 
            studentActiveSessionRepository.findByStudentIdAndIsActiveTrue(enrollmentNo);

        if (activeSessionOpt.isPresent()) {
            StudentActiveSession activeSession = activeSessionOpt.get();
            // If the registered session ID does not match the current HTTP session, block the request
            if (!activeSession.getSessionId().equals(session.getId())) {
                // Invalidate current HTTP session
                session.invalidate();
                
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
