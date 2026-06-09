package com.employee.management.backend.controller;

import com.employee.management.backend.dto.LeaveBalanceDTO;
import com.employee.management.backend.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping
    public ResponseEntity<List<LeaveBalanceDTO>> getAllLeaveBalances() {
        List<LeaveBalanceDTO> balances = leaveBalanceService.getAllLeaveBalances();
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/{empId}")
    public ResponseEntity<List<LeaveBalanceDTO>> getLeaveBalancesByEmployeeId(@PathVariable Long empId) {
        List<LeaveBalanceDTO> balances = leaveBalanceService.getLeaveBalancesByEmployeeId(empId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/{empId}/{leaveType}")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(
            @PathVariable Long empId,
            @PathVariable String leaveType) {
        try {
            LeaveBalanceDTO balance = leaveBalanceService.getLeaveBalance(empId, leaveType);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{empId}/{leaveType}")
    public ResponseEntity<?> updateLeaveBalance(
            @PathVariable Long empId,
            @PathVariable String leaveType,
            @RequestBody UpdateLeaveBalanceRequest request) {
        try {
            LeaveBalanceDTO updated = leaveBalanceService.updateLeaveBalance(empId, leaveType, request.getBalance());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    static class UpdateLeaveBalanceRequest {
        private Integer balance;

        public UpdateLeaveBalanceRequest() {
        }

        public UpdateLeaveBalanceRequest(Integer balance) {
            this.balance = balance;
        }

        public Integer getBalance() {
            return balance;
        }

        public void setBalance(Integer balance) {
            this.balance = balance;
        }
    }

    static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
