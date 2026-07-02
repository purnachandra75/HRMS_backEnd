package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.service.EmployeeService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/employees", "/api/employee"})
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping({"", "/getemployee"})
    public Page<Employee> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(size, 1);
        String normalizedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        if (normalizedSearch != null) {
            return employeeService.searchEmployees(normalizedSearch, PageRequest.of(normalizedPage, normalizedSize));
        }
        return employeeService.filterEmployees(department, status, PageRequest.of(normalizedPage, normalizedSize));
    }

    @GetMapping({"/report", "/reports"})
    public Page<Employee> getEmployeeReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        return employeeService.filterEmployees(department, status, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    }

    @GetMapping("/{empId}")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long empId) {
        return ResponseEntity.ok(employeeService.findById(empId));
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        if (employee.getRole() == null || employee.getRole().trim().isEmpty()) {
            employee.setRole("employee");
        }
        Employee saved = employeeService.createEmployee(employee);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Email is required"));
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Password is required"));
        }

        try {
            Employee employee = employeeService.findByEmail(request.getEmail().trim());
            
            String storedPassword = employee.getPassword() != null ? employee.getPassword().trim() : "";
            String providedPassword = request.getPassword().trim();
            
            System.out.println("Login attempt - Email: " + request.getEmail() + ", Stored Password: " + storedPassword + ", Provided Password: " + providedPassword);
            
            if (storedPassword.isEmpty() || !storedPassword.equals(providedPassword)) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid email or password"));
            }
            String name = String.format("%s %s", employee.getFirstName(), employee.getLastName()).trim();
            return ResponseEntity.ok(new LoginResponse(employee.getEmpId(), name, employee.getEmail(), employee.getRole()));
        } catch (Exception ex) {
            System.out.println("Login exception: " + ex.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid email or password"));
        }
    }

    @PutMapping("/{empId}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long empId, @RequestBody Employee employee) {
        Employee updated = employeeService.updateEmployee(empId, employee);
        return ResponseEntity.ok(updated);
    }

    public static class LoginRequest {
        @JsonProperty("email")
        private String email;
        @JsonProperty("password")
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse() {
        }

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LoginResponse {
        private Long userId;
        private String name;
        private String email;
        private String role;

        public LoginResponse() {
        }

        public LoginResponse(Long userId, String name, String email, String role) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }
    }

    @DeleteMapping("/{empId}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long empId) {
        employeeService.deleteEmployee(empId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = {"/{empId}/documents/{docType}", "/{empId}/document/{docType}"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadEmployeeDocument(
            @PathVariable Long empId,
            @PathVariable String docType,
            @RequestParam("file") MultipartFile file) {
        employeeService.uploadEmployeeDocument(empId, docType, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = {"/{empId}/documents/{docType}", "/{empId}/document/{docType}"})
    public ResponseEntity<byte[]> downloadEmployeeDocument(
            @PathVariable Long empId,
            @PathVariable String docType) {
        DocumentFile documentFile = employeeService.getEmployeeDocument(empId, docType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentFile.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(documentFile.getContentType()))
                .contentLength(documentFile.getSize())
                .body(documentFile.getData());
    }
}
