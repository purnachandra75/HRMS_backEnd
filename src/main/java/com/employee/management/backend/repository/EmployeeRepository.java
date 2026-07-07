package com.employee.management.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Employee;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);

    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "(:search IS NULL OR LOWER(e.firstName) LIKE :search OR LOWER(e.lastName) LIKE :search OR LOWER(COALESCE(jd.designation, '')) LIKE :search) AND " +
            "(:department IS NULL OR :department = '' OR TRIM(LOWER(COALESCE(jd.department, ''))) = TRIM(LOWER(:department))) AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) = TRIM(LOWER(:status))) AND " +
            "((:status IS NOT NULL AND TRIM(LOWER(:status)) = 'inactive') OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive') AND " +
            "(:employeeType IS NULL OR :employeeType = '' OR TRIM(LOWER(COALESCE(jd.employeeType, ''))) = TRIM(LOWER(:employeeType)))")
    Page<Employee> searchEmployees(@Param("search") String search,
                                   @Param("department") String department,
                                   @Param("status") String status,
                                   @Param("employeeType") String employeeType,
                                   Pageable pageable);

    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "(:department IS NULL OR :department = '' OR TRIM(LOWER(COALESCE(jd.department, ''))) = TRIM(LOWER(:department))) AND " +
            "(:status IS NULL OR :status = '' OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) = TRIM(LOWER(:status))) AND " +
            "((:status IS NOT NULL AND TRIM(LOWER(:status)) = 'inactive') OR TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive')")
    Page<Employee> filterEmployees(@Param("department") String department,
                                    @Param("status") String status,
                                    Pageable pageable);

        @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive'")
    Page<Employee> findAllActive(Pageable pageable);

        @Query("SELECT COUNT(e) FROM Employee e LEFT JOIN e.jobDetails jd WHERE TRIM(LOWER(COALESCE(jd.employeeStatus, ''))) <> 'inactive'")
    long countActive();
}
