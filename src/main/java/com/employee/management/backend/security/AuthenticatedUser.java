package com.employee.management.backend.security;

public record AuthenticatedUser(Long empId, String email, String role) {
}
