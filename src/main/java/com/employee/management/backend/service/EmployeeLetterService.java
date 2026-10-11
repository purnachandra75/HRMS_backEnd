package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.dto.EmployeeLetterResponseDTO;
import com.employee.management.backend.exception.ResourceNotFoundException;
import com.employee.management.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EmployeeLetterService {
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    private final EmployeeRepository employeeRepository;

    public EmployeeLetterService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // Looks the employee up by the client-assigned Employee ID (e.g. "EMP001") inside the
    // caller's own client, so an admin can only generate letters for their own company.
    public EmployeeLetterResponseDTO getLetterDetails(Long clientId, String employeeCode, LocalDate relievingDate) {
        String code = employeeCode == null ? "" : employeeCode.trim();
        Employee employee = employeeRepository.findByClientIdAndEmployeeId(clientId, code)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", code));
        JobDetails jobDetails = employee.getJobDetails();

        String employeeFullName = buildFullName(employee);
        String designation = jobDetails != null ? jobDetails.getDesignation() : null;
        LocalDate joiningDate = jobDetails != null ? parseDate(jobDetails.getDateOfJoining()) : null;
        return new EmployeeLetterResponseDTO(
                employee.getEmployeeId(),
                employeeFullName,
                designation,
                joiningDate,
                relievingDate);
    }

    private String buildFullName(Employee employee) {
        String firstName = employee.getFirstName() != null ? employee.getFirstName().trim() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName().trim() : "";
        return (firstName + " " + lastName).trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmedValue = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmedValue, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Invalid joining date format for employee job details");
    }
}
