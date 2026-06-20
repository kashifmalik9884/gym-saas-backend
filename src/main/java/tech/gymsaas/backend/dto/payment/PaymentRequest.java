package tech.gymsaas.backend.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PaymentRequest {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Amount is required")
    private Double amount;

    private LocalDate paymentDate;
    private LocalDate dueDate;
    private String paymentStatus; // Default to "paid" or "pending"
    private String paymentMethod;
    private String notes;
}