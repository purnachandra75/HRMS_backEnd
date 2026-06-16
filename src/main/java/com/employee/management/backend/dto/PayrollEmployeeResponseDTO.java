package com.employee.management.backend.dto;

public class PayrollEmployeeResponseDTO {
    private Long employeeId;
    private String employeeName;
    private String requestedEmployeeName;
    private String status;
    private String message;
    private Double basicSalary;
    private Double bonus;
    private Double ctc;
    private Double monthlyGrossSalary;
    private Integer paidLeaveDays;
    private Integer unpaidLeaveDays;
    private Double leaveDeduction;
    private Double netSalary;

    public PayrollEmployeeResponseDTO() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getRequestedEmployeeName() {
        return requestedEmployeeName;
    }

    public void setRequestedEmployeeName(String requestedEmployeeName) {
        this.requestedEmployeeName = requestedEmployeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getCtc() {
        return ctc;
    }

    public void setCtc(Double ctc) {
        this.ctc = ctc;
    }

    public Double getMonthlyGrossSalary() {
        return monthlyGrossSalary;
    }

    public void setMonthlyGrossSalary(Double monthlyGrossSalary) {
        this.monthlyGrossSalary = monthlyGrossSalary;
    }

    public Integer getPaidLeaveDays() {
        return paidLeaveDays;
    }

    public void setPaidLeaveDays(Integer paidLeaveDays) {
        this.paidLeaveDays = paidLeaveDays;
    }

    public Integer getUnpaidLeaveDays() {
        return unpaidLeaveDays;
    }

    public void setUnpaidLeaveDays(Integer unpaidLeaveDays) {
        this.unpaidLeaveDays = unpaidLeaveDays;
    }

    public Double getLeaveDeduction() {
        return leaveDeduction;
    }

    public void setLeaveDeduction(Double leaveDeduction) {
        this.leaveDeduction = leaveDeduction;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }
}
