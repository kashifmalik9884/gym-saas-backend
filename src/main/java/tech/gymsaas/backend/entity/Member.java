package tech.gymsaas.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private Double monthlyFee;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private Integer billingDay;

    @Column(nullable = false)
    private LocalDate nextDueDate;

    private LocalDate lastPaidDate;

    @Column(nullable = false, length = 20)
    private String paymentStatus;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 500)
    private String photoUrl;

    @Column(length = 150)
    private String photoFileId;

    @Column(length = 150)
    private String biometricUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.active == null) {
            this.active = true;
        }

        if (this.paymentStatus == null || this.paymentStatus.isBlank()) {
            this.paymentStatus = "pending";
        }

        if (this.joiningDate != null && (this.billingDay == null || this.billingDay < 1 || this.billingDay > 31)) {
            this.billingDay = this.joiningDate.getDayOfMonth();
        }

        // 🌟 Safe Updates: Only update if the string properties actually alter
        updateIfChanged("name", trimToNull(this.name));
        updateIfChanged("phone", trimToNull(this.phone));
        updateIfChanged("paymentStatus", trimToNull(this.paymentStatus));
        updateIfChanged("photoUrl", trimToNull(this.photoUrl));
        updateIfChanged("photoFileId", trimToNull(this.photoFileId));
        updateIfChanged("biometricUserId", trimToNull(this.biometricUserId));
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        // 🌟 Safe Updates: Avoid setting identical string hashes to drop dirty flags
        updateIfChanged("name", trimToNull(this.name));
        updateIfChanged("phone", trimToNull(this.phone));
        updateIfChanged("paymentStatus", trimToNull(this.paymentStatus));
        updateIfChanged("photoUrl", trimToNull(this.photoUrl));
        updateIfChanged("photoFileId", trimToNull(this.photoFileId));
        updateIfChanged("biometricUserId", trimToNull(this.biometricUserId));
    }

    private void updateIfChanged(String field, String newValue) {
        switch (field) {
            case "name" -> { if ((newValue == null && this.name != null) || (newValue != null && !newValue.equals(this.name))) this.name = newValue; }
            case "phone" -> { if ((newValue == null && this.phone != null) || (newValue != null && !newValue.equals(this.phone))) this.phone = newValue; }
            case "paymentStatus" -> { if ((newValue == null && this.paymentStatus != null) || (newValue != null && !newValue.equals(this.paymentStatus))) this.paymentStatus = newValue; }
            case "photoUrl" -> { if ((newValue == null && this.photoUrl != null) || (newValue != null && !newValue.equals(this.photoUrl))) this.photoUrl = newValue; }
            case "photoFileId" -> { if ((newValue == null && this.photoFileId != null) || (newValue != null && !newValue.equals(this.photoFileId))) this.photoFileId = newValue; }
            case "biometricUserId" -> { if ((newValue == null && this.biometricUserId != null) || (newValue != null && !newValue.equals(this.biometricUserId))) this.biometricUserId = newValue; }
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}