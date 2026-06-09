package com.employee.management.backend.repository;

import com.employee.management.backend.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeEmpId(Long empId);
    Optional<LeaveBalance> findByEmployeeEmpIdAndLeaveType(Long empId, String leaveType);
}
