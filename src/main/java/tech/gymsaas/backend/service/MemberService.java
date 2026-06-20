package tech.gymsaas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tech.gymsaas.backend.dto.member.MemberRequest;
import tech.gymsaas.backend.dto.member.MemberResponse;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.exception.ResourceNotFoundException;
import tech.gymsaas.backend.repository.MemberRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final tech.gymsaas.backend.repository.GymRepository gymRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.base-url}")
    private String uploadBaseUrl;

    public MemberService(MemberRepository memberRepository,
                         UserRepository userRepository,
                         tech.gymsaas.backend.repository.GymRepository gymRepository) { // 2. Update constructor
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.gymRepository = gymRepository;
    }

    public List<MemberResponse> getAllMembers(Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        return memberRepository.findByGym_IdOrderByCreatedAtDesc(gymId)
                .stream()
                .map(member -> this.toResponse(member, gymId)) // <-- Pass gymId here
                .toList();
    }

    public MemberResponse getMemberById(Long id, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        Member member = memberRepository.findByIdAndGym_Id(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        return toResponse(member, gymId); // <-- Pass gymId here
    }

    public List<MemberResponse> getDueSoonMembers(Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        LocalDate today = LocalDate.now();
        LocalDate dueLimit = today.plusDays(2);

        return memberRepository
                .findByGym_IdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(gymId, today, dueLimit)
                .stream()
                .map(member -> this.toResponse(member, gymId)) // <-- Pass gymId here
                .toList();
    }

    public MemberResponse createMember(MemberRequest request, MultipartFile photo, Authentication authentication) {
        Gym gym = getCurrentGym(authentication);

        Member member = Member.builder()
                .gym(gym)
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .monthlyFee(request.getMonthlyFee())
                .joiningDate(request.getJoiningDate())
                .billingDay(request.getBillingDay())
                .nextDueDate(request.getNextDueDate())
                .lastPaidDate(request.getLastPaidDate())
                .paymentStatus(request.getPaymentStatus())
                .active(request.getActive())
                .photoUrl(request.getPhotoUrl())
                .photoFileId(request.getPhotoFileId())
                .biometricUserId(request.getBiometricUserId())
                .build();

        applyDefaults(member, true);

        if (photo != null && !photo.isEmpty()) {
            member.setPhotoUrl(storePhoto(photo));
        }

        return toResponse(memberRepository.save(member), gym.getId());
    }

    public MemberResponse updateMember(Long id,
                                       MemberRequest request,
                                       MultipartFile photo,
                                       Authentication authentication) {
        Gym gym = getCurrentGym(authentication);

        Member existing = memberRepository.findByIdAndGym_Id(id, gym.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        LocalDate previousJoiningDate = existing.getJoiningDate();

        existing.setName(request.getName().trim());
        existing.setPhone(request.getPhone().trim());
        existing.setMonthlyFee(request.getMonthlyFee());
        existing.setJoiningDate(request.getJoiningDate());
        existing.setLastPaidDate(request.getLastPaidDate());
        existing.setPaymentStatus(request.getPaymentStatus());
        existing.setActive(request.getActive());
        existing.setPhotoFileId(request.getPhotoFileId());
        existing.setBiometricUserId(request.getBiometricUserId());

        if (photo != null && !photo.isEmpty()) {
            existing.setPhotoUrl(storePhoto(photo));
        } else {
            existing.setPhotoUrl(request.getPhotoUrl());
        }

        existing.setBillingDay(request.getBillingDay());
        existing.setNextDueDate(request.getNextDueDate());

        applyDefaults(existing, false);

        if (existing.getJoiningDate() != null
                && previousJoiningDate != null
                && !existing.getJoiningDate().equals(previousJoiningDate)
                && existing.getNextDueDate() == null) {
            int billingDay = existing.getBillingDay() != null
                    ? existing.getBillingDay()
                    : existing.getJoiningDate().getDayOfMonth();
            existing.setNextDueDate(calculateNextDueDate(existing.getJoiningDate(), billingDay));
        }

        return toResponse(memberRepository.save(existing), gym.getId());
    }

    public void deleteMember(Long id, Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);
        Member member = memberRepository.findByIdAndGym_Id(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        memberRepository.delete(member);
    }

    private Gym getCurrentGym(Authentication authentication) {
        Long gymId = getCurrentGymId(authentication);

        // Safely fetch the Gym record explicitly by its ID to avoid lazy loading proxy crashes
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym account not found with ID: " + gymId));
    }

    private Long getCurrentGymId(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();

        // Direct check for your optimized custom principal
        if (principal instanceof tech.gymsaas.backend.security.UserPrincipal) {
            Long gymId = ((tech.gymsaas.backend.security.UserPrincipal) principal).getGymId();
            if (gymId != null) {
                return gymId; // 🌟 Returns instantly without hitting the database a second time!
            }
        }

        // Fallback profile lookups should be avoided if possible
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> {
                    if (user.getGym() == null) throw new ResourceNotFoundException("Gym not linked");
                    return user.getGym().getId();
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void applyDefaults(Member member, boolean isCreate) {
        if (member.getPaymentStatus() == null || member.getPaymentStatus().isBlank()) {
            member.setPaymentStatus("pending");
        }

        if (member.getActive() == null) {
            member.setActive(true);
        }

        if (member.getBillingDay() == null && member.getJoiningDate() != null) {
            member.setBillingDay(member.getJoiningDate().getDayOfMonth());
        }

        if (isCreate && member.getNextDueDate() == null && member.getJoiningDate() != null) {
            member.setNextDueDate(calculateNextDueDate(member.getJoiningDate(), member.getBillingDay()));
        }
    }

    private LocalDate calculateNextDueDate(LocalDate baseDate, Integer billingDay) {
        LocalDate nextMonth = baseDate.plusMonths(1);
        int targetDay = billingDay != null ? billingDay : baseDate.getDayOfMonth();
        int maxDay = YearMonth.from(nextMonth).lengthOfMonth();
        return nextMonth.withDayOfMonth(Math.min(targetDay, maxDay));
    }

    private String storePhoto(MultipartFile photo) {
        try {
            String originalName = photo.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return uploadBaseUrl + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store photo");
        }
    }

    private MemberResponse toResponse(Member member, Long fallbackGymId) {
        return MemberResponse.builder()
                .id(member.getId())
                .gymId(fallbackGymId) // 🌟 Direct assignment! No proxy triggers, no extra database hits.
                .name(member.getName())
                .phone(member.getPhone())
                .monthlyFee(member.getMonthlyFee())
                .joiningDate(member.getJoiningDate())
                .billingDay(member.getBillingDay())
                .nextDueDate(member.getNextDueDate())
                .lastPaidDate(member.getLastPaidDate())
                .paymentStatus(member.getPaymentStatus())
                .active(member.getActive())
                .photoUrl(member.getPhotoUrl())
                .photoFileId(member.getPhotoFileId())
                .biometricUserId(member.getBiometricUserId())
                .build();
    }
}