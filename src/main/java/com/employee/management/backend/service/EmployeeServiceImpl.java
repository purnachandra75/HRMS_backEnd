package com.employee.management.backend.service;

import com.employee.management.backend.Entity.AddressDetails;
import com.employee.management.backend.Entity.DocumentDetails;
import com.employee.management.backend.Entity.EducationDetails;
import com.employee.management.backend.Entity.EmergencyContact;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.Entity.SalaryDetails;
import com.employee.management.backend.exception.ResourceNotFoundException;
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
       // existing.setPassword(incoming.getPassword());
        existing.setRole(incoming.getRole());
        existing.setProfilePhoto(incoming.getProfilePhoto());

        // Update child entities instead of replacing them to avoid Hibernate session conflicts
        updateAddressDetails(existing, incoming.getAddressDetails());
        updateJobDetails(existing, incoming.getJobDetails());
        updateSalaryDetails(existing, incoming.getSalaryDetails());
        updateEducationDetails(existing, incoming.getEducationDetails());
        updateEmergencyContact(existing, incoming.getEmergencyContact());
        updateDocumentDetails(existing, incoming.getDocumentDetails());
    }

    private void updateAddressDetails(Employee employee, AddressDetails incoming) {
        if (incoming == null) {
            employee.setAddressDetails(null);
            return;
        }
        
        AddressDetails existing = employee.getAddressDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setAddressDetails(incoming);
        } else {
            existing.setPermanentAddress(incoming.getPermanentAddress());
            existing.setCurrentAddress(incoming.getCurrentAddress());
            existing.setCountry(incoming.getCountry());
            existing.setState(incoming.getState());
            existing.setCity(incoming.getCity());
            existing.setPincode(incoming.getPincode());
        }
    }

    private void updateJobDetails(Employee employee, JobDetails incoming) {
        if (incoming == null) {
            employee.setJobDetails(null);
            return;
        }
        
        JobDetails existing = employee.getJobDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setJobDetails(incoming);
        } else {
            existing.setDesignation(incoming.getDesignation());
            existing.setDepartment(incoming.getDepartment());
            existing.setEmployeeType(incoming.getEmployeeType());
            existing.setReportingManager(incoming.getReportingManager());
            existing.setDateOfJoining(incoming.getDateOfJoining());
            existing.setWorkLocation(incoming.getWorkLocation());
            existing.setEmployeeStatus(incoming.getEmployeeStatus());
            existing.setShiftTiming(incoming.getShiftTiming());
            existing.setExperience(incoming.getExperience());
            existing.setEmployeeCategory(incoming.getEmployeeCategory());
        }
    }

    private void updateSalaryDetails(Employee employee, SalaryDetails incoming) {
        if (incoming == null) {
            employee.setSalaryDetails(null);
            return;
        }
        
        SalaryDetails existing = employee.getSalaryDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setSalaryDetails(incoming);
        } else {
            existing.setBasicSalary(incoming.getBasicSalary());
            existing.setBonus(incoming.getBonus());
            existing.setCtc(incoming.getCtc());
            existing.setPfApplicable(incoming.getPfApplicable());
            existing.setPfNumber(incoming.getPfNumber());
            existing.setEsiApplicable(incoming.getEsiApplicable());
            existing.setBankName(incoming.getBankName());
            existing.setAccountNumber(incoming.getAccountNumber());
            existing.setIfscCode(incoming.getIfscCode());
            existing.setBranchName(incoming.getBranchName());
            existing.setPanNumber(incoming.getPanNumber());
            existing.setAadhaarNumber(incoming.getAadhaarNumber());
            existing.setUanNumber(incoming.getUanNumber());
        }
    }

    private void updateEducationDetails(Employee employee, EducationDetails incoming) {
        if (incoming == null) {
            employee.setEducationDetails(null);
            return;
        }
        
        EducationDetails existing = employee.getEducationDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setEducationDetails(incoming);
        } else {
            existing.setHighestQualification(incoming.getHighestQualification());
            existing.setUniversityCollege(incoming.getUniversityCollege());
            existing.setYearOfPassing(incoming.getYearOfPassing());
            existing.setPercentageCGPA(incoming.getPercentageCGPA());
        }
    }

    private void updateEmergencyContact(Employee employee, EmergencyContact incoming) {
        if (incoming == null) {
            employee.setEmergencyContact(null);
            return;
        }
        
        EmergencyContact existing = employee.getEmergencyContact();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setEmergencyContact(incoming);
        } else {
            existing.setEmergencyContactName(incoming.getEmergencyContactName());
            existing.setRelationship(incoming.getRelationship());
            existing.setEmergencyContactNumber(incoming.getEmergencyContactNumber());
            existing.setEmergencyAlternateNumber(incoming.getEmergencyAlternateNumber());
            existing.setEmergencyAddress(incoming.getEmergencyAddress());
        }
    }

    private void updateDocumentDetails(Employee employee, DocumentDetails incoming) {
        if (incoming == null) {
            employee.setDocumentDetails(null);
            return;
        }
        
        DocumentDetails existing = employee.getDocumentDetails();
        if (existing == null) {
            incoming.setEmployee(employee);
            employee.setDocumentDetails(incoming);
        } else {
            existing.setResumeUpload(incoming.getResumeUpload());
            existing.setIdProofUpload(incoming.getIdProofUpload());
            existing.setAddressProofUpload(incoming.getAddressProofUpload());
            existing.setEducationalCertificates(incoming.getEducationalCertificates());
            existing.setExperienceCertificates(incoming.getExperienceCertificates());
            existing.setPassportPhoto(incoming.getPassportPhoto());
        }
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
