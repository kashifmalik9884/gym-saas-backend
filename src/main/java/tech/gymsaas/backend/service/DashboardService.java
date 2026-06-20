package tech.gymsaas.backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tech.gymsaas.backend.dto.dashboard.DashboardSummaryDto;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.exception.ResourceNotFoundException;
import tech.gymsaas.backend.repository.AttendanceRepository;
import tech.gymsaas.backend.repository.MemberRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public DashboardService(MemberRepository memberRepository,
                            AttendanceRepository attendanceRepository,
                            UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    public DashboardSummaryDto getSummary(Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);

        LocalDate today = LocalDate.now();
        LocalDate dueLimit = today.plusDays(2);

        long totalMembers = memberRepository.countByGym_IdAndActiveTrue(gymId);
        long todayAttendance = attendanceRepository.countByGym_IdAndDate(gymId, today);
        long dueSoon = memberRepository.countByGym_IdAndNextDueDateBetweenAndActiveTrue(gymId, today, dueLimit);
        long unreadMessages = 0;

        return DashboardSummaryDto.builder()
                .totalMembers(totalMembers)
                .todayAttendance(todayAttendance)
                .dueSoon(dueSoon)
                .unreadMessages(unreadMessages)
                .build();
    }

    public List<Member> getDueSoonMembers(Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);

        LocalDate today = LocalDate.now();
        LocalDate dueLimit = today.plusDays(2);

        return memberRepository
                .findByGym_IdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                        gymId,
                        today,
                        dueLimit
                );
    }

    private Gym getCurrentGym(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Gym gym = user.getGym();
        if (gym == null) {
            throw new ResourceNotFoundException("Gym not linked to user");
        }

        return gym;
    }

    private Long getCurrentGymId(Authentication authentication) {
        return getCurrentGym(authentication).getId();
    }
}