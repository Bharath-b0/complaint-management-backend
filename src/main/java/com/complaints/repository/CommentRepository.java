package com.complaints.repository;

import com.complaints.entity.Comment;
import com.complaints.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByComplaintOrderByCreatedAtAsc(Complaint complaint);
    List<Comment> findByComplaintAndInternalFalseOrderByCreatedAtAsc(Complaint complaint);
}
