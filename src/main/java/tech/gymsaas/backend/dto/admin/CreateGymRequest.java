package tech.gymsaas.backend.dto.admin;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGymRequest {

    @NotBlank(message = "Gym name is required")
    @Size(max = 150, message = "Gym name must be at most 150 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 150, message = "Slug must be at most 150 characters")
    private String slug;

    @NotBlank(message = "Owner name is required")
    @Size(max = 120, message = "Owner name must be at most 120 characters")
    private String ownerName;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be valid")
    @Size(max = 150, message = "Owner email must be at most 150 characters")
    private String ownerEmail;

    @NotBlank
    @Size(min = 6)
    private String ownerPassword;

    @NotNull(message = "Access start date is required")
    private LocalDate accessStartDate;

    @NotNull(message = "Access end date is required")
    private LocalDate accessEndDate;
}