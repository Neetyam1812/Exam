package University.exam.repository;

import University.exam.Entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e WHERE " +
           "e.visibility = 'PUBLIC' " +
           "OR e.eventType = 'EXAM' " +
           "OR e.eventType = 'HOLIDAY' " +
           "OR (e.visibility = 'PRIVATE' AND e.createdBy = :username) " +
           "OR (e.visibility = 'SHARED' AND (e.createdBy = :username OR e.sharedWith LIKE CONCAT('%,', :userIdStr, ',%') OR e.assignedTo = 'Everyone' OR e.assignedTo = 'All')) " +
           "OR (e.visibility IS NULL AND (e.eventType = 'EXAM' OR e.createdBy = :username OR e.assignedTo = :username))")
    List<CalendarEvent> findVisibleEvents(@Param("username") String username, @Param("userIdStr") String userIdStr);

    @Query("SELECT e FROM CalendarEvent e WHERE (" +
           "e.visibility = 'PUBLIC' " +
           "OR e.eventType = 'EXAM' " +
           "OR e.eventType = 'HOLIDAY' " +
           "OR (e.visibility = 'PRIVATE' AND e.createdBy = :username) " +
           "OR (e.visibility = 'SHARED' AND (e.createdBy = :username OR e.sharedWith LIKE CONCAT('%,', :userIdStr, ',%') OR e.assignedTo = 'Everyone' OR e.assignedTo = 'All')) " +
           "OR (e.visibility IS NULL AND (e.eventType = 'EXAM' OR e.createdBy = :username OR e.assignedTo = :username))" +
           ") AND e.startDatetime >= :start AND e.endDatetime <= :end")
    List<CalendarEvent> findVisibleEventsInDateRange(
            @Param("username") String username,
            @Param("userIdStr") String userIdStr,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CalendarEvent> findByEventType(String eventType);

    List<CalendarEvent> findByStatus(String status);
}
