package tech.gymsaas.backend.dto.payment;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private Double amount;
    private LocalDate paymentDate;
    private LocalDate dueDate;
    private String paymentStatus;
    private String paymentMethod;
    private String notes;
}