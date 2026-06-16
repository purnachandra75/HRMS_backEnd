package com.employee.management.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class PayrollProcessResponseDTO {
    private Integer month;
    private Integer year;
    private Integer totalEmployees;
    private Double totalGrossSalary;
    private Double totalLeaveDeduction;
    private Double totalNetSalary;
    private List<PayrollEmployeeResponseDTO> employees = new ArrayList<>();

    public PayrollProcessResponseDTO() {
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

    public Integer getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(Integer totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public Double getTotalGrossSalary() {
        return totalGrossSalary;
    }

    public void setTotalGrossSalary(Double totalGrossSalary) {
        this.totalGrossSalary = totalGrossSalary;
    }

    public Double getTotalLeaveDeduction() {
        return totalLeaveDeduction;
    }

    public void setTotalLeaveDeduction(Double totalLeaveDeduction) {
        this.totalLeaveDeduction = totalLeaveDeduction;
    }

    public Double getTotalNetSalary() {
        return totalNetSalary;
    }

    public void setTotalNetSalary(Double totalNetSalary) {
        this.totalNetSalary = totalNetSalary;
    }

    public List<PayrollEmployeeResponseDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<PayrollEmployeeResponseDTO> employees) {
        this.employees = employees;
    }
}
