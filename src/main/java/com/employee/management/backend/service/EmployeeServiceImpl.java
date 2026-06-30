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
import com.employee.management.backend.dto.DocumentFile;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Page<Employee> findAllEmployees(Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        int pageSize = Math.max(pageable.getPageSize(), 1);
        int requestedPage = Math.max(pageable.getPageNumber(), 0);
        long totalElements = employeeRepository.count();

        if (totalElements == 0) {
            return Page.empty(pageable);
        }

        int maxPageIndex = (int) Math.max(0, Math.ceil((double) totalElements / pageSize) - 1);
        int safePageNumber = Math.min(requestedPage, maxPageIndex);
        Pageable safePageable = PageRequest.of(safePageNumber, pageSize, pageable.getSort());
        return employeeRepository.findAll(safePageable);
    }

    @Override
    public Page<Employee> searchEmployees(String search, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.isEmpty()) {
            return findAllEmployees(PageRequest.of(Math.max(pageable.getPageNumber(), 0), Math.max(pageable.getPageSize(), 1), pageable.getSort()));
        }

        if (isNumeric(normalizedSearch)) {
            Long employeeId = Long.parseLong(normalizedSearch);
            final int safePageNumber = Math.max(pageable.getPageNumber(), 0);
            final int safePageSize = Math.max(pageable.getPageSize(), 1);
            final var sort = pageable.getSort();
            final PageRequest pageRequest = PageRequest.of(safePageNumber, safePageSize, sort);
            final PageRequest singleResultRequest = PageRequest.of(0, 1, sort);
            return employeeRepository.findById(employeeId)
                    .map(employee -> new PageImpl<Employee>(List.of(employee), singleResultRequest, 1))
                    .orElseGet(() -> new PageImpl<Employee>(List.of(), pageRequest, 0));
        }

        String searchPattern = "%" + normalizedSearch.toLowerCase() + "%";
        return employeeRepository.searchEmployees(searchPattern, PageRequest.of(Math.max(pageable.getPageNumber(), 0), Math.max(pageable.getPageSize(), 1), pageable.getSort()));
    }

    private boolean isNumeric(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
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

    @Override
    public void uploadEmployeeDocument(Long empId, String docType, MultipartFile file) {
        Employee employee = findById(empId);
        DocumentDetails details = employee.getDocumentDetails();
        if (details == null) {
            details = new DocumentDetails();
            details.setEmployee(employee);
            employee.setDocumentDetails(details);
        }

        try {
            byte[] data = file.getBytes();
            switch (docType.toLowerCase()) {
                case "resume":
                    details.setResumeUploadData(data);
                    details.setResumeUploadName(file.getOriginalFilename());
                    details.setResumeUploadContentType(file.getContentType());
                    break;
                case "idproof":
                    details.setIdProofUploadData(data);
                    details.setIdProofUploadName(file.getOriginalFilename());
                    details.setIdProofUploadContentType(file.getContentType());
                    break;
                case "addressproof":
                    details.setAddressProofUploadData(data);
                    details.setAddressProofUploadName(file.getOriginalFilename());
                    details.setAddressProofUploadContentType(file.getContentType());
                    break;
                case "education":
                    details.setEducationalCertificatesData(data);
                    details.setEducationalCertificatesName(file.getOriginalFilename());
                    details.setEducationalCertificatesContentType(file.getContentType());
                    break;
                case "experience":
                    details.setExperienceCertificatesData(data);
                    details.setExperienceCertificatesName(file.getOriginalFilename());
                    details.setExperienceCertificatesContentType(file.getContentType());
                    break;
                case "passport":
                    details.setPassportPhotoData(data);
                    details.setPassportPhotoName(file.getOriginalFilename());
                    details.setPassportPhotoContentType(file.getContentType());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported document type: " + docType);
            }
            employeeRepository.save(employee);
        } catch (Exception ex) {
            throw new RuntimeException("Could not store document file", ex);
        }
    }

    @Override
    public DocumentFile getEmployeeDocument(Long empId, String docType) {
        Employee employee = findById(empId);
        DocumentDetails details = employee.getDocumentDetails();
        if (details == null) {
            throw new ResourceNotFoundException("DocumentDetails", "empId", empId);
        }

        switch (docType.toLowerCase()) {
            case "resume":
                return toDocumentFile(details.getResumeUploadData(), details.getResumeUploadName(), details.getResumeUploadContentType());
            case "idproof":
                return toDocumentFile(details.getIdProofUploadData(), details.getIdProofUploadName(), details.getIdProofUploadContentType());
            case "addressproof":
                return toDocumentFile(details.getAddressProofUploadData(), details.getAddressProofUploadName(), details.getAddressProofUploadContentType());
            case "education":
                return toDocumentFile(details.getEducationalCertificatesData(), details.getEducationalCertificatesName(), details.getEducationalCertificatesContentType());
            case "experience":
                return toDocumentFile(details.getExperienceCertificatesData(), details.getExperienceCertificatesName(), details.getExperienceCertificatesContentType());
            case "passport":
                return toDocumentFile(details.getPassportPhotoData(), details.getPassportPhotoName(), details.getPassportPhotoContentType());
            default:
                throw new IllegalArgumentException("Unsupported document type: " + docType);
        }
    }

    private DocumentFile toDocumentFile(byte[] data, String fileName, String contentType) {
        if (data == null || fileName == null) {
            throw new ResourceNotFoundException("Document file", "name", fileName);
        }
        return new DocumentFile(fileName, contentType == null ? "application/octet-stream" : contentType, data.length, data);
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
