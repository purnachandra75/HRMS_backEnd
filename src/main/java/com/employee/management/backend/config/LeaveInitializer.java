package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.LeaveBalance;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.LeaveBalanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeaveInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public LeaveInitializer(EmployeeRepository employeeRepository, 
                            LeaveBalanceRepository leaveBalanceRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Get all employees
        List<Employee> employees = employeeRepository.findAll();

        // Default leave types and their annual allocation
        String[] leaveTypes = {"Casual", "Sick", "Paid"};
        int[] leaveAllocations = {12, 8, 20}; // Default days per year

        for (Employee employee : employees) {
            for (int i = 0; i < leaveTypes.length; i++) {
                // Check if leave balance already exists
                var existing = leaveBalanceRepository
                        .findByEmployeeEmpIdAndLeaveType(employee.getEmpId(), leaveTypes[i]);

                if (existing.isEmpty()) {
                    // Create new leave balance
                    LeaveBalance leaveBalance = new LeaveBalance();
                    leaveBalance.setEmployee(employee);
                    leaveBalance.setLeaveType(leaveTypes[i]);
                    leaveBalance.setBalance(leaveAllocations[i]);
                    leaveBalanceRepository.save(leaveBalance);
                }
            }
        }
    }
}
