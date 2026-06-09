package com.employee.management.backend.repository;

import com.employee.management.backend.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeEmpIdOrderByCreatedAtDesc(Long empId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
