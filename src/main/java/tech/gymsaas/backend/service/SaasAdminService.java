package tech.gymsaas.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.gymsaas.backend.dto.admin.*;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.GymRenewalLog;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.exception.BadRequestException;
import tech.gymsaas.backend.exception.ResourceNotFoundException;
import tech.gymsaas.backend.repository.GymRenewalLogRepository;
import tech.gymsaas.backend.repository.GymRepository;
import tech.gymsaas.backend.repository.SaasAdminUserRepository;
import tech.gymsaas.backend.repository.UserRepository;
import tech.gymsaas.backend.repository.RefreshTokenRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SaasAdminService {

    private final GymRepository gymRepository;
    private final GymRenewalLogRepository renewalLogRepository;
    private final UserRepository userRepository;
    private final SaasAdminUserRepository saasAdminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public SaasAdminService(GymRepository gymRepository,
                            GymRenewalLogRepository renewalLogRepository,
                            UserRepository userRepository,
                            SaasAdminUserRepository saasAdminUserRepository,
                            PasswordEncoder passwordEncoder) {
        this.gymRepository = gymRepository;
        this.renewalLogRepository = renewalLogRepository;
        this.userRepository = userRepository;
        this.saasAdminUserRepository = saasAdminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<GymResponse> getAllGyms() {
        return gymRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GymResponse> getExpiredGyms() {
        return gymRepository.findByAccessEndDateBeforeOrderByAccessEndDateAsc(LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 🌟 NEW: Subscription analysis engine. Computes metrics and aggregates items.
    @Transactional(readOnly = true)
    public RenewalDashboardResponse getRenewalDashboardMetrics() {
        List<Gym> gyms = gymRepository.findAll();

        long dueSoon = 0;
        long expired = 0;
        long renewed = 0;

        LocalDate today = LocalDate.now();
        List<RenewalQueueRespons> queueList = new ArrayList<>();

        for (Gym gym : gyms) {
            LocalDate end = gym.getAccessEndDate();
            String calculatedStatus = "renewed";

            if (end == null || "suspended".equalsIgnoreCase(gym.getStatus())) {
                calculatedStatus = "expired";
                expired++;
            } else if (end.isBefore(today)) {
                calculatedStatus = "expired";
                expired++;
            } else {
                long daysLeft = ChronoUnit.DAYS.between(today, end);
                if (daysLeft <= 7) { // Flag records expiring in 7 days or fewer as due-soon
                    calculatedStatus = "due-soon";
                    dueSoon++;
                } else {
                    renewed++;
                }
            }

            queueList.add(RenewalQueueRespons.builder()
                    .gymId(gym.getId())
                    .gymName(gym.getName())
                    .ownerName(gym.getOwnerName())
                    .accessEndDate(end)
                    .status(calculatedStatus)
                    .build());
        }

        return RenewalDashboardResponse.builder()
                .totalRecords(gyms.size())
                .dueSoonCount(dueSoon)
                .expiredCount(expired)
                .renewedCount(renewed)
                .queue(queueList)
                .build();
    }

    @Transactional(readOnly = true)
    public GymResponse getGymById(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));
        return toResponse(gym);
    }
  @Transactional
    public void deleteGym(Long gymId) {
    Gym gym = gymRepository.findById(gymId)
            .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));

    // 1. Find OWNER user for this gym
            var ownerOpt = userRepository.findByGymIdAndRole(gymId, "OWNER");

            ownerOpt.ifPresent(owner -> {
        // 2. Delete all refresh tokens for this user to satisfy FK constraint
            refreshTokenRepository.deleteByUser_Id(owner.getId());

        // 3. Delete the owner user
            userRepository.delete(owner);
            });

    // 4. Delete renewal logs for this gym (if FK exists)
        renewalLogRepository.deleteByGym_Id(gymId);

    // 5. Finally delete the gym
        gymRepository.delete(gym);
}

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public GymResponse createGym(CreateGymRequest request) {
        String gymName = safeTrim(request.getName());
        String ownerName = safeTrim(request.getOwnerName());
        String ownerEmail = normalizeEmail(request.getOwnerEmail());
        String ownerPassword = request.getOwnerPassword() == null ? "" : request.getOwnerPassword().trim();
        String slug = normalizeSlug(request.getSlug());

        if (gymName.isBlank()) throw new BadRequestException("Gym name is required");
        if (slug.isBlank()) throw new BadRequestException("Gym slug is required");
        if (ownerName.isBlank()) throw new BadRequestException("Owner name is required");
        if (ownerEmail.isBlank()) throw new BadRequestException("Owner email is required");
        if (ownerPassword.isBlank()) throw new BadRequestException("Owner password is required");
        if (request.getAccessStartDate() == null || request.getAccessEndDate() == null) {
            throw new BadRequestException("Access dates are required");
        }
        if (request.getAccessEndDate().isBefore(request.getAccessStartDate())) {
            throw new BadRequestException("Access end date cannot be before access start date");
        }

        if (gymRepository.existsBySlug(slug)) {
            throw new BadRequestException("Gym slug already exists");
        }
        if (gymRepository.existsByOwnerEmailIgnoreCase(ownerEmail)) {
            throw new BadRequestException("Owner email already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(ownerEmail)) {
            throw new BadRequestException("A user with this email already exists");
        }
        if (saasAdminUserRepository.existsByEmailIgnoreCase(ownerEmail)) {
            throw new BadRequestException("A SaaS admin with this email already exists");
        }

        Gym gym = Gym.builder()
                .name(gymName)
                .slug(slug)
                .ownerName(ownerName)
                .ownerEmail(ownerEmail)
                .status("active")
                .accessStartDate(request.getAccessStartDate())
                .accessEndDate(request.getAccessEndDate())
                .lastRenewedAt(LocalDateTime.now())
                .build();

        gym = gymRepository.saveAndFlush(gym);

        User owner = User.builder()
                .gym(gym)
                .fullName(ownerName)
                .email(ownerEmail)
                .password(passwordEncoder.encode(ownerPassword))
                .role("OWNER")
                .active(true)
                .lastLoginAt(null)
                .build();

        userRepository.saveAndFlush(owner);

        return toResponse(gym);
    }

    @Transactional
    public GymResponse updateGym(Long gymId, CreateGymRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with ID: " + gymId));

        String gymName = safeTrim(request.getName());
        String ownerName = safeTrim(request.getOwnerName());
        String ownerEmail = normalizeEmail(request.getOwnerEmail());
        String slug = normalizeSlug(request.getSlug());

        if (gymName.isBlank()) throw new BadRequestException("Gym name is required");
        if (slug.isBlank()) throw new BadRequestException("Gym slug is required");
        if (ownerName.isBlank()) throw new BadRequestException("Owner name is required");
        if (ownerEmail.isBlank()) throw new BadRequestException("Owner email is required");

        if (!gym.getSlug().equalsIgnoreCase(slug) && gymRepository.existsBySlug(slug)) {
            throw new BadRequestException("Gym slug already exists");
        }
        if (!gym.getOwnerEmail().equalsIgnoreCase(ownerEmail)) {
            if (gymRepository.existsByOwnerEmailIgnoreCase(ownerEmail) ||
                    userRepository.existsByEmailIgnoreCase(ownerEmail) ||
                    saasAdminUserRepository.existsByEmailIgnoreCase(ownerEmail)) {
                throw new BadRequestException("Owner email already exists elsewhere in the system");
            }
        }

        gym.setName(gymName);
        gym.setSlug(slug);
        gym.setOwnerName(ownerName);
        gym.setOwnerEmail(ownerEmail);
        gym.setAccessStartDate(request.getAccessStartDate());
        gym.setAccessEndDate(request.getAccessEndDate());
        gym = gymRepository.saveAndFlush(gym);

        User owner = userRepository.findByGymIdAndRole(gymId, "OWNER")
                .orElse(User.builder().gym(gym).role("OWNER").active(true).build());

        owner.setFullName(ownerName);
        owner.setEmail(ownerEmail);

        if (request.getOwnerPassword() != null && !request.getOwnerPassword().trim().isBlank()) {
            owner.setPassword(passwordEncoder.encode(request.getOwnerPassword().trim()));
        }

        userRepository.saveAndFlush(owner);

        return toResponse(gym);
    }

    @Transactional
    public GymResponse renewGym(Long gymId, RenewGymRequest request, String adminEmail) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));

        if (request.getNewAccessEndDate().isBefore(gym.getAccessEndDate())) {
            throw new BadRequestException("New access end date must be on or after current end date");
        }

        LocalDate previousEndDate = gym.getAccessEndDate();

        gym.setAccessEndDate(request.getNewAccessEndDate());
        gym.setLastRenewedAt(LocalDateTime.now());
        gym.setStatus("active");

        gym = gymRepository.saveAndFlush(gym);

        GymRenewalLog log = GymRenewalLog.builder()
                .gym(gym)
                .previousEndDate(previousEndDate)
                .newEndDate(request.getNewAccessEndDate())
                .renewedByEmail(adminEmail)
                .note(request.getNote())
                .build();

        renewalLogRepository.saveAndFlush(log);

        return toResponse(gym);
    }

    @Transactional
    public GymResponse suspendGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));
        gym.setStatus("suspended");
        return toResponse(gymRepository.saveAndFlush(gym));
    }


    @Transactional
    public void updateAdminProfileDetails(String currentAdminEmail, tech.gymsaas.backend.dto.admin.AdminProfileRequest request) {
        // 1. Find the existing admin from the database using their current authentication email
        var admin = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Admin account not found"));

        // 2. Apply the updated values from the frontend request form
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());

        // 3. Save changes back to your cloud data schema
        userRepository.save(admin);
    }

    @Transactional
    public GymResponse activateGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));
        gym.setStatus("active");
        return toResponse(gymRepository.saveAndFlush(gym));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSlug(String value) {
        String slug = value == null ? "" : value.trim().toLowerCase();
        slug = slug.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug;
    }

    private GymResponse toResponse(Gym gym) {
        return GymResponse.builder()
                .id(gym.getId())
                .name(gym.getName())
                .slug(gym.getSlug())
                .ownerName(gym.getOwnerName())
                .ownerEmail(gym.getOwnerEmail())
                .status(gym.getStatus())
                .accessStartDate(gym.getAccessStartDate())
                .accessEndDate(gym.getAccessEndDate())
                .lastRenewedAt(gym.getLastRenewedAt())
                .build();
    }
}
