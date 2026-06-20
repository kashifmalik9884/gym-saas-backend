package tech.gymsaas.backend.dto.attendance;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Long id;
    private Long gymId;
    private Long memberId;
    private String memberName;
    private LocalDate date;
    private LocalTime punchIn;
    private String status;
    private String source;
    private String deviceId;
    private String note;
    private String editedBy;
}