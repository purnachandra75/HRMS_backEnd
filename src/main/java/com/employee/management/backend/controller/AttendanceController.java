package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Attendance;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.Holiday;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.HolidayRepository;
import com.employee.management.backend.security.SecurityUtils;
import com.employee.management.backend.service.AttendanceTimeUtils;
import com.employee.management.backend.service.MissedCheckoutAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final MissedCheckoutAlertService missedCheckoutAlertService;

    public AttendanceController(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
                                 HolidayRepository holidayRepository,
                                 MissedCheckoutAlertService missedCheckoutAlertService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.holidayRepository = holidayRepository;
        this.missedCheckoutAlertService = missedCheckoutAlertService;
    }

    /**
     * The shift the employee is currently in: today's attendance row, or - for a shift that
     * crossed midnight - yesterday's row that was checked in but not checked out, as long as
     * that check-in is still within the missed check-out window (14h by default). Past that
     * window it counts as a missed check-out and today starts fresh.
     */
    private Optional<Attendance> findCurrentShift(Long empId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> todays = attendanceRepository.findByEmployeeEmpIdAndDate(empId, today.toString());
        if (todays.isPresent()) {
            return todays;
        }
        return attendanceRepository.findByEmployeeEmpIdAndDate(empId, today.minusDays(1).toString())
                .filter(att -> att.getCheckOutTime() == null || att.getCheckOutTime().isBlank())
                .filter(att -> {
                    LocalDateTime checkIn = AttendanceTimeUtils.checkInDateTime(att.getDate(), att.getCheckInTime());
                    return checkIn != null && LocalDateTime.now()
                            .isBefore(checkIn.plusHours(missedCheckoutAlertService.getMissedCheckoutHours()));
                });
    }

    @GetMapping
    public List<AttendanceResponse> getAllAttendance(@RequestParam(required = false) Long employeeId,
                                                     @RequestParam(required = false) String month,
                                                     @RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) Integer monthNumber,
                                                     @RequestParam(required = false) String search,
                                                     @RequestParam(required = false) String status) {
        final String normalizedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        final String statusFilter = normalizeStatusFilter(status);
        final Long clientId = SecurityUtils.currentClientId();

        // When a specific year+month is selected, build the report against the
        // month's actual working days (excluding weekends/holidays) so employees
        // who never checked in still show up as ABSENT instead of just vanishing.
        if (year != null && monthNumber != null) {
            return buildMonthlyAttendance(clientId, employeeId, normalizedSearch, year, monthNumber).stream()
                    .filter(record -> matchesStatus(record.getStatus(), statusFilter))
                    .toList();
        }

        List<Attendance> attendanceList = attendanceRepository.findAll();

        return attendanceList.stream()
                .filter(record -> record.getEmployee() != null && belongsToClient(record.getEmployee(), clientId))
                .filter(record -> employeeId == null || record.getEmployee().getEmpId().equals(employeeId))
                .filter(record -> matchesSearch(record.getEmployee(), normalizedSearch))
                .filter(record -> matchesStatus(record.getStatus(), statusFilter))
                .filter(record -> {
                    if (month == null || month.isBlank()) return true;
                    if (record.getDate() == null) return false;
                    return record.getDate().startsWith(month);
                })
                .filter(record -> {
                    if (year == null && monthNumber == null) return true;
                    if (record.getDate() == null || record.getDate().length() < 7) return false;
                    try {
                        int recordYear = Integer.parseInt(record.getDate().substring(0, 4));
                        int recordMonth = Integer.parseInt(record.getDate().substring(5, 7));
                        boolean yearOk = year == null || recordYear == year;
                        boolean monthOk = monthNumber == null || recordMonth == monthNumber;
                        return yearOk && monthOk;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .map(AttendanceResponse::fromAttendance)
                .toList();
    }

    // Keeps one company's admin from seeing another company's attendance. A null clientId
    // (no tenant on the token) leaves the list unfiltered, matching the old behaviour.
    private static boolean belongsToClient(Employee employee, Long clientId) {
        return clientId == null
                || (employee.getClient() != null && clientId.equals(employee.getClient().getId()));
    }

    // status filter: "PRESENT" or "ABSENT" (any case). Blank or "ALL" means no filter.
    private static String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase();
        return normalized.equals("ALL") ? null : normalized;
    }

    private static boolean matchesStatus(String recordStatus, String statusFilter) {
        return statusFilter == null || (recordStatus != null && recordStatus.equalsIgnoreCase(statusFilter));
    }

    // search matches the client-assigned Employee ID (e.g. "EMP001") exactly, ignoring case,
    // or the employee's full name as a substring.
    private static boolean matchesSearch(Employee employee, String search) {
        if (search == null) return true;
        if (employee == null) return false;
        if (employee.getEmployeeId() != null && employee.getEmployeeId().trim().equalsIgnoreCase(search)) {
            return true;
        }
        String fullName = String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim().toLowerCase();
        return fullName.contains(search.toLowerCase());
    }

    /**
     * Builds one row per matched employee per working day of the given month
     * (Mon-Fri, minus holidays, minus days after today), using the real
     * attendance record when the employee checked in and a synthetic ABSENT
     * row otherwise.
     */
    private List<AttendanceResponse> buildMonthlyAttendance(Long clientId, Long employeeId, String search,
                                                              int year, int monthNumber) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, monthNumber);
        } catch (Exception ex) {
            return List.of();
        }

        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate cappedLastDay = lastDay.isAfter(today) ? today : lastDay;
        if (cappedLastDay.isBefore(firstDay)) {
            return List.of();
        }

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(emp -> belongsToClient(emp, clientId))
                .filter(emp -> employeeId == null || employeeId.equals(emp.getEmpId()))
                .filter(emp -> matchesSearch(emp, search))
                .toList();

        if (employees.isEmpty()) {
            return List.of();
        }

        Set<LocalDate> holidayDates = holidayRepository.findByDateBetweenOrderByDateAsc(firstDay, cappedLastDay).stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());

        String monthPrefix = String.format("%04d-%02d", year, monthNumber);
        Map<Long, Map<String, Attendance>> attendanceByEmployee = attendanceRepository.findByDateStartingWith(monthPrefix).stream()
                .filter(record -> record.getEmployee() != null && record.getDate() != null)
                .collect(Collectors.groupingBy(
                        record -> record.getEmployee().getEmpId(),
                        Collectors.toMap(Attendance::getDate, r -> r, (a, b) -> a)));

        List<AttendanceResponse> result = new ArrayList<>();
        for (Employee employee : employees) {
            LocalDate joinDate = parseLocalDate(
                    employee.getJobDetails() != null ? employee.getJobDetails().getDateOfJoining() : null);
            Map<String, Attendance> employeeAttendance = attendanceByEmployee.getOrDefault(employee.getEmpId(), Map.of());

            for (LocalDate day = firstDay; !day.isAfter(cappedLastDay); day = day.plusDays(1)) {
                DayOfWeek dayOfWeek = day.getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) continue;
                if (holidayDates.contains(day)) continue;
                if (joinDate != null && day.isBefore(joinDate)) continue;

                Attendance existing = employeeAttendance.get(day.toString());
                result.add(existing != null
                        ? AttendanceResponse.fromAttendance(existing)
                        : AttendanceResponse.absent(employee, day.toString()));
            }
        }

        result.sort(Comparator.comparing(AttendanceResponse::getDate)
                .thenComparing(r -> r.getEmployeeName() == null ? "" : r.getEmployeeName()));

        return result;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    // With year + month, returns only that month's records (what the employee's monthly report
    // needs); without them, every record for the employee as before.
    @GetMapping("/employee/{empId}")
    public ResponseEntity<?> getAttendanceForEmployee(@PathVariable Long empId,
                                                      @RequestParam(required = false) Integer year,
                                                      @RequestParam(required = false) Integer month,
                                                      @RequestParam(required = false) String status) {
        final String statusFilter = normalizeStatusFilter(status);
        if (year != null && month != null) {
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().body(Map.of("error", "Month must be between 1 and 12"));
            }
            String monthPrefix = String.format("%04d-%02d", year, month);
            return ResponseEntity.ok(attendanceRepository
                    .findByEmployeeEmpIdAndDateStartingWithOrderByDateDesc(empId, monthPrefix).stream()
                    .filter(record -> matchesStatus(record.getStatus(), statusFilter))
                    .map(AttendanceResponse::fromAttendance)
                    .toList());
        }
        return ResponseEntity.ok(attendanceRepository.findByEmployeeEmpIdOrderByIdDesc(empId).stream()
                .filter(record -> matchesStatus(record.getStatus(), statusFilter))
                .map(AttendanceResponse::fromAttendance)
                .toList());
    }

    @GetMapping("/today/{empId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable Long empId) {
        return findCurrentShift(empId)
                .map(att -> ResponseEntity.ok(AttendanceResponse.fromAttendance(att)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> body) {
        try {
            Long empId = Long.valueOf(body.get("employeeId").toString());
            String checkInTime = body.getOrDefault("checkInTime", LocalTime.now().toString()).toString();
            Employee employee = employeeRepository.findById(empId).orElse(null);
            if (employee == null) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));

            String today = LocalDate.now().toString();
            Attendance attendance = attendanceRepository.findByEmployeeEmpIdAndDate(empId, today).orElse(null);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(today);
                attendance.setCheckInTime(checkInTime);
                attendance.setStatus("PRESENT");
                attendanceRepository.save(attendance);
            } else {
                attendance.setCheckInTime(checkInTime);
                attendance.setStatus("PRESENT");
                attendanceRepository.save(attendance);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Checked in successfully");
            resp.put("checkInTime", checkInTime);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
    }

    private static String textValue(Map<String, Object> body, String key) {
        return body.get(key) == null ? "" : body.get(key).toString().trim();
    }

    /**
     * Admin correction of one attendance record (e.g. a missed check-out): change its date,
     * check-in and check-out times and mark it PRESENT or ABSENT. Total hours are recalculated
     * from the times. Only an admin of the employee's own company can do this. Blank fields are
     * left unchanged.
     * Body: { "status": "PRESENT" | "ABSENT", "date": "2026-10-08", "checkInTime": "09:30",
     *         "checkOutTime": "18:30", "remarks": "..." }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Attendance attendance = attendanceRepository.findById(id).orElse(null);
        if (attendance == null || attendance.getEmployee() == null
                || SecurityUtils.currentClientId() == null
                || !belongsToClient(attendance.getEmployee(), SecurityUtils.currentClientId())) {
            return ResponseEntity.status(404).body(Map.of("error", "Attendance record not found"));
        }

        String status = body.get("status") == null ? "" : body.get("status").toString().trim().toUpperCase();
        if (!status.equals("PRESENT") && !status.equals("ABSENT")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status must be PRESENT or ABSENT"));
        }
        String date = textValue(body, "date");
        String checkInTime = textValue(body, "checkInTime");
        String checkOutTime = textValue(body, "checkOutTime");
        String remarks = textValue(body, "remarks");

        // Date: optional; must be a real day, not in the future, and not a day that already has
        // another attendance record for this employee.
        if (!date.isEmpty() && !date.equals(attendance.getDate())) {
            LocalDate newDate;
            try {
                newDate = LocalDate.parse(date);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid date"));
            }
            if (newDate.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Date cannot be in the future"));
            }
            boolean taken = attendanceRepository
                    .findByEmployeeEmpIdAndDate(attendance.getEmployee().getEmpId(), newDate.toString())
                    .filter(other -> !other.getId().equals(attendance.getId()))
                    .isPresent();
            if (taken) {
                return ResponseEntity.status(409).body(Map.of("error", "This employee already has attendance on " + newDate));
            }
            attendance.setDate(newDate.toString());
        }

        if (!checkInTime.isEmpty()) {
            if (AttendanceTimeUtils.parseTime(checkInTime) == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid check-in time"));
            }
            attendance.setCheckInTime(checkInTime);
        }
        if (!checkOutTime.isEmpty()) {
            if (AttendanceTimeUtils.parseTime(checkOutTime) == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid check-out time"));
            }
            attendance.setCheckOutTime(checkOutTime);
        }

        if (status.equals("ABSENT")) {
            attendance.setTotalHours(null);
        } else {
            if (attendance.getCheckInTime() == null || attendance.getCheckInTime().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Check-in time is required for a present day"));
            }
            LocalDateTime in = AttendanceTimeUtils.checkInDateTime(attendance.getDate(), attendance.getCheckInTime());
            LocalTime outTime = AttendanceTimeUtils.parseTime(attendance.getCheckOutTime());
            if (in != null && outTime != null) {
                // Same day as the check-in, or the next morning for a shift that crossed midnight.
                LocalDateTime out = LocalDateTime.of(in.toLocalDate(), outTime);
                if (!out.isAfter(in)) {
                    out = out.plusDays(1);
                }
                attendance.setTotalHours(AttendanceTimeUtils.hoursBetween(in, out));
            }
        }

        attendance.setStatus(status);
        attendance.setRemarks(remarks.isEmpty() ? "Updated by admin" : remarks);
        // Already dealt with by HR, so the missed check-out email shouldn't go out for it.
        attendance.setMissedCheckoutAlertSent(true);
        attendanceRepository.save(attendance);
        return ResponseEntity.ok(AttendanceResponse.fromAttendance(attendance));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, Object> body) {
        try {
            Long empId = Long.valueOf(body.get("employeeId").toString());
            String checkOutTime = body.getOrDefault("checkOutTime", LocalTime.now().toString()).toString();

            Attendance attendance = findCurrentShift(empId).orElse(null);
            if (attendance == null) return ResponseEntity.badRequest().body(Map.of("error", "No check-in record for today"));

            // Total hours = check-out minus check-in. Check-out is always today, while the
            // check-in may be on the previous day for a shift that crossed midnight.
            LocalDateTime in = AttendanceTimeUtils.checkInDateTime(attendance.getDate(), attendance.getCheckInTime());
            LocalTime outTime = AttendanceTimeUtils.parseTime(checkOutTime);
            if (in != null && outTime != null) {
                LocalDateTime out = LocalDateTime.of(LocalDate.now(), outTime);
                if (!out.isAfter(in)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Check-out time must be after check-in time"));
                }
                attendance.setTotalHours(AttendanceTimeUtils.hoursBetween(in, out));
            }
            attendance.setCheckOutTime(checkOutTime);

            attendanceRepository.save(attendance);

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Checked out successfully");
            resp.put("checkOutTime", checkOutTime);
            resp.put("totalHours", attendance.getTotalHours());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
    }

    public static class AttendanceResponse {
        private Long id;
        private Long employeeId;
        // Client-assigned Employee ID (e.g. "EMP001") shown in the UI; employeeId is the internal key.
        private String employeeCode;
        private String employeeName;
        private String date;
        private String checkInTime;
        private String checkOutTime;
        private Double totalHours;
        private String status;
        private String remarks;

        public AttendanceResponse() {
        }

        public static AttendanceResponse fromAttendance(Attendance attendance) {
            AttendanceResponse response = new AttendanceResponse();
            response.id = attendance.getId();
            response.employeeId = attendance.getEmployee() != null ? attendance.getEmployee().getEmpId() : null;
            if (attendance.getEmployee() != null) {
                response.employeeCode = attendance.getEmployee().getEmployeeId();
                response.employeeName = String.format("%s %s",
                        attendance.getEmployee().getFirstName() == null ? "" : attendance.getEmployee().getFirstName(),
                        attendance.getEmployee().getLastName() == null ? "" : attendance.getEmployee().getLastName()).trim();
            }
            response.date = attendance.getDate();
            response.checkInTime = attendance.getCheckInTime();
            response.checkOutTime = attendance.getCheckOutTime();
            response.totalHours = attendance.getTotalHours();
            response.status = attendance.getStatus();
            response.remarks = attendance.getRemarks();
            return response;
        }

        public static AttendanceResponse absent(Employee employee, String date) {
            AttendanceResponse response = new AttendanceResponse();
            response.employeeId = employee.getEmpId();
            response.employeeCode = employee.getEmployeeId();
            response.employeeName = String.format("%s %s",
                    employee.getFirstName() == null ? "" : employee.getFirstName(),
                    employee.getLastName() == null ? "" : employee.getLastName()).trim();
            response.date = date;
            response.status = "ABSENT";
            return response;
        }

        public Long getId() {
            return id;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public String getDate() {
            return date;
        }

        public String getCheckInTime() {
            return checkInTime;
        }

        public String getCheckOutTime() {
            return checkOutTime;
        }

        public Double getTotalHours() {
            return totalHours;
        }

        public String getStatus() {
            return status;
        }

        public String getRemarks() {
            return remarks;
        }
    }
}
