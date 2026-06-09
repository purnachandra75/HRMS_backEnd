package com.employee.management.backend.repository;

import com.employee.management.backend.model.LeaveHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveHistoryRepository extends JpaRepository<LeaveHistory, Long> {
    List<LeaveHistory> findByEmployeeEmpId(Long empId);
    List<LeaveHistory> findByEmployeeEmpIdAndYear(Long empId, Integer year);
}
