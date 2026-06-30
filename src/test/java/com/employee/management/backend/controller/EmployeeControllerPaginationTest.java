package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void shouldReturnPaginatedEmployees() throws Exception {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setFirstName("Alice");
        employee.setLastName("Smith");
        employee.setEmail("alice@example.com");
        employee.setRole("employee");

        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(1, 5), 6);
        when(employeeService.searchEmployees(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].empId").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.totalElements").value(6));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeService).searchEmployees(any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void shouldSearchEmployeesByKeyword() throws Exception {
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setFirstName("Bob");
        employee.setLastName("Brown");
        employee.setEmail("bob@example.com");
        employee.setRole("employee");

        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 5), 1);
        when(employeeService.searchEmployees(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/employee/getemployee")
                        .param("page", "0")
                        .param("size", "5")
                        .param("search", "b")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].empId").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Bob"));
    }
}
