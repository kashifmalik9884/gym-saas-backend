package tech.gymsaas.backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tech.gymsaas.backend.dto.attendance.AttendanceRequest;
import tech.gymsaas.backend.dto.attendance.AttendanceResponse;
import tech.gymsaas.backend.entity.Attendance;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.exception.ResourceNotFoundException;
import tech.gymsaas.backend.repository.AttendanceRepository;
import tech.gymsaas.backend.repository.MemberRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             MemberRepository memberRepository,
                             UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public List<AttendanceResponse> getAttendance(LocalDate date, String status, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);

        boolean hasDate = date != null;
        boolean hasStatus = status != null && !status.isBlank() && !"all".equalsIgnoreCase(status);

        List<Attendance> records;

        if (hasDate && hasStatus) {
            records = attendanceRepository.findByGym_IdAndDateAndStatusIgnoreCaseOrderByIdDesc(
                    gymId, date, status.trim()
            );
        } else if (hasDate) {
            records = attendanceRepository.findByGym_IdAndDateOrderByIdDesc(gymId, date);
        } else if (hasStatus) {
            records = attendanceRepository.findByGym_IdAndStatusIgnoreCaseOrderByDateDescIdDesc(
                    gymId, status.trim()
            );
        } else {
            records = attendanceRepository.findByGym_IdOrderByDateDescIdDesc(gymId);
        }

        return records.stream().map(this::toResponse).toList();
    }

    public AttendanceResponse getAttendanceById(Long id, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        Attendance attendance = attendanceRepository.findByIdAndGym_Id(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        return toResponse(attendance);
    }

    public List<AttendanceResponse> getAttendanceByMemberId(Long memberId, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        return attendanceRepository.findByMember_IdAndGym_IdOrderByDateDescIdDesc(memberId, gymId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AttendanceResponse createAttendance(AttendanceRequest request, Authentication authentication) {
        Gym gym = getCurrentGym(authentication);
        Member member = memberRepository.findByIdAndGym_Id(request.getMemberId(), gym.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        Attendance attendance = Attendance.builder()
                .gym(gym)
                .member(member)
                .memberName(member.getName())
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .punchIn(request.getPunchIn())
                .status(request.getStatus())
                .source(request.getSource())
                .deviceId(request.getDeviceId())
                .note(request.getNote())
                .build();

        normalizeAttendance(attendance, false);

        return toResponse(attendanceRepository.save(attendance));
    }

    public AttendanceResponse updateAttendance(Long id,
                                               AttendanceRequest request,
                                               Authentication authentication) {
        Gym gym = getCurrentGym(authentication);

        Attendance existing = attendanceRepository.findByIdAndGym_Id(id, gym.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        Member member = memberRepository.findByIdAndGym_Id(request.getMemberId(), gym.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        existing.setGym(gym);
        existing.setMember(member);
        existing.setMemberName(member.getName());
        existing.setDate(request.getDate() != null ? request.getDate() : existing.getDate());
        existing.setPunchIn(request.getPunchIn());
        existing.setStatus(request.getStatus());
        existing.setSource(request.getSource());
        existing.setDeviceId(request.getDeviceId());
        existing.setNote(request.getNote());
        existing.setEditedBy(authentication.getName());
        existing.setEditedAt(LocalDateTime.now());

        normalizeAttendance(existing, true);

        return toResponse(attendanceRepository.save(existing));
    }

    public void deleteAttendance(Long id, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        Attendance attendance = attendanceRepository.findByIdAndGym_Id(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        attendanceRepository.delete(attendance);
    }

    public AttendanceResponse createDemoScan(Long memberId, Authentication authentication) {
        Gym gym = getCurrentGym(authentication);
        Member member = memberRepository.findByIdAndGym_Id(memberId, gym.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        Attendance attendance = Attendance.builder()
                .gym(gym)
                .member(member)
                .memberName(member.getName())
                .date(LocalDate.now())
                .punchIn(LocalTime.now().withSecond(0).withNano(0))
                .status("present")
                .source("demo-scan")
                .deviceId("demo-device-1")
                .note("Demo biometric scan")
                .build();

        normalizeAttendance(attendance, false);

        return toResponse(attendanceRepository.save(attendance));
    }

    private Gym getCurrentGym(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getGym() == null) {
            throw new ResourceNotFoundException("Gym not linked to user");
        }

        return user.getGym();
    }

    private Long getCurrentGymId(Authentication authentication) {
        return getCurrentGym(authentication).getId();
    }

    private void normalizeAttendance(Attendance attendance, boolean isUpdate) {
        if (attendance.getMemberName() != null) {
            attendance.setMemberName(attendance.getMemberName().trim());
        }

        if (attendance.getStatus() == null || attendance.getStatus().isBlank()) {
            attendance.setStatus("present");
        } else {
            attendance.setStatus(attendance.getStatus().trim().toLowerCase());
        }

        if (attendance.getSource() == null || attendance.getSource().isBlank()) {
            attendance.setSource("manual");
        } else {
            attendance.setSource(attendance.getSource().trim().toLowerCase());
        }

        if (attendance.getDeviceId() != null && attendance.getDeviceId().isBlank()) {
            attendance.setDeviceId(null);
        }

        if (attendance.getNote() != null && attendance.getNote().isBlank()) {
            attendance.setNote(null);
        }

        if (isUpdate && attendance.getEditedAt() == null) {
            attendance.setEditedAt(LocalDateTime.now());
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .gymId(attendance.getGym().getId())
                .memberId(attendance.getMember().getId())
                .memberName(attendance.getMemberName())
                .date(attendance.getDate())
                .punchIn(attendance.getPunchIn())
                .status(attendance.getStatus())
                .source(attendance.getSource())
                .deviceId(attendance.getDeviceId())
                .note(attendance.getNote())
                .editedBy(attendance.getEditedBy())
                .build();
    }
}