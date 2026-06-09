package com.employee.management.backend.controller;

import com.employee.management.backend.dto.CreateLeaveRequestDTO;
import com.employee.management.backend.dto.LeaveReportDTO;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.service.LeaveRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaveRequests() {
        List<LeaveRequestDTO> requests = leaveRequestService.getAllLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeaveRequestsByEmployeeId(@PathVariable Long empId) {
        List<LeaveRequestDTO> requests = leaveRequestService.getLeaveRequestsByEmployeeId(empId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<LeaveRequestDTO> getLeaveRequestById(@PathVariable Long requestId) {
        LeaveRequestDTO request = leaveRequestService.getLeaveRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeaveRequestsByStatus(@PathVariable String status) {
        List<LeaveRequestDTO> requests = leaveRequestService.getLeaveRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/report/{empId}")
    public ResponseEntity<?> getLeaveReport(@PathVariable Long empId) {
        try {
            LeaveReportDTO report = leaveRequestService.getLeaveReport(empId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLeaveRequest(@RequestBody CreateLeaveRequestDTO requestDTO) {
        try {
            LeaveRequestDTO createdRequest = leaveRequestService.createLeaveRequest(requestDTO);
            return ResponseEntity.status(201).body(createdRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{requestId}/status")
    public ResponseEntity<?> updateLeaveRequestStatus(
            @PathVariable Long requestId,
            @RequestBody UpdateLeaveRequestStatusDTO statusDTO) {
        try {
            LeaveRequestDTO updatedRequest = leaveRequestService.updateLeaveRequestStatus(requestId, statusDTO);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
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
