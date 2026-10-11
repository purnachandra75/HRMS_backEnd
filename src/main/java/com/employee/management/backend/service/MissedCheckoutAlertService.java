package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Attendance;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Warns when someone checked in but hasn't checked out within app.attendance.missed-checkout-hours
 * (14 by default) of their check-in: one email to the employee and one to each admin (HR) of
 * their company. Runs every app.attendance.missed-checkout-check-ms (15 minutes by default) and
 * marks the attendance row so each missed check-out is reported only once.
 */
@Service
public class MissedCheckoutAlertService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final String mailFrom;
    private final long missedCheckoutHours;

    public MissedCheckoutAlertService(AttendanceRepository attendanceRepository,
                                      EmployeeRepository employeeRepository,
                                      EmailService emailService,
                                      @Value("${app.mail.from}") String mailFrom,
                                      @Value("${app.attendance.missed-checkout-hours:14}") long missedCheckoutHours) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.mailFrom = mailFrom;
        this.missedCheckoutHours = missedCheckoutHours;
    }

    public long getMissedCheckoutHours() {
        return missedCheckoutHours;
    }

    @Scheduled(fixedDelayString = "${app.attendance.missed-checkout-check-ms:900000}", initialDelay = 60000)
    @Transactional
    public void sendMissedCheckoutAlerts() {
        LocalDateTime now = LocalDateTime.now();
        // Only recent rows: a shift can't have started more than a couple of days before its
        // 14-hour deadline, and this keeps old history from being emailed about all at once.
        String fromDate = LocalDate.now().minusDays(2).toString();

        int sent = 0;
        for (Attendance attendance : attendanceRepository.findOpenWithoutMissedCheckoutAlert(fromDate)) {
            LocalDateTime checkIn = AttendanceTimeUtils.checkInDateTime(attendance.getDate(), attendance.getCheckInTime());
            if (checkIn == null || now.isBefore(checkIn.plusHours(missedCheckoutHours))) {
                continue;
            }
            notify(attendance, checkIn);
            attendance.setMissedCheckoutAlertSent(true);
            if (attendance.getRemarks() == null || attendance.getRemarks().isBlank()) {
                attendance.setRemarks("Missed check-out");
            }
            attendanceRepository.save(attendance);
            sent++;
        }
        if (sent > 0) {
            System.out.println("Missed check-out alerts sent for " + sent + " attendance record(s)");
        }
    }

    private void notify(Attendance attendance, LocalDateTime checkIn) {
        Employee employee = attendance.getEmployee();
        String name = HtmlUtils.htmlEscape(fullName(employee));
        String code = employee.getEmployeeId() == null ? "" : HtmlUtils.htmlEscape(employee.getEmployeeId());
        String checkInText = checkIn.format(DISPLAY);
        String subject = "Missed check-out warning - " + fullName(employee) + " (" + attendance.getDate() + ")";

        String details = "<ul>"
                + "<li><strong>Employee Name:</strong> " + name + "</li>"
                + "<li><strong>Employee ID:</strong> " + code + "</li>"
                + "<li><strong>Checked in at:</strong> " + checkInText + "</li>"
                + "<li><strong>Checked out:</strong> Not yet</li>"
                + "</ul>";

        if (hasEmail(employee)) {
            String body = "<p>Hello " + name + ",</p>"
                    + "<p>You checked in but have not checked out, and it has been more than "
                    + missedCheckoutHours + " hours since your check-in.</p>"
                    + details
                    + "<p>Please remember to check out at the end of every working day. "
                    + "If you have already left, contact HR to get your attendance corrected.</p>";
            emailService.sendHtmlEmail(employee.getEmail().trim(), mailFrom, null, subject, body);
        }

        String hrBody = "<p>Hello,</p>"
                + "<p>This employee checked in but has not checked out more than "
                + missedCheckoutHours + " hours after check-in:</p>"
                + details
                + "<p>A warning email has also been sent to the employee.</p>";
        String replyTo = hasEmail(employee) ? employee.getEmail().trim() : null;
        for (String hrEmail : hrEmails(employee)) {
            emailService.sendHtmlEmail(hrEmail, mailFrom, replyTo, subject, hrBody);
        }
    }

    // Every admin of the employee's own company, never anyone else's - the same people who get
    // leave and ticket notifications. The employee is skipped if they're an admin themselves.
    private Set<String> hrEmails(Employee employee) {
        Set<String> emails = new LinkedHashSet<>();
        if (employee.getClient() == null) return emails;
        for (Employee admin : employeeRepository.findByClientIdAndRoleIgnoreCase(employee.getClient().getId(), "admin")) {
            if (hasEmail(admin) && !admin.getEmpId().equals(employee.getEmpId())) {
                emails.add(admin.getEmail().trim());
            }
        }
        return emails;
    }

    private static boolean hasEmail(Employee employee) {
        return employee.getEmail() != null && !employee.getEmail().isBlank();
    }

    private static String fullName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }
}
