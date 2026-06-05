package University.exam.repository;

import University.exam.Entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySubmissionId(Long submissionId);
    List<Answer> findByExamAttemptId(Long examAttemptId);
    Optional<Answer> findFirstByExamAttemptIdAndQuestionIdOrderByUpdatedAtDesc(Long examAttemptId, Long questionId);
    Optional<Answer> findFirstBySubmissionIdAndQuestionIdOrderByUpdatedAtDesc(Long submissionId, Long questionId);
}
