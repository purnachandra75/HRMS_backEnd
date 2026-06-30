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
            "LOWER(e.firstName) LIKE :search OR " +
            "LOWER(e.lastName) LIKE :search OR " +
            "LOWER(COALESCE(jd.designation, '')) LIKE :search")
    Page<Employee> searchEmployees(@Param("search") String search, Pageable pageable);
}
