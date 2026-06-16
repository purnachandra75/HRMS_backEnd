package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveRequest;
import com.employee.management.backend.Entity.SalaryDetails;
import com.employee.management.backend.dto.PayrollEmployeeRequestDTO;
import com.employee.management.backend.dto.PayrollEmployeeResponseDTO;
import com.employee.management.backend.dto.PayrollProcessRequestDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public PayrollService(EmployeeRepository employeeRepository,
                          LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional(readOnly = true)
    public PayrollProcessResponseDTO processPayroll(PayrollProcessRequestDTO request) {
        YearMonth payrollMonth = resolvePayrollMonth(request);
        PayrollProcessResponseDTO response = new PayrollProcessResponseDTO();
        response.setMonth(payrollMonth.getMonthValue());
        response.setYear(payrollMonth.getYear());

        List<PayrollEmployeeRequestDTO> requestedEmployees = request.getEmployees();
        for (PayrollEmployeeRequestDTO requestedEmployee : requestedEmployees) {
            PayrollEmployeeResponseDTO employeePayroll = processEmployeePayroll(requestedEmployee, payrollMonth);
            response.getEmployees().add(employeePayroll);
        }

        response.setTotalEmployees(response.getEmployees().size());
        response.setTotalGrossSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getMonthlyGrossSalary()))
                .sum()));
        response.setTotalLeaveDeduction(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getLeaveDeduction()))
                .sum()));
        response.setTotalNetSalary(round(response.getEmployees().stream()
                .mapToDouble(employee -> valueOrZero(employee.getNetSalary()))
                .sum()));

        return response;
    }

    private PayrollEmployeeResponseDTO processEmployeePayroll(PayrollEmployeeRequestDTO requestedEmployee,
                                                              YearMonth payrollMonth) {
        PayrollEmployeeResponseDTO response = new PayrollEmployeeResponseDTO();
        if (requestedEmployee == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee payload is required");
            return response;
        }

        response.setEmployeeId(requestedEmployee.getEmployeeId());
        response.setRequestedEmployeeName(requestedEmployee.getEmployeeName());

        if (requestedEmployee.getEmployeeId() == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee id is required");
            return response;
        }

        Employee employee = employeeRepository.findById(requestedEmployee.getEmployeeId()).orElse(null);
        if (employee == null) {
            response.setStatus("FAILED");
            response.setMessage("Employee not found");
            return response;
        }

        response.setEmployeeName(buildEmployeeName(employee));
        SalaryDetails salaryDetails = employee.getSalaryDetails();
        if (salaryDetails == null) {
            response.setStatus("FAILED");
            response.setMessage("Salary details not found");
            return response;
        }

        double basicSalary = parseAmount(salaryDetails.getBasicSalary());
        double bonus = parseAmount(salaryDetails.getBonus());
        double ctc = parseAmount(salaryDetails.getCtc());
        double monthlyGrossSalary = ctc > 0 ? ctc / 12 : basicSalary + bonus;
        int unpaidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, true);
        int paidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, false);
        double dailySalary = monthlyGrossSalary / payrollMonth.lengthOfMonth();
        double leaveDeduction = dailySalary * unpaidLeaveDays;
        double netSalary = monthlyGrossSalary - leaveDeduction;

        response.setStatus("PROCESSED");
        response.setMessage("Payroll processed successfully");
        response.setBasicSalary(round(basicSalary));
        response.setBonus(round(bonus));
        response.setCtc(round(ctc));
        response.setMonthlyGrossSalary(round(monthlyGrossSalary));
        response.setPaidLeaveDays(paidLeaveDays);
        response.setUnpaidLeaveDays(unpaidLeaveDays);
        response.setLeaveDeduction(round(leaveDeduction));
        response.setNetSalary(round(netSalary));

        return response;
    }

    private YearMonth resolvePayrollMonth(PayrollProcessRequestDTO request) {
        LocalDate today = LocalDate.now();
        int month = request.getMonth() == null ? today.getMonthValue() : request.getMonth();
        int year = request.getYear() == null ? today.getYear() : request.getYear();
        return YearMonth.of(year, month);
    }

    private int getApprovedLeaveDays(Long empId, YearMonth payrollMonth, boolean unpaidOnly) {
        return leaveRequestRepository.findByEmployeeEmpIdOrderByCreatedAtDesc(empId).stream()
                .filter(leaveRequest -> "Approved".equalsIgnoreCase(leaveRequest.getStatus()))
                .filter(leaveRequest -> overlapsPayrollMonth(leaveRequest, payrollMonth))
                .filter(leaveRequest -> unpaidOnly == isUnpaidLeave(leaveRequest.getLeaveType()))
                .mapToInt(leaveRequest -> countLeaveDaysInMonth(leaveRequest, payrollMonth))
                .sum();
    }

    private boolean overlapsPayrollMonth(LeaveRequest leaveRequest, YearMonth payrollMonth) {
        LocalDate monthStart = payrollMonth.atDay(1);
        LocalDate monthEnd = payrollMonth.atEndOfMonth();
        return !leaveRequest.getToDate().isBefore(monthStart) && !leaveRequest.getFromDate().isAfter(monthEnd);
    }

    private int countLeaveDaysInMonth(LeaveRequest leaveRequest, YearMonth payrollMonth) {
        LocalDate start = leaveRequest.getFromDate().isBefore(payrollMonth.atDay(1))
                ? payrollMonth.atDay(1)
                : leaveRequest.getFromDate();
        LocalDate end = leaveRequest.getToDate().isAfter(payrollMonth.atEndOfMonth())
                ? payrollMonth.atEndOfMonth()
                : leaveRequest.getToDate();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }

    private boolean isUnpaidLeave(String leaveType) {
        if (leaveType == null) {
            return false;
        }
        String normalizedLeaveType = leaveType.trim().toLowerCase();
        return normalizedLeaveType.equals("unpaid")
                || normalizedLeaveType.equals("lop")
                || normalizedLeaveType.equals("loss of pay");
    }

    private String buildEmployeeName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    private double parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(amount.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double valueOrZero(Double value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
