package com.pcare.support.repo;

import com.pcare.support.domain.ComplaintComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, Long> {
    List<ComplaintComment> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
