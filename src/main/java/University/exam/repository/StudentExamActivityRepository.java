package University.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import University.exam.Entity.StudentExamActivity;
import java.util.Optional;

@Repository
public interface StudentExamActivityRepository extends JpaRepository<StudentExamActivity, Long> {
    Optional<StudentExamActivity> findByStudentId(Long studentId);
    Optional<StudentExamActivity> findByStudentEnrollmentNo(String enrollmentNo);
}
