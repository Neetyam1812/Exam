package University.exam.repository;

import University.exam.Entity.ExamViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamViolationRepository extends JpaRepository<ExamViolation, Long> {
    List<ExamViolation> findByStudentId(String studentId);
    List<ExamViolation> findByStudentIdOrderByTimeDesc(String studentId);
    long countByStudentIdAndEventType(String studentId, String eventType);
}
