package com.employee.management.backend.repository;

import com.employee.management.backend.Entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByDateAndTitle(LocalDate date, String title);

    @Query("SELECT h FROM Holiday h WHERE FUNCTION('YEAR', h.date) = :year ORDER BY h.date ASC")
    List<Holiday> findByYear(@Param("year") Integer year);

    @Query("SELECT DISTINCT FUNCTION('YEAR', h.date) FROM Holiday h ORDER BY FUNCTION('YEAR', h.date) DESC")
    List<Integer> findDistinctYears();

    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    List<Holiday> findAllByOrderByDateAsc();
}
