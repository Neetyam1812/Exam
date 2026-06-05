package University.exam.repository;

import University.exam.Entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    java.util.Optional<Result> findBySubmissionId(Long submissionId);
    
    @Query("SELECT r FROM Result r WHERE r.submission.paper.subject = :subjectName AND r.submission.paper.semester = :semester ORDER BY r.submission.student.division ASC, r.submission.student.enrollmentNo ASC")
    List<Result> findBySubjectAndSemester(@Param("subjectName") String subjectName, @Param("semester") String semester);
    
    @Query("SELECT r FROM Result r WHERE r.submission.paper.subject = :subjectName AND r.submission.paper.semester = :semester AND r.submission.student.division = :division ORDER BY r.submission.student.enrollmentNo ASC")
    List<Result> findBySubjectAndSemesterAndDivision(@Param("subjectName") String subjectName, @Param("semester") String semester, @Param("division") String division);
                               
    @Query("SELECT DISTINCT r.submission.paper.subject FROM Result r WHERE r.submission.paper.subject IS NOT NULL")
    List<String> findDistinctSubjectNames();

    @Query("SELECT DISTINCT r.submission.paper.semester FROM Result r WHERE r.submission.paper.semester IS NOT NULL")
    List<String> findDistinctSemesters();

    @Query("SELECT DISTINCT r.submission.student.division FROM Result r WHERE r.submission.student.division IS NOT NULL")
    List<String> findDistinctDivisions();

    long countByResultStatus(String resultStatus);

    @Query("SELECT r FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.submission.paper.subject = :subjectName AND r.submission.paper.semester = :semester ORDER BY r.submission.student.division ASC, r.submission.student.enrollmentNo ASC")
    List<Result> findByAdminIdAndSubjectAndSemester(@Param("adminId") Long adminId, @Param("subjectName") String subjectName, @Param("semester") String semester);
    
    @Query("SELECT r FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.submission.paper.subject = :subjectName AND r.submission.paper.semester = :semester AND r.submission.student.division = :division ORDER BY r.submission.student.enrollmentNo ASC")
    List<Result> findByAdminIdAndSubjectAndSemesterAndDivision(@Param("adminId") Long adminId, @Param("subjectName") String subjectName, @Param("semester") String semester, @Param("division") String division);
                               
    @Query("SELECT DISTINCT r.submission.paper.subject FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.submission.paper.subject IS NOT NULL")
    List<String> findDistinctSubjectNamesByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT DISTINCT r.submission.paper.semester FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.submission.paper.semester IS NOT NULL")
    List<String> findDistinctSemestersByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT DISTINCT r.submission.student.division FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.submission.student.division IS NOT NULL")
    List<String> findDistinctDivisionsByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(r) FROM Result r WHERE r.submission.paper.admin.id = :adminId AND r.resultStatus = :resultStatus")
    long countByAdminIdAndResultStatus(@Param("adminId") Long adminId, @Param("resultStatus") String resultStatus);
}
