package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Attendance;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public List<AttendanceResponse> getAllAttendance(@RequestParam(required = false) Long employeeId,
                                                     @RequestParam(required = false) String month) {
        List<Attendance> attendanceList = attendanceRepository.findAll();
        return attendanceList.stream()
                .filter(record -> employeeId == null || record.getEmployee().getEmpId().equals(employeeId))
                .filter(record -> {
                    if (month == null || month.isBlank()) return true;
                    if (record.getDate() == null) return false;
                    return record.getDate().startsWith(month);
                })
                .map(AttendanceResponse::fromAttendance)
                .toList();
    }

    @GetMapping("/employee/{empId}")
    public List<AttendanceResponse> getAttendanceForEmployee(@PathVariable Long empId) {
        return attendanceRepository.findByEmployeeEmpIdOrderByIdDesc(empId).stream()
                .map(AttendanceResponse::fromAttendance)
                .toList();
    }

    @GetMapping("/today/{empId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable Long empId) {
        String today = LocalDate.now().toString();
        return attendanceRepository.findByEmployeeEmpIdAndDate(empId, today)
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

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, Object> body) {
        try {
            Long empId = Long.valueOf(body.get("employeeId").toString());
            String checkOutTime = body.getOrDefault("checkOutTime", LocalTime.now().toString()).toString();

            String today = LocalDate.now().toString();
            Attendance attendance = attendanceRepository.findByEmployeeEmpIdAndDate(empId, today).orElse(null);
            if (attendance == null) return ResponseEntity.badRequest().body(Map.of("error", "No check-in record for today"));

            attendance.setCheckOutTime(checkOutTime);

            // compute total hours if checkIn exists
            if (attendance.getCheckInTime() != null && !attendance.getCheckInTime().isEmpty()) {
                try {
                    DateTimeFormatter[] formats = new DateTimeFormatter[] {
                            DateTimeFormatter.ofPattern("HH:mm:ss"),
                            DateTimeFormatter.ofPattern("HH:mm")
                    };
                    LocalTime in = null;
                    LocalTime out = null;
                    for (DateTimeFormatter f : formats) {
                        try { in = LocalTime.parse(attendance.getCheckInTime(), f); break; } catch (Exception e) { }
                    }
                    for (DateTimeFormatter f : formats) {
                        try { out = LocalTime.parse(checkOutTime, f); break; } catch (Exception e) { }
                    }
                    if (in != null && out != null) {
                        long minutes = ChronoUnit.MINUTES.between(in, out);
                        double hours = Math.round((minutes / 60.0) * 100.0) / 100.0;
                        attendance.setTotalHours(hours);
                    }
                } catch (Exception e) {
                    // ignore parsing errors
                }
            }

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

        public Long getId() {
            return id;
        }

        public Long getEmployeeId() {
            return employeeId;
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
