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
<<<<<<< HEAD
            "LOWER(e.firstName) LIKE :search OR " +
            "LOWER(e.lastName) LIKE :search OR " +
            "LOWER(COALESCE(jd.designation, '')) LIKE :search")
    Page<Employee> searchEmployees(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Employee e LEFT JOIN e.jobDetails jd WHERE " +
            "(:department = '' OR LOWER(COALESCE(jd.department, '')) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
            "(:status = '' OR LOWER(COALESCE(jd.employeeStatus, '')) LIKE LOWER(CONCAT('%', :status, '%')))" )
    Page<Employee> filterEmployees(@Param("department") String department,
                                    @Param("status") String status,
                                    Pageable pageable);
=======
            "(:search IS NULL OR LOWER(e.firstName) LIKE :search OR LOWER(e.lastName) LIKE :search OR LOWER(COALESCE(jd.designation, '')) LIKE :search) AND " +
            "(:department IS NULL OR :department = '' OR LOWER(COALESCE(jd.department, '')) = LOWER(:department)) AND " +
            "(:status IS NULL OR :status = '' OR LOWER(COALESCE(jd.employeeStatus, '')) = LOWER(:status)) AND " +
            "(:employeeType IS NULL OR :employeeType = '' OR LOWER(COALESCE(jd.employeeType, '')) = LOWER(:employeeType))")
    Page<Employee> searchEmployees(@Param("search") String search,
                                   @Param("department") String department,
                                   @Param("status") String status,
                                   @Param("employeeType") String employeeType,
                                   Pageable pageable);
>>>>>>> 66e0f715eb692c643a365b43c3a0ce851805ea94
}
