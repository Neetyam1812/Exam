package University.exam.service;

import University.exam.Entity.Student;
import University.exam.Entity.StudentExamActivity;
import University.exam.repository.StudentExamActivityRepository;
import University.exam.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudentExamActivityService {

    @Autowired
    private StudentExamActivityRepository studentExamActivityRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    public void updateActivity(String enrollmentNo, Long examId, String currentSection, Integer currentQuestionNo, String timeRemaining, String status) {
        if (enrollmentNo == null) {
            return;
        }

        Student student = studentRepository.findByEnrollmentNo(enrollmentNo).orElse(null);
        if (student == null) {
            return;
        }

        StudentExamActivity activity = studentExamActivityRepository.findByStudentId(student.getId())
                .orElseGet(() -> {
                    StudentExamActivity newActivity = new StudentExamActivity();
                    newActivity.setStudent(student);
                    return newActivity;
                });

        if (examId != null) {
            activity.setExamId(examId);
        }
        if (currentSection != null) {
            activity.setCurrentSection(currentSection);
        }
        if (currentQuestionNo != null) {
            activity.setCurrentQuestionNo(currentQuestionNo);
        }
        if (timeRemaining != null) {
            activity.setTimeRemaining(timeRemaining);
        }

        // Handle status transitions securely
        // Once status is Submitted or Terminated, do not revert to Active unless a new exam is started
        if (status != null) {
            activity.setStatus(status);
        } else {
            // Default to Active if not specified and not already Submitted/Terminated
            String currentStatus = activity.getStatus();
            if (currentStatus == null || (!"Submitted".equalsIgnoreCase(currentStatus) && !"Terminated".equalsIgnoreCase(currentStatus))) {
                activity.setStatus("Active");
            }
        }

        activity.setLastActivityTime(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());

        studentExamActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<StudentExamActivity> getAllActivities() {
        List<StudentExamActivity> activities = studentExamActivityRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        // Dynamically compute "Inactive" status for active/idle students
        for (StudentExamActivity activity : activities) {
            String currentStatus = activity.getStatus();
            if (currentStatus == null || (!"Submitted".equalsIgnoreCase(currentStatus) && !"Terminated".equalsIgnoreCase(currentStatus))) {
                if (activity.getLastActivityTime() == null || activity.getLastActivityTime().isBefore(now.minusSeconds(60))) {
                    activity.setStatus("Inactive");
                } else {
                    activity.setStatus("Active");
                }
            }
        }
        return activities;
    }
}
