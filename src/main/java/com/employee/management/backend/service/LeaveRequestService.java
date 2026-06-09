package com.employee.management.backend.service;

import com.employee.management.backend.dto.CreateLeaveRequestDTO;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.model.Employee;
import com.employee.management.backend.model.LeaveBalance;
import com.employee.management.backend.model.LeaveHistory;
import com.employee.management.backend.model.LeaveRequest;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.repository.LeaveHistoryRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveHistoryRepository leaveHistoryRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveHistoryRepository leaveHistoryRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveHistoryRepository = leaveHistoryRepository;
    }

    public LeaveRequestDTO createLeaveRequest(CreateLeaveRequestDTO requestDTO) {
    	System.out.println(requestDTO.getEmpId());
    	System.out.println(employeeRepository.findById(requestDTO.getEmpId()));
        Employee employee = employeeRepository.findById(requestDTO.getEmpId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", requestDTO.getEmpId()));

        // Parse dates
        LocalDate fromDate = LocalDate.parse(requestDTO.getFromDate(), DATE_FORMATTER);
        LocalDate toDate = LocalDate.parse(requestDTO.getToDate(), DATE_FORMATTER);

        // Calculate days (inclusive)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (daysBetween <= 0) {
            throw new RuntimeException("Invalid date range");
        }

        // Use provided year or default to current year
        Integer year = requestDTO.getYear();
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        // Check leave balance
        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByEmployeeEmpIdAndLeaveType(requestDTO.getEmpId(), requestDTO.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found for this leave type"));

        if (leaveBalance.getBalance() < daysBetween) {
            throw new RuntimeException("Insufficient leave balance. Available: " + leaveBalance.getBalance() + 
                                     " days, Requested: " + daysBetween + " days");
        }

        // Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(requestDTO.getLeaveType());
        leaveRequest.setDays((int) daysBetween);
        leaveRequest.setYear(year);
        leaveRequest.setFromDate(fromDate);
        leaveRequest.setToDate(toDate);
        leaveRequest.setCreatedAt(LocalDate.now());
        leaveRequest.setReason(requestDTO.getReason());
        leaveRequest.setStatus("Pending");

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return convertToDTO(savedRequest);
    }

    public List<LeaveRequestDTO> getAllLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getLeaveRequestsByEmployeeId(Long empId) {
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(empId);
        return requests.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public LeaveRequestDTO updateLeaveRequestStatus(Long requestId, UpdateLeaveRequestStatusDTO statusDTO) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));

        String oldStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(statusDTO.getStatus());

        // If approved, deduct from leave balance
        if ("Approved".equals(statusDTO.getStatus()) && !"Approved".equals(oldStatus)) {
            LeaveBalance leaveBalance = leaveBalanceRepository
                    .findByEmployeeEmpIdAndLeaveType(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            leaveBalance.setBalance(leaveBalance.getBalance() - leaveRequest.getDays());
            leaveBalanceRepository.save(leaveBalance);

            // Create leave history entry
            LeaveHistory history = new LeaveHistory(
                    leaveRequest.getEmployee(),
                    leaveRequest.getLeaveType(),
                    leaveRequest.getDays(),
                    leaveRequest.getYear()
            );
            leaveHistoryRepository.save(history);
        }

        // If rejected or cancelled, restore leave balance
        if ("Rejected".equals(statusDTO.getStatus()) && "Approved".equals(oldStatus)) {
            LeaveBalance leaveBalance = leaveBalanceRepository
                    .findByEmployeeEmpIdAndLeaveType(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            leaveBalance.setBalance(leaveBalance.getBalance() + leaveRequest.getDays());
            leaveBalanceRepository.save(leaveBalance);
        }

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return convertToDTO(updatedRequest);
    }

    public LeaveRequestDTO getLeaveRequestById(Long requestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));
        return convertToDTO(leaveRequest);
    }

    public List<LeaveRequestDTO> getLeaveRequestsByStatus(String status) {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(req -> req.getStatus().equals(status))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public com.employee.management.backend.dto.LeaveReportDTO getLeaveReport(Long empId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));

        com.employee.management.backend.dto.LeaveReportDTO report = new com.employee.management.backend.dto.LeaveReportDTO(
                employee.getEmpId(),
                employee.getFirstName() + " " + employee.getLastName()
        );

        // Get all leave balances for this employee
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeEmpId(empId);
        for (LeaveBalance balance : balances) {
            report.addLeaveType(balance.getLeaveType(), balance.getBalance());
        }

        return report;
    }

    private LeaveRequestDTO convertToDTO(LeaveRequest leaveRequest) {
        return new LeaveRequestDTO(
                leaveRequest.getId(),
                leaveRequest.getEmployee().getEmpId(),
                leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getDays(),
                leaveRequest.getYear(),
                leaveRequest.getCreatedAt().toString(),
                leaveRequest.getFromDate().toString(),
                leaveRequest.getToDate().toString(),
                leaveRequest.getStatus(),
                leaveRequest.getReason()
        );
    }
}
