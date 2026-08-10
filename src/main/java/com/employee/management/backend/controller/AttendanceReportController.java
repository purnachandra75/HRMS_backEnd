package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Attendance;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single consolidated endpoint for the Reports module's Attendance Report,
 * so the frontend doesn't need to separately call /api/employees and
 * /api/attendance and join them client-side.
 */
@RestController
@RequestMapping("/api/reports/attendance")
public class AttendanceReportController {

    private final EmployeeService employeeService;
    private final AttendanceRepository attendanceRepository;

    public AttendanceReportController(EmployeeService employeeService, AttendanceRepository attendanceRepository) {
        this.employeeService = employeeService;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public Page<AttendanceReportRowDTO> getAttendanceReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam int year,
            @RequestParam int month) {

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(size, 1);
        String normalizedDepartment = normalizeFilterValue(department);
        String normalizedStatus = normalizeFilterValue(status);

        Page<Employee> employeePage = employeeService.filterEmployees(
                normalizedDepartment, normalizedStatus, PageRequest.of(normalizedPage, normalizedSize));

        String monthPrefix = String.format("%04d-%02d", year, month);
        List<Attendance> monthAttendance = attendanceRepository.findByDateStartingWith(monthPrefix);

        Map<Long, Long> presentDaysByEmployee = monthAttendance.stream()
                .filter(record -> "PRESENT".equals(record.getStatus()) && record.getEmployee() != null)
                .collect(Collectors.groupingBy(record -> record.getEmployee().getEmpId(), Collectors.counting()));

        int workingDays = calculateWorkingDays(year, month);
        String monthLabel = YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        List<AttendanceReportRowDTO> content = employeePage.getContent().stream()
                .map(employee -> {
                    AttendanceReportRowDTO dto = new AttendanceReportRowDTO();
                    dto.setEmpId(employee.getEmpId());
                    dto.setFullName(String.format("%s %s",
                            employee.getFirstName() == null ? "" : employee.getFirstName(),
                            employee.getLastName() == null ? "" : employee.getLastName()).trim());
                    dto.setDepartment(employee.getJobDetails() != null ? employee.getJobDetails().getDepartment() : null);
                    dto.setMonth(monthLabel);
                    dto.setPresentDays(presentDaysByEmployee.getOrDefault(employee.getEmpId(), 0L).intValue());
                    dto.setWorkingDays(workingDays);
                    return dto;
                })
                .toList();

        return new PageImpl<>(content, employeePage.getPageable(), employeePage.getTotalElements());
    }

    private int calculateWorkingDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int total = 0;
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            DayOfWeek dayOfWeek = yearMonth.atDay(day).getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    public static class AttendanceReportRowDTO {
        private Long empId;
        private String fullName;
        private String department;
        private String month;
        private int presentDays;
        private int workingDays;

        public Long getEmpId() {
            return empId;
        }

        public void setEmpId(Long empId) {
            this.empId = empId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public int getPresentDays() {
            return presentDays;
        }

        public void setPresentDays(int presentDays) {
            this.presentDays = presentDays;
        }

        public int getWorkingDays() {
            return workingDays;
        }

        public void setWorkingDays(int workingDays) {
            this.workingDays = workingDays;
        }
    }
}
