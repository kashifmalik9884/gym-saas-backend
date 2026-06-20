package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.gymsaas.backend.entity.Gym;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GymRepository extends JpaRepository<Gym, Long> {
    Optional<Gym> findBySlug(String slug);
    Optional<Gym> findByOwnerEmailIgnoreCase(String ownerEmail);
    boolean existsBySlug(String slug);
    boolean existsByOwnerEmailIgnoreCase(String ownerEmail);
    List<Gym> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
    List<Gym> findByAccessEndDateBeforeOrderByAccessEndDateAsc(LocalDate date);
}