package tech.gymsaas.backend.dto.member;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @NotNull(message = "Monthly fee is required")
    @Positive(message = "Monthly fee must be greater than 0")
    private Double monthlyFee;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @Min(value = 1, message = "Billing day must be between 1 and 31")
    @Max(value = 31, message = "Billing day must be between 1 and 31")
    private Integer billingDay;

    private LocalDate nextDueDate;
    private LocalDate lastPaidDate;

    @Size(max = 20, message = "Payment status must be at most 20 characters")
    private String paymentStatus;

    private Boolean active;

    @Size(max = 500, message = "Photo URL must be at most 500 characters")
    private String photoUrl;

    @Size(max = 100, message = "Photo file ID must be at most 100 characters")
    private String photoFileId;

    @Size(max = 100, message = "Biometric user ID must be at most 100 characters")
    private String biometricUserId;
}