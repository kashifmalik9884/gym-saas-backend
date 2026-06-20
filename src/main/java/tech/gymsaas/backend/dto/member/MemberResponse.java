package tech.gymsaas.backend.dto.member;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private Long id;
    private Long gymId;
    private String name;
    private String phone;
    private Double monthlyFee;
    private LocalDate joiningDate;
    private Integer billingDay;
    private LocalDate nextDueDate;
    private LocalDate lastPaidDate;
    private String paymentStatus;
    private Boolean active;
    private String photoUrl;
    private String photoFileId;
    private String biometricUserId;
}