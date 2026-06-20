package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.gymsaas.backend.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByGymIdAndRole(Long gymId, String role);
    Optional<User> findByEmail(String email);
}