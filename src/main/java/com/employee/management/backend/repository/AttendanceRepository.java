package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Attendance;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployeeEmpIdOrderByIdDesc(Long empId);
    // One employee's records for one month - datePrefix is "yyyy-MM" (dates are stored yyyy-MM-dd).
    List<Attendance> findByEmployeeEmpIdAndDateStartingWithOrderByDateDesc(Long empId, String datePrefix);
    Optional<Attendance> findByEmployeeEmpIdAndDate(Long empId, String date);
    List<Attendance> findByDateStartingWith(String datePrefix);

    // Checked in but never checked out, on or after fromDate (yyyy-MM-dd compares correctly as a
    // string), and not yet warned about - candidates for the missed check-out email.
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee e LEFT JOIN FETCH e.client WHERE " +
            "a.date >= :fromDate AND a.checkInTime IS NOT NULL AND a.checkInTime <> '' AND " +
            "(a.checkOutTime IS NULL OR a.checkOutTime = '') AND " +
            "(a.missedCheckoutAlertSent IS NULL OR a.missedCheckoutAlertSent = false)")
    List<Attendance> findOpenWithoutMissedCheckoutAlert(@Param("fromDate") String fromDate);
}
