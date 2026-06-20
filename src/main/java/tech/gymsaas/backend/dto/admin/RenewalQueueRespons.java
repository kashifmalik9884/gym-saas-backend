package tech.gymsaas.backend.dto.admin;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalQueueRespons {
    private Long gymId;
    private String gymName;
    private String ownerName;
    private LocalDate accessEndDate;
    private String status; // "due-soon", "expired", "renewed"
}