package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.gymsaas.backend.entity.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m JOIN FETCH m.gym WHERE m.gym.id = :gymId ORDER BY m.createdAt DESC")
    List<Member> findByGym_IdOrderByCreatedAtDesc(@Param("gymId") Long gymId);

    List<Member> findByGym_IdAndActiveTrueOrderByCreatedAtDesc(Long gymId);

    Optional<Member> findByIdAndGym_Id(Long id, Long gymId);

    List<Member> findByGym_IdAndNextDueDateBetween(Long gymId, LocalDate startDate, LocalDate endDate);

    List<Member> findByGym_IdAndActiveTrueAndNextDueDateBetween(
            Long gymId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Member> findByGym_IdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
            Long gymId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByGym_Id(Long gymId);

    long countByGym_IdAndActiveTrue(Long gymId);

    long countByGym_IdAndNextDueDateBetween(Long gymId, LocalDate startDate, LocalDate endDate);

    long countByGym_IdAndNextDueDateBetweenAndActiveTrue(
            Long gymId,
            LocalDate startDate,
            LocalDate endDate
    );
}