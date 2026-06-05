package com.employee.management.backend.service;

import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.model.Employee;
import com.employee.management.backend.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public Employee findById(Long empId) {
        return employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "empId", empId));
    }

    @Override
    public Employee createEmployee(Employee employee) {
        linkChildEntities(employee);
        return employeeRepository.save(employee);
    }

    @Override
    public Employee updateEmployee(Long empId, Employee employee) {
        Employee existing = findById(empId);
        applyUpdates(existing, employee);
        linkChildEntities(existing);
        return employeeRepository.save(existing);
    }

    @Override
    public void deleteEmployee(Long empId) {
        Employee existing = findById(empId);
        employeeRepository.delete(existing);
    }

    @Override
    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "email", email));
    }

    private void applyUpdates(Employee existing, Employee incoming) {
        existing.setFirstName(incoming.getFirstName());
        existing.setLastName(incoming.getLastName());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setAlternatePhone(incoming.getAlternatePhone());
        existing.setGender(incoming.getGender());
        existing.setDateOfBirth(incoming.getDateOfBirth());
        existing.setBloodGroup(incoming.getBloodGroup());
        existing.setMaritalStatus(incoming.getMaritalStatus());
        existing.setNationality(incoming.getNationality());
        existing.setPassword(incoming.getPassword());
        existing.setRole(incoming.getRole());
        existing.setProfilePhoto(incoming.getProfilePhoto());

        existing.setAddressDetails(incoming.getAddressDetails());
        existing.setJobDetails(incoming.getJobDetails());
        existing.setSalaryDetails(incoming.getSalaryDetails());
        existing.setEducationDetails(incoming.getEducationDetails());
        existing.setEmergencyContact(incoming.getEmergencyContact());
        existing.setDocumentDetails(incoming.getDocumentDetails());
    }

    private void linkChildEntities(Employee employee) {
        if (employee == null) {
            return;
        }

        if (employee.getAddressDetails() != null) {
            employee.getAddressDetails().setEmployee(employee);
        }
        if (employee.getJobDetails() != null) {
            employee.getJobDetails().setEmployee(employee);
        }
        if (employee.getSalaryDetails() != null) {
            employee.getSalaryDetails().setEmployee(employee);
        }
        if (employee.getEducationDetails() != null) {
            employee.getEducationDetails().setEmployee(employee);
        }
        if (employee.getEmergencyContact() != null) {
            employee.getEmergencyContact().setEmployee(employee);
        }
        if (employee.getDocumentDetails() != null) {
            employee.getDocumentDetails().setEmployee(employee);
        }
    }
}
