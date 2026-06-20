package tech.gymsaas.backend.dto.admin;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymResponse {
    private Long id;
    private String name;
    private String slug;
    private String ownerName;
    private String ownerEmail;
    private String status;
    private LocalDate accessStartDate;
    private LocalDate accessEndDate;
    private LocalDateTime lastRenewedAt;
}