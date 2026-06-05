package com.employee.management.backend.repository;

import com.employee.management.backend.model.AddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressDetailsRepository extends JpaRepository<AddressDetails, Long> {
}
