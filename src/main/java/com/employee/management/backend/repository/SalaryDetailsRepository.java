package com.employee.management.backend.repository;

import com.employee.management.backend.model.SalaryDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryDetailsRepository extends JpaRepository<SalaryDetails, Long> {
}
