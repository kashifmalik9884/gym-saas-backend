package tech.gymsaas.backend.dto.attendance;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    private LocalDate date;
    private LocalTime punchIn;

    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    @Size(max = 20, message = "Source must be at most 20 characters")
    private String source;

    @Size(max = 50, message = "Device ID must be at most 50 characters")
    private String deviceId;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}