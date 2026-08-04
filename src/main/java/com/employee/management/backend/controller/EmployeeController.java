package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.dto.DocumentFile;
import com.employee.management.backend.security.JwtUtil;
import com.employee.management.backend.service.EmployeeService;
import com.employee.management.backend.service.PasswordResetService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/employees", "/api/employee"})
public class EmployeeController {
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public EmployeeController(EmployeeService employeeService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                               PasswordResetService passwordResetService) {
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping({"", "/getemployee"})
    public Page<Employee> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeType) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(size, 1);
        String normalizedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String normalizedDepartment = normalizeFilterValue(department);
        String normalizedStatus = normalizeFilterValue(status);
        String normalizedEmployeeType = normalizeFilterValue(employeeType);
        if (normalizedSearch != null) {
            return employeeService.searchEmployees(
                    normalizedSearch,
                    normalizedDepartment,
                    normalizedStatus,
                    normalizedEmployeeType,
                    PageRequest.of(normalizedPage, normalizedSize)
            );
        }
        return employeeService.filterEmployees(normalizedDepartment, normalizedStatus, PageRequest.of(normalizedPage, normalizedSize));
    }

    @GetMapping({"/report", "/reports"})
    public Page<Employee> getEmployeeReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        String normalizedDepartment = normalizeFilterValue(department);
        String normalizedStatus = normalizeFilterValue(status);
        return employeeService.filterEmployees(normalizedDepartment, normalizedStatus, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    }

    @GetMapping("/{empId}")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long empId) {
        return ResponseEntity.ok(employeeService.findById(empId));
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        // Only an already-authenticated admin may set an arbitrary role (e.g. creating another
        // admin). Anonymous/self-registration requests always become a plain employee, regardless
        // of what role value the request body claims.
        if (!isCurrentUserAdmin() || employee.getRole() == null || employee.getRole().trim().isEmpty()) {
            employee.setRole("employee");
        }
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }
        Employee saved = employeeService.createEmployee(employee);
        return ResponseEntity.ok(saved);
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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

            if (!isPasswordValid(storedPassword, providedPassword, employee.getEmpId())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid email or password"));
            }
            String name = String.format("%s %s", employee.getFirstName(), employee.getLastName()).trim();
            String token = jwtUtil.generateToken(employee.getEmpId(), employee.getEmail(), employee.getRole());
            return ResponseEntity.ok(new LoginResponse(employee.getEmpId(), name, employee.getEmail(), employee.getRole(), token));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid email or password"));
        }
    }

    // Supports accounts created before password hashing was introduced: if the stored value
    // isn't a bcrypt hash, fall back to a plain comparison and transparently upgrade it to a
    // hash on successful login so every account converges to hashed storage over time.
    private boolean isPasswordValid(String storedPassword, String providedPassword, Long empId) {
        if (storedPassword.isEmpty()) {
            return false;
        }
        boolean looksHashed = storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$");
        if (looksHashed) {
            return passwordEncoder.matches(providedPassword, storedPassword);
        }
        if (storedPassword.equals(providedPassword)) {
            employeeService.updatePasswordHash(empId, passwordEncoder.encode(providedPassword));
            return true;
        }
        return false;
    }

    @PutMapping("/{empId}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long empId, @RequestBody Employee employee) {
        Employee updated = employeeService.updateEmployee(empId, employee);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.requestReset(request.getEmail());
        } catch (Exception ex) {
            // Log server-side for diagnosing SMTP/config issues, but never let the caller
            // distinguish "email doesn't exist" from "email send failed".
            System.out.println("Password reset request failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        return ResponseEntity.ok(new ApiResponse(true, "If that email is registered, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new ApiResponse(true, "Password has been reset successfully. You can now log in."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to reset password"));
        }
    }

    public static class ForgotPasswordRequest {
        @JsonProperty("email")
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ResetPasswordRequest {
        @JsonProperty("token")
        private String token;
        @JsonProperty("newPassword")
        private String newPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
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
        private String token;

        public LoginResponse() {
        }

        public LoginResponse(Long userId, String name, String email, String role, String token) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.token = token;
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

        public String getToken() {
            return token;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
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
