package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.gymsaas.backend.entity.GymRenewalLog;

import java.util.List;

public interface GymRenewalLogRepository extends JpaRepository<GymRenewalLog, Long> {
    List<GymRenewalLog> findByGym_IdOrderByCreatedAtDesc(Long gymId);
    void deleteByGymId(Long gymId);
}
