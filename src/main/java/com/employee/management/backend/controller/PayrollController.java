package com.employee.management.backend.controller;

import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.dto.PayrollProcessRequestDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import com.employee.management.backend.security.AuthenticatedUser;
import com.employee.management.backend.service.PayrollExcelService;
import com.employee.management.backend.service.PayrollService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollExcelService payrollExcelService;

    public PayrollController(PayrollService payrollService,
                             PayrollExcelService payrollExcelService) {
        this.payrollService = payrollService;
        this.payrollExcelService = payrollExcelService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestBody PayrollProcessRequestDTO request) {
        try {
            return processPayrollAndBuildExcelResponse(request);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/process/excel")
    public ResponseEntity<?> processPayrollExcel(@RequestBody PayrollProcessRequestDTO request) {
        try {
            return processPayrollAndBuildExcelResponse(request);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/process")
    public ResponseEntity<?> getProcessedPayrollExcel(@RequestParam Integer month,
                                                      @RequestParam Integer year) {
        try {
            ResponseEntity<?> validationError = validateMonth(month);
            if (validationError != null) return validationError;

            PayrollProcessResponseDTO response = payrollService.getProcessedPayrollByMonthAndYear(month, year);
            return buildExcelResponse(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping({"", "/report", "/reports"})
    public ResponseEntity<?> getPayrollReport(@RequestParam Integer month,
                                              @RequestParam Integer year) {
        return buildReportResponse(month, year);
    }

    @GetMapping({"/{year}/{month}", "/report/{year}/{month}"})
    public ResponseEntity<?> getPayrollReportByPath(@PathVariable Integer year,
                                                    @PathVariable Integer month) {
        return buildReportResponse(month, year);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{payrollId}/status")
    public ResponseEntity<?> updatePayrollStatus(@PathVariable Long payrollId,
                                                 @RequestBody PayrollStatusUpdateRequest request) {
        return buildStatusUpdateResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{payrollId}/status")
    public ResponseEntity<?> patchPayrollStatus(@PathVariable Long payrollId,
                                                @RequestBody PayrollStatusUpdateRequest request) {
        return buildStatusUpdateResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping({"/report/{payrollId}/status", "/reports/{payrollId}/status"})
    public ResponseEntity<?> updatePayrollReportStatus(@PathVariable Long payrollId,
                                                       @RequestBody PayrollStatusUpdateRequest request) {
        return buildStatusUpdateResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping({"/report/{payrollId}/status", "/reports/{payrollId}/status"})
    public ResponseEntity<?> patchPayrollReportStatus(@PathVariable Long payrollId,
                                                      @RequestBody PayrollStatusUpdateRequest request) {
        return buildStatusUpdateResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{payrollId}/payslip-mode")
    public ResponseEntity<?> updatePayslipMode(@PathVariable Long payrollId,
                                               @RequestBody PayslipModeUpdateRequest request) {
        return buildPayslipModeResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{payrollId}/payslip-mode")
    public ResponseEntity<?> patchPayslipMode(@PathVariable Long payrollId,
                                              @RequestBody PayslipModeUpdateRequest request) {
        return buildPayslipModeResponse(payrollId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/{payrollId}/payslip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadManualPayslip(@PathVariable Long payrollId,
                                                 @RequestParam("file") MultipartFile file) {
        try {
            payrollService.uploadManualPayslip(payrollId, file);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{payrollId}/payslip")
    public ResponseEntity<?> downloadManualPayslip(@PathVariable Long payrollId,
                                                    Authentication authentication) {
        AuthenticatedUser requester = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user
                : null;
        try {
            DocumentFile payslip = payrollService.getManualPayslip(payrollId, requester);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payslip.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(payslip.getContentType()))
                    .contentLength(payslip.getSize())
                    .body(payslip.getData());
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> buildPayslipModeResponse(Long payrollId, PayslipModeUpdateRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
            }

            return ResponseEntity.ok(payrollService.updatePayslipMode(
                    payrollId,
                    request.getEmployeeId(),
                    request.getMonth(),
                    request.getYear(),
                    Boolean.TRUE.equals(request.getManualPayslip())
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> processPayrollAndBuildExcelResponse(PayrollProcessRequestDTO request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
        }
        if (request.getEmployees() == null || request.getEmployees().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employees are required"));
        }

        PayrollProcessResponseDTO response = payrollService.processPayroll(request);
        return buildExcelResponse(response);
    }

    private ResponseEntity<?> buildStatusUpdateResponse(Long payrollId, PayrollStatusUpdateRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
            }

            ResponseEntity<?> validationError = validateMonth(request.getMonth());
            if (validationError != null) return validationError;

            return ResponseEntity.ok(payrollService.updatePayrollCreditStatus(
                    payrollId,
                    request.getEmployeeId(),
                    request.getMonth(),
                    request.getYear(),
                    request.getStatus()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> buildReportResponse(Integer month, Integer year) {
        try {
            ResponseEntity<?> validationError = validateMonth(month);
            if (validationError != null) return validationError;

            PayrollProcessResponseDTO response = payrollService.getProcessedPayrollByMonthAndYear(month, year);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ResponseEntity<?> validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "Month must be between 1 and 12"));
        }
        return null;
    }

    private ResponseEntity<byte[]> buildExcelResponse(PayrollProcessResponseDTO response) {
        byte[] excel = payrollExcelService.buildPayrollExcel(response);
        String filename = String.format("payroll-%s-%s.xlsx", response.getMonth(), response.getYear());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    public static class PayrollStatusUpdateRequest {
        private Long employeeId;
        private Integer month;
        private Integer year;
        private String status;

        public PayrollStatusUpdateRequest() {
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class PayslipModeUpdateRequest {
        private Long employeeId;
        private Integer month;
        private Integer year;
        private Boolean manualPayslip;

        public PayslipModeUpdateRequest() {
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public Boolean getManualPayslip() {
            return manualPayslip;
        }

        public void setManualPayslip(Boolean manualPayslip) {
            this.manualPayslip = manualPayslip;
        }
    }
}
