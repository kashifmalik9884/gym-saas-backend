package tech.gymsaas.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gyms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, unique = true, length = 150)
    private String ownerEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDate accessStartDate;

    @Column(nullable = false)
    private LocalDate accessEndDate;

    private LocalDateTime lastRenewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        String targetStatus = lowerTrimToNull(this.status);
        if (targetStatus == null) {
            targetStatus = "active";
        }

        if (this.accessStartDate == null) {
            this.accessStartDate = LocalDate.now();
        }

        if (this.accessEndDate == null) {
            this.accessEndDate = this.accessStartDate.plusYears(1);
        }

        updateIfChanged("name", trimToNull(this.name));
        updateIfChanged("slug", lowerTrimToNull(this.slug));
        updateIfChanged("ownerName", trimToNull(this.ownerName));
        updateIfChanged("ownerEmail", lowerTrimToNull(this.ownerEmail));
        updateIfChanged("status", targetStatus);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        updateIfChanged("name", trimToNull(this.name));
        updateIfChanged("slug", lowerTrimToNull(this.slug));
        updateIfChanged("ownerName", trimToNull(this.ownerName));
        updateIfChanged("ownerEmail", lowerTrimToNull(this.ownerEmail));
        updateIfChanged("status", lowerTrimToNull(this.status));
    }

    private void updateIfChanged(String field, String newValue) {
        switch (field) {
            case "name" -> { if ((newValue == null && this.name != null) || (newValue != null && !newValue.equals(this.name))) this.name = newValue; }
            case "slug" -> { if ((newValue == null && this.slug != null) || (newValue != null && !newValue.equals(this.slug))) this.slug = newValue; }
            case "ownerName" -> { if ((newValue == null && this.ownerName != null) || (newValue != null && !newValue.equals(this.ownerName))) this.ownerName = newValue; }
            case "ownerEmail" -> { if ((newValue == null && this.ownerEmail != null) || (newValue != null && !newValue.equals(this.ownerEmail))) this.ownerEmail = newValue; }
            case "status" -> { if ((newValue == null && this.status != null) || (newValue != null && !newValue.equals(this.status))) this.status = newValue; }
        }
    }

    public boolean isExpired() {
        return accessEndDate != null && accessEndDate.isBefore(LocalDate.now());
    }

    public boolean isSuspended() {
        return "suspended".equalsIgnoreCase(this.status);
    }

    public boolean isActiveTenant() {
        return !isSuspended() && !isExpired();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String lowerTrimToNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }
}