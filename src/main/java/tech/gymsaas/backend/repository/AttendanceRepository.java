package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.gymsaas.backend.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByGym_IdOrderByDateDescIdDesc(Long gymId);

    List<Attendance> findByGym_IdAndDateOrderByIdDesc(Long gymId, LocalDate date);

    List<Attendance> findByGym_IdAndStatusIgnoreCaseOrderByDateDescIdDesc(Long gymId, String status);

    List<Attendance> findByGym_IdAndDateAndStatusIgnoreCaseOrderByIdDesc(Long gymId, LocalDate date, String status);

    Optional<Attendance> findByIdAndGym_Id(Long id, Long gymId);

    List<Attendance> findByMember_IdAndGym_IdOrderByDateDescIdDesc(Long memberId, Long gymId);

    long countByGym_IdAndDate(Long gymId, LocalDate date);
}