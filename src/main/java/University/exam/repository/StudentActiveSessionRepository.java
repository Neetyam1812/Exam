package University.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import University.exam.Entity.StudentActiveSession;
import java.util.Optional;
import java.util.List;

@Repository
public interface StudentActiveSessionRepository extends JpaRepository<StudentActiveSession, Long> {
    Optional<StudentActiveSession> findByStudentIdAndIsActiveTrue(String studentId);
    Optional<StudentActiveSession> findBySessionId(String sessionId);
    List<StudentActiveSession> findByStudentId(String studentId);
}
