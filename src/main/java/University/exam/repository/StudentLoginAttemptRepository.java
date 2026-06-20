package University.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import University.exam.Entity.StudentLoginAttempt;
import java.util.List;

@Repository
public interface StudentLoginAttemptRepository extends JpaRepository<StudentLoginAttempt, Long> {
    List<StudentLoginAttempt> findByStudentId(String studentId);
    List<StudentLoginAttempt> findByResult(String result);
    List<StudentLoginAttempt> findByResultOrderByAttemptTimeDesc(String result);
    List<StudentLoginAttempt> findByStudentIdOrderByAttemptTimeDesc(String studentId);
    long countByStudentIdAndResult(String studentId, String result);
}
