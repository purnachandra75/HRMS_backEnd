package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.Entity.LeaveHistory;
import com.employee.management.backend.Entity.LeaveRequest;
import com.employee.management.backend.dto.CreateLeaveRequestDTO;
import com.employee.management.backend.dto.LeaveRequestDTO;
import com.employee.management.backend.dto.UpdateLeaveRequestStatusDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import com.employee.management.backend.repository.LeaveHistoryRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import com.employee.management.backend.repository.HolidayRepository;
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
    private final HolidayRepository holidayRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveHistoryRepository leaveHistoryRepository,
                               HolidayRepository holidayRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveHistoryRepository = leaveHistoryRepository;
        this.holidayRepository = holidayRepository;
    }

    public LeaveRequestDTO createLeaveRequest(CreateLeaveRequestDTO requestDTO) {
    	System.out.println(requestDTO.getEmpId());
    	System.out.println(employeeRepository.findById(requestDTO.getEmpId()));
        Employee employee = employeeRepository.findById(requestDTO.getEmpId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", requestDTO.getEmpId()));

        // Parse dates
        LocalDate fromDate = LocalDate.parse(requestDTO.getFromDate(), DATE_FORMATTER);
        LocalDate toDate = LocalDate.parse(requestDTO.getToDate(), DATE_FORMATTER);

        // Calculate calendar days (inclusive) and working days (exclude weekends and holidays)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (daysBetween <= 0) {
            throw new RuntimeException("Invalid date range");
        }

        int workingDays = calculateWorkingDays(fromDate, toDate);

        // Use provided year or default to current year
        Integer year = requestDTO.getYear();
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        // Check leave balance against working days
        LeaveBalance leaveBalance = leaveBalanceRepository
            .findByEmployeeEmpIdAndLeaveType(requestDTO.getEmpId(), requestDTO.getLeaveType())
            .orElseThrow(() -> new RuntimeException("Leave balance not found for this leave type"));

        if (leaveBalance.getBalance() < workingDays) {
            throw new RuntimeException("Insufficient leave balance. Available: " + leaveBalance.getBalance() +
                " days, Requested: " + workingDays + " working days");
        }

        // Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(requestDTO.getLeaveType());
        leaveRequest.setDays(workingDays);
        leaveRequest.setYear(year);
        leaveRequest.setFromDate(fromDate);
        leaveRequest.setToDate(toDate);
        leaveRequest.setCreatedAt(LocalDate.now());
        leaveRequest.setReason(requestDTO.getReason());
        leaveRequest.setStatus("Pending");

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return convertToDTO(savedRequest);
    }

    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) return 0;
        // fetch holidays in range
        List<com.employee.management.backend.Entity.Holiday> holidays = holidayRepository.findByDateBetweenOrderByDateAsc(start, end);
        java.util.Set<LocalDate> holidayDates = holidays.stream().map(com.employee.management.backend.Entity.Holiday::getDate).collect(java.util.stream.Collectors.toSet());

        int workingDays = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            boolean isWeekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(d);
            if (!isWeekend && !isHoliday) workingDays++;
            d = d.plusDays(1);
        }
        return workingDays;
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

        // preserve original days in case we need to restore
        Integer originalDays = leaveRequest.getDays();

        // compute working days from dates (exclude weekends and holidays)
        int workingDaysFromDates = calculateWorkingDays(leaveRequest.getFromDate(), leaveRequest.getToDate());

        // Prefer the days already stored on the leave request (created working days).
        // If missing, fall back to computed working days. Ignore any incorrect `days` value sent in DTO.
        int effectiveDays = (leaveRequest.getDays() != null && leaveRequest.getDays() > 0) ? leaveRequest.getDays() : workingDaysFromDates;
        leaveRequest.setDays(effectiveDays);

        leaveRequest.setStatus(statusDTO.getStatus());

        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByEmployeeEmpIdAndLeaveType(leaveRequest.getEmployee().getEmpId(), leaveRequest.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        // Approve flow
        if ("Approved".equals(statusDTO.getStatus())) {
            // Transition from non-approved -> approved: deduct full days
            if (!"Approved".equals(oldStatus)) {
                int deduct = effectiveDays;
                if (leaveBalance.getBalance() < deduct) {
                    throw new RuntimeException("Insufficient leave balance. Available: " + leaveBalance.getBalance() + " days, Requested: " + deduct + " days");
                }
                leaveBalance.setBalance(leaveBalance.getBalance() - deduct);
                leaveBalanceRepository.save(leaveBalance);

                LeaveHistory history = new LeaveHistory(
                        leaveRequest.getEmployee(),
                        leaveRequest.getLeaveType(),
                        deduct,
                        leaveRequest.getYear()
                );
                leaveHistoryRepository.save(history);
            } else {
                // Already approved and still approved: adjust by delta if days changed
                int newDays = effectiveDays;
                int delta = newDays - (originalDays == null ? 0 : originalDays);
                if (delta > 0) {
                    if (leaveBalance.getBalance() < delta) {
                        throw new RuntimeException("Insufficient leave balance for adjustment. Available: " + leaveBalance.getBalance() + " days, Additional requested: " + delta + " days");
                    }
                    leaveBalance.setBalance(leaveBalance.getBalance() - delta);
                    leaveBalanceRepository.save(leaveBalance);

                    LeaveHistory history = new LeaveHistory(
                            leaveRequest.getEmployee(),
                            leaveRequest.getLeaveType(),
                            delta,
                            leaveRequest.getYear()
                    );
                    leaveHistoryRepository.save(history);
                } else if (delta < 0) {
                    // reduction in approved days -> restore balance
                    leaveBalance.setBalance(leaveBalance.getBalance() + (-delta));
                    leaveBalanceRepository.save(leaveBalance);
                }
            }
        }

        // Rejection or cancellation: if it was previously approved, restore original days
        if (("Rejected".equals(statusDTO.getStatus()) || "Cancelled".equals(statusDTO.getStatus())) && "Approved".equals(oldStatus)) {
            int restore = originalDays == null ? 0 : originalDays;
            leaveBalance.setBalance(leaveBalance.getBalance() + restore);
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
