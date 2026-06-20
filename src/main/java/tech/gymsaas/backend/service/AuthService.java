package tech.gymsaas.backend.service;

import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.gymsaas.backend.dto.auth.*;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.RefreshToken;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.exception.BadRequestException;
import tech.gymsaas.backend.exception.ResourceNotFoundException;
import tech.gymsaas.backend.repository.GymRepository;
import tech.gymsaas.backend.repository.SaasAdminUserRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final SaasAdminUserRepository saasAdminUserRepository;

    public AuthService(UserRepository userRepository,
                       GymRepository gymRepository,
                       SaasAdminUserRepository saasAdminUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.gymRepository = gymRepository;
        this.saasAdminUserRepository = saasAdminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    public String ping() {
        return "Auth API is working";
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        throw new BadRequestException("Use SaaS admin dashboard to create a gym and its owner account");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var saasAdminOpt = saasAdminUserRepository.findByEmailIgnoreCase(request.getEmail());
        if (saasAdminOpt.isPresent()) {
            var admin = saasAdminOpt.get();

            if (!Boolean.TRUE.equals(admin.getIsActive())) {
                throw new BadRequestException("This super admin account is disabled");
            }

            String accessToken = jwtService.generateSuperAdminToken(admin);

            return new AuthResponse(
                    accessToken,
                    null,
                    "Bearer",
                    admin.getId(),
                    admin.getFullName(),
                    admin.getEmail(),
                    "SUPER_ADMIN",
                    null,
                    null,
                    null,
                    admin.getIsActive()
            );
        }

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateActiveGymUser(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createOrReplaceRefreshToken(user);
        Gym gym = user.getGym();

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                gym.getId(),
                gym.getName(),
                gym.getAccessEndDate(),
                user.getActive()
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken verifiedToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = verifiedToken.getUser();

        validateActiveGymUser(user);

        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        String newAccessToken = jwtService.generateToken(user);
        Gym gym = user.getGym();

        return new AuthResponse(
                newAccessToken,
                rotatedToken.getToken(),
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                gym.getId(),
                gym.getName(),
                gym.getAccessEndDate(),
                user.getActive()
        );
    }

    public void logout(String refreshTokenValue, String email) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenService.revokeByToken(refreshTokenValue);
            return;
        }

        if (email != null && !email.isBlank()) {
            var saasAdminOpt = saasAdminUserRepository.findByEmailIgnoreCase(email);
            if (saasAdminOpt.isPresent()) {
                return;
            }

            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            refreshTokenService.revokeByUser(user);
        }
    }

    public UserMeResponse getCurrentUser(String email) {
        var saasAdminOpt = saasAdminUserRepository.findByEmailIgnoreCase(email);
        if (saasAdminOpt.isPresent()) {
            var admin = saasAdminOpt.get();

            return new UserMeResponse(
                    admin.getId(),
                    admin.getFullName(),
                    admin.getEmail(),
                    "SUPER_ADMIN",
                    admin.getIsActive(),
                    null,
                    null,
                    null
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Gym gym = user.getGym();

        return new UserMeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                gym != null ? gym.getId() : null,
                gym != null ? gym.getName() : null,
                gym != null ? gym.getAccessEndDate() : null
        );
    }

    private void validateActiveGymUser(User user) {
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("This user account is disabled");
        }

        Gym gym = user.getGym();
        if (gym == null) {
            throw new BadRequestException("Gym account not linked to this user");
        }

        if ("suspended".equalsIgnoreCase(gym.getStatus())) {
            throw new BadRequestException("This gym account is suspended");
        }

        if (gym.isExpired()) {
            throw new BadRequestException("Your 1-year access has expired. Contact SaaS admin for renewal");
        }

        if (!"active".equalsIgnoreCase(gym.getStatus())) {
            throw new BadRequestException("This gym account is not active");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateUniqueSlug(String gymName) {
        String base = gymName.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (base.isBlank()) {
            base = "gym";
        }

        String slug = base;
        int counter = 1;

        while (gymRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }

        return slug;
    }
}