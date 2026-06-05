package com.employee.management.backend.service;

import com.employee.management.backend.model.Employee;

import java.util.List;

public interface EmployeeService {
    List<Employee> findAllEmployees();

    Employee findById(Long empId);

    Employee createEmployee(Employee employee);

    Employee updateEmployee(Long empId, Employee employee);

    void deleteEmployee(Long empId);

    Employee findByEmail(String email);
}
