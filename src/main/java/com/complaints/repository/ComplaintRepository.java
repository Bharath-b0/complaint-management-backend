package com.complaints.repository;

import com.complaints.entity.Complaint;
import com.complaints.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    Optional<Complaint> findByTicketNumber(String ticketNumber);

    Page<Complaint> findBySubmittedBy(User user, Pageable pageable);

    Page<Complaint> findByAssignedTo(User agent, Pageable pageable);

    Page<Complaint> findByStatus(Complaint.Status status, Pageable pageable);

    Page<Complaint> findByCategory(Complaint.Category category, Pageable pageable);

    long countByStatus(Complaint.Status status);

    long countByPriority(Complaint.Priority priority);

    long countBySubmittedBy(User user);

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'CLOSED') AND c.dueDate < :now")
    List<Complaint> findOverdueComplaints(@Param("now") LocalDateTime now);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :start AND c.createdAt <= :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Complaint c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.ticketNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Complaint> searchComplaints(@Param("query") String query, Pageable pageable);
}
