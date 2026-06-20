package tech.gymsaas.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 120)
    private String memberName;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime punchIn;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(length = 50)
    private String deviceId;

    @Column(length = 500)
    private String note;

    @Column(length = 100)
    private String editedBy;

    private LocalDateTime editedAt;

    @PrePersist
    public void prePersist() {
        normalizeStrings();

        if (status == null || status.isBlank()) {
            status = "present";
        }

        if (source == null || source.isBlank()) {
            source = "manual";
        }

        if (date == null) {
            date = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        normalizeStrings();
    }

    private void normalizeStrings() {
        memberName = trimToNull(memberName);
        status = trimToNull(status);
        source = trimToNull(source);
        deviceId = trimToNull(deviceId);
        note = trimToNull(note);
        editedBy = trimToNull(editedBy);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}