package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.gymsaas.backend.entity.RefreshToken;
import tech.gymsaas.backend.entity.User;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
     void deleteByUser_Id(Long userId);
}
