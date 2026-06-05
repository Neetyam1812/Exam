package University.exam.repository;

import University.exam.Entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByStudentEnrollmentNoAndExamIdAndExamType(String enrollmentNo, Long examId, String examType);
}
