package tech.gymsaas.backend.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.gymsaas.backend.dto.attendance.AttendanceRequest;
import tech.gymsaas.backend.dto.attendance.AttendanceResponse;
import tech.gymsaas.backend.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(attendanceService.getAttendance(date, status, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> getAttendanceById(@PathVariable Long id,
                                                                Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id, authentication));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByMemberId(@PathVariable Long memberId,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getAttendanceByMemberId(memberId, authentication));
    }

    @PostMapping
    public ResponseEntity<AttendanceResponse> createAttendance(@Valid @RequestBody AttendanceRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createAttendance(request, authentication));
    }

    @PostMapping("/demo-scan")
    public ResponseEntity<AttendanceResponse> createDemoScan(@RequestBody Map<String, Long> payload,
                                                             Authentication authentication) {
        Long memberId = payload.get("memberId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createDemoScan(memberId, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendance(@PathVariable Long id,
                                                               @Valid @RequestBody AttendanceRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAttendance(@PathVariable Long id,
                                                                Authentication authentication) {
        attendanceService.deleteAttendance(id, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Attendance deleted successfully",
                "id", id
        ));
    }
}