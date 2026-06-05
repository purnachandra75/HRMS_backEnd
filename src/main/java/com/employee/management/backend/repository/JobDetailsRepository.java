package com.employee.management.backend.repository;

import com.employee.management.backend.model.JobDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDetailsRepository extends JpaRepository<JobDetails, Long> {
}
