package com.employee.management.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.LeaveRequest;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeEmpIdOrderByCreatedAtDesc(Long empId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(lr.status)) = TRIM(LOWER(:status))) " +
            "ORDER BY lr.createdAt DESC")
    Page<LeaveRequest> filterLeaveRequests(@Param("status") String status, Pageable pageable);
}
