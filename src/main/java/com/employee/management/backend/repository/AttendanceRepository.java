package com.employee.management.backend.repository;

import com.employee.management.backend.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployeeEmpIdOrderByIdDesc(Long empId);
    Optional<Attendance> findByEmployeeEmpIdAndDate(Long empId, String date);
}
