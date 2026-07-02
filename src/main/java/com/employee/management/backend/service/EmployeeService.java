package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import com.employee.management.backend.dto.DocumentFile;

public interface EmployeeService {
    Page<Employee> findAllEmployees(Pageable pageable);

    Page<Employee> searchEmployees(String search, Pageable pageable);

<<<<<<< HEAD
    Page<Employee> filterEmployees(String department, String status, Pageable pageable);
=======
    Page<Employee> searchEmployees(String search, String department, String status, String employeeType, Pageable pageable);
>>>>>>> 66e0f715eb692c643a365b43c3a0ce851805ea94

    Employee findById(Long empId);

    Employee createEmployee(Employee employee);

    Employee updateEmployee(Long empId, Employee employee);

    void deleteEmployee(Long empId);

    Employee findByEmail(String email);

    void uploadEmployeeDocument(Long empId, String docType, MultipartFile file);

    DocumentFile getEmployeeDocument(Long empId, String docType);
}
