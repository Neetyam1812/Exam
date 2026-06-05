package University.exam.repository;

import University.exam.Entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {
    java.util.List<Paper> findByAdminId(Long adminId);
    long countByAdminId(Long adminId);
}
