package com.employee.management.backend.controller;

import com.employee.management.backend.dto.PayrollProcessRequestDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import com.employee.management.backend.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestBody PayrollProcessRequestDTO request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
            }
            if (request.getEmployees() == null || request.getEmployees().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Employees are required"));
            }
            PayrollProcessResponseDTO response = payrollService.processPayroll(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
