package University.exam.repository;

import University.exam.Entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    long countByStatus(String status);
    Optional<Submission> findByStudentEnrollmentNoAndPaperId(String enrollmentNo, Long paperId);
    java.util.List<Submission> findByPaperId(Long paperId);
    java.util.List<Submission> findByPaperAdminId(Long adminId);
    long countByPaperAdminId(Long adminId);
    long countByStatusAndPaperAdminId(String status, Long adminId);
    java.util.List<Submission> findByStudentEnrollmentNo(String enrollmentNo);
}
