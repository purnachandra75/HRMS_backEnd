package com.employee.management.backend.service;

import java.util.List;

import com.employee.management.backend.Entity.Employee;

public interface EmployeeService {
    List<Employee> findAllEmployees();

    Employee findById(Long empId);

    Employee createEmployee(Employee employee);

    Employee updateEmployee(Long empId, Employee employee);

    void deleteEmployee(Long empId);

    Employee findByEmail(String email);
}
