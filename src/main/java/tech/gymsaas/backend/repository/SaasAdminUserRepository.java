package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.gymsaas.backend.entity.SaasAdminUser;

import java.util.Optional;

public interface SaasAdminUserRepository extends JpaRepository<SaasAdminUser, Long> {
    Optional<SaasAdminUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}