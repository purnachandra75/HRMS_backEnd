package com.employee.management.backend.controller;

import com.employee.management.backend.dto.CreateHolidayDTO;
import com.employee.management.backend.dto.HolidayDTO;
import com.employee.management.backend.dto.UpdateHolidayDTO;
import com.employee.management.backend.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public ResponseEntity<List<HolidayDTO>> listHolidays(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Integer upcomingDays) {
        return ResponseEntity.ok(holidayService.getHolidays(year, start, end, upcomingDays));
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getYears() {
        return ResponseEntity.ok(holidayService.getHolidayYears());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayDTO> getHoliday(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getHoliday(id));
    }

    @PostMapping
    public ResponseEntity<HolidayDTO> createHoliday(
            @RequestHeader(value = "X-User-Role", defaultValue = "EMPLOYEE") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "system") String userName,
            @Valid @RequestBody CreateHolidayDTO request) {
        requireAdmin(userRole);
        HolidayDTO holidayDTO = holidayService.createHoliday(request, userName);
        return ResponseEntity.created(URI.create("/api/holidays/" + holidayDTO.getId()))
                .body(holidayDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolidayDTO> updateHoliday(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "EMPLOYEE") String userRole,
            @RequestHeader(value = "X-User-Name", defaultValue = "system") String userName,
            @Valid @RequestBody UpdateHolidayDTO request) {
        requireAdmin(userRole);
        return ResponseEntity.ok(holidayService.updateHoliday(id, request, userName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "EMPLOYEE") String userRole) {
        requireAdmin(userRole);
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String userRole) {
        if (!"ADMIN".equalsIgnoreCase(userRole.trim())) {
            throw new IllegalArgumentException("Admin role required");
        }
    }
}
