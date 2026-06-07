package University.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import University.exam.Entity.ExamPasteLog;

@Repository
public interface ExamPasteLogRepository extends JpaRepository<ExamPasteLog, Long> {
}
