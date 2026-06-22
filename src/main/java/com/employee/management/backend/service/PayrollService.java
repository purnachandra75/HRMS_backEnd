package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveRequest;
import com.employee.management.backend.Entity.Payroll;
import com.employee.management.backend.Entity.SalaryDetails;
import com.employee.management.backend.dto.PayrollEmployeeRequestDTO;
import com.employee.management.backend.dto.PayrollEmployeeResponseDTO;
import com.employee.management.backend.dto.PayrollProcessRequestDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveRequestRepository;
import com.employee.management.backend.repository.PayrollRepository;
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
    private final PayrollRepository payrollRepository;

    public PayrollService(EmployeeRepository employeeRepository,
                          LeaveRequestRepository leaveRequestRepository,
                          PayrollRepository payrollRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRepository = payrollRepository;
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public PayrollProcessResponseDTO getProcessedPayrollByMonthAndYear(Integer month, Integer year) {
        PayrollProcessResponseDTO response = new PayrollProcessResponseDTO();
        response.setMonth(month);
        response.setYear(year);

        List<Payroll> payrolls = payrollRepository.findByMonthAndYearOrderByEmployeeIdAsc(month, year);
        for (Payroll payroll : payrolls) {
            response.getEmployees().add(convertPayrollToResponse(payroll));
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

    @Transactional
    public PayrollEmployeeResponseDTO updatePayrollCreditStatus(Long payrollId,
                                                                Long employeeId,
                                                                Integer month,
                                                                Integer year,
                                                                String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new RuntimeException("Status is required");
        }

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseGet(() -> findPayrollByEmployeeMonthAndYear(employeeId, month, year));

        payroll.setCreditStatus(status.trim());
        Payroll updatedPayroll = payrollRepository.save(payroll);
        return convertPayrollToResponse(updatedPayroll);
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
        double netSalary = Math.max(0, monthlyGrossSalary - leaveDeduction);
        Payroll payroll = savePayroll(employee, salaryDetails, payrollMonth, unpaidLeaveDays, round(netSalary));

        response.setStatus("PROCESSED");
        response.setMessage("Payroll processed successfully");
        response.setPayrollId(payroll.getId());
        response.setDateOfJoining(payroll.getDateOfJoining());
        response.setLop(payroll.getLop());
        response.setSalary(payroll.getSalary());
        response.setPanNumber(payroll.getPanNumber());
        response.setAccountNumber(payroll.getAccountNumber());
        response.setIfsc(payroll.getIfsc());
        response.setUan(payroll.getUan());
        response.setPf(payroll.getPf());
        response.setCreditStatus(payroll.getCreditStatus());
        response.setMonth(payroll.getMonth());
        response.setYear(payroll.getYear());
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

    private Payroll savePayroll(Employee employee,
                                SalaryDetails salaryDetails,
                                YearMonth payrollMonth,
                                int lop,
                                double salary) {
        Payroll payroll = payrollRepository
                .findByEmployeeIdAndMonthAndYear(employee.getEmpId(), payrollMonth.getMonthValue(), payrollMonth.getYear())
                .orElseGet(Payroll::new);

        payroll.setEmployeeId(employee.getEmpId());
        payroll.setEmployeeName(buildEmployeeName(employee));
        payroll.setDateOfJoining(employee.getJobDetails() == null ? null : employee.getJobDetails().getDateOfJoining());
        payroll.setLop(lop);
        payroll.setSalary(salary);
        payroll.setPanNumber(salaryDetails.getPanNumber());
        payroll.setAccountNumber(salaryDetails.getAccountNumber());
        payroll.setIfsc(salaryDetails.getIfscCode());
        payroll.setUan(salaryDetails.getUanNumber());
        payroll.setPf(salaryDetails.getPfNumber());
        if (payroll.getCreditStatus() == null || payroll.getCreditStatus().trim().isEmpty()) {
            payroll.setCreditStatus("NON_CREDITED");
        }
        payroll.setMonth(payrollMonth.getMonthValue());
        payroll.setYear(payrollMonth.getYear());

        return payrollRepository.save(payroll);
    }

    private PayrollEmployeeResponseDTO convertPayrollToResponse(Payroll payroll) {
        PayrollEmployeeResponseDTO response = new PayrollEmployeeResponseDTO();
        response.setPayrollId(payroll.getId());
        response.setEmployeeId(payroll.getEmployeeId());
        response.setEmployeeName(payroll.getEmployeeName());
        response.setRequestedEmployeeName(payroll.getEmployeeName());
        response.setStatus("PROCESSED");
        response.setMessage("Payroll data fetched from database");
        response.setDateOfJoining(payroll.getDateOfJoining());
        response.setLop(payroll.getLop());
        response.setSalary(payroll.getSalary());
        response.setNetSalary(payroll.getSalary());
        response.setPanNumber(payroll.getPanNumber());
        response.setAccountNumber(payroll.getAccountNumber());
        response.setIfsc(payroll.getIfsc());
        response.setUan(payroll.getUan());
        response.setPf(payroll.getPf());
        response.setCreditStatus(payroll.getCreditStatus());
        response.setMonth(payroll.getMonth());
        response.setYear(payroll.getYear());

        Employee employee = employeeRepository.findById(payroll.getEmployeeId()).orElse(null);
        if (employee != null && employee.getSalaryDetails() != null) {
            SalaryDetails salaryDetails = employee.getSalaryDetails();
            YearMonth payrollMonth = YearMonth.of(payroll.getYear(), payroll.getMonth());
            double basicSalary = parseAmount(salaryDetails.getBasicSalary());
            double bonus = parseAmount(salaryDetails.getBonus());
            double ctc = parseAmount(salaryDetails.getCtc());
            double monthlyGrossSalary = ctc > 0 ? ctc / 12 : basicSalary + bonus;
            int unpaidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, true);
            int paidLeaveDays = getApprovedLeaveDays(employee.getEmpId(), payrollMonth, false);
            double dailySalary = monthlyGrossSalary / payrollMonth.lengthOfMonth();
            double leaveDeduction = dailySalary * unpaidLeaveDays;

            response.setBasicSalary(round(basicSalary));
            response.setBonus(round(bonus));
            response.setCtc(round(ctc));
            response.setMonthlyGrossSalary(round(monthlyGrossSalary));
            response.setPaidLeaveDays(paidLeaveDays);
            response.setUnpaidLeaveDays(unpaidLeaveDays);
            response.setLeaveDeduction(round(leaveDeduction));
        } else {
            response.setMonthlyGrossSalary(payroll.getSalary());
            response.setLeaveDeduction(0.0);
        }
        return response;
    }

    private Payroll findPayrollByEmployeeMonthAndYear(Long employeeId, Integer month, Integer year) {
        if (employeeId == null || month == null || year == null) {
            throw new RuntimeException("Payroll record not found");
        }
        return payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));
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
