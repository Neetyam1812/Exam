package University.exam.repository;

import University.exam.Entity.CanvasDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DrawingRepository extends JpaRepository<CanvasDataEntity, Long> {
    Optional<CanvasDataEntity> findByAnswerId(Long answerId);
}
