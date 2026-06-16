package com.employee.management.backend.dto;

public class PayrollEmployeeRequestDTO {
    private Long employeeId;
    private String employeeName;

    public PayrollEmployeeRequestDTO() {
    }

    public PayrollEmployeeRequestDTO(Long employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
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
}
