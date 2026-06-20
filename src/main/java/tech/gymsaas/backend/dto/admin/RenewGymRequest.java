package tech.gymsaas.backend.dto.admin;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewGymRequest {

    @NotNull(message = "New access end date is required")
    private LocalDate newAccessEndDate;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}