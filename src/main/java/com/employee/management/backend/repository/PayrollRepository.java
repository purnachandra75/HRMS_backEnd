package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    // Joins to Employee (a plain equality join, not a mapped relation - Payroll only stores a
    // raw employeeId) purely to scope by e.client.id, so one company's admin can never see
    // another company's payroll rows. Status/search are optional filters applied server-side;
    // search matches the client-assigned Employee ID exactly, or the employee name partially.
    @Query("SELECT p FROM Payroll p JOIN Employee e ON e.empId = p.employeeId " +
            "WHERE e.client.id = :clientId AND p.month = :month AND p.year = :year " +
            "AND (:status IS NULL OR :status = '' OR TRIM(LOWER(p.creditStatus)) = TRIM(LOWER(:status))) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(COALESCE(e.employeeId, '')) = LOWER(:search) " +
            "     OR LOWER(p.employeeName) LIKE LOWER(CONCAT('%',:search,'%'))) " +
            "ORDER BY p.employeeId ASC")
    List<Payroll> filterForClient(@Param("clientId") Long clientId, @Param("month") Integer month, @Param("year") Integer year,
                                   @Param("status") String status, @Param("search") String search);

    // Lightweight companion to filterForClient - just the employee ids already processed for a
    // client+month+year, used by the Run Payroll screen to stop an employee being processed
    // twice in the same month.
    @Query("SELECT p.employeeId FROM Payroll p JOIN Employee e ON e.empId = p.employeeId " +
            "WHERE e.client.id = :clientId AND p.month = :month AND p.year = :year")
    List<Long> findProcessedEmployeeIds(@Param("clientId") Long clientId, @Param("month") Integer month, @Param("year") Integer year);
}
