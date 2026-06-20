package tech.gymsaas.backend.security;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.repository.SaasAdminUserRepository;
import tech.gymsaas.backend.repository.UserRepository;
import tech.gymsaas.backend.repository.GymRepository; // Ensure this import exists

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SaasAdminUserRepository saasAdminUserRepository;
    private final GymRepository gymRepository; // 1. Added explicit repo dependency

    public CustomUserDetailsService(
            UserRepository userRepository,
            SaasAdminUserRepository saasAdminUserRepository,
            GymRepository gymRepository
    ) {
        this.userRepository = userRepository;
        this.saasAdminUserRepository = saasAdminUserRepository;
        this.gymRepository = gymRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("=== CUSTOM USER DETAILS SERVICE CALLED ===");
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        System.out.println("TARGET EMAIL = [" + normalizedEmail + "]");

        // 1. Check SaaS Super Admin Table
        var saasAdmin = saasAdminUserRepository.findByEmailIgnoreCase(normalizedEmail);
        if (saasAdmin.isPresent()) {
            var admin = saasAdmin.get();
            if (admin.getIsActive() != null && !Boolean.TRUE.equals(admin.getIsActive())) {
                throw new DisabledException("This super admin account is disabled");
            }


            return new UserPrincipal(
                    admin.getEmail(),
                    admin.getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                    admin.getId(),
                    null
            );
        }

        // 2. Fallback to App User Table
        System.out.println("FALLING BACK TO USERS TABLE LOOKUP...");
        tech.gymsaas.backend.entity.User appUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        System.out.println("APP USER FOUND: ID = " + appUser.getId() + ", ROLE = " + appUser.getRole() + ", ACTIVE = " + appUser.getActive());

        if (appUser.getActive() != null && !Boolean.TRUE.equals(appUser.getActive())) {
            throw new DisabledException("This user account is disabled");
        }

        // 2. Fetch the Gym profile via explicit repository to avoid LazyInitializationException session issues
        Gym gym = null;
        if (appUser.getGym() != null) {
            // Safe fallback lookup using the ID direct mapping
            gym = gymRepository.findById(appUser.getGym().getId()).orElse(null);
        }

        if (gym == null) {
            System.out.println("REJECTED: Gym account not linked to this user profile.");
            throw new LockedException("Gym account not linked to this user");
        }

        String gymStatus = gym.getStatus() != null ? gym.getStatus().trim().toLowerCase() : "";
        System.out.println("LINKED TENANT GYM DETAILS: ID = " + gym.getId() + ", STATUS = " + gymStatus);

        if ("suspended".equals(gymStatus)) {
            throw new LockedException("This gym account is suspended");
        }

        try {
            if (gym.isExpired()) {
                throw new AccountExpiredException("Your access has expired. Contact SaaS admin for renewal");
            }
        } catch (Exception e) {
            System.out.println("Warning: Gym expiration check optimization skipped: " + e.getMessage());
        }

        String targetRole = appUser.getRole() != null ? appUser.getRole().trim().toUpperCase() : "OWNER";
        System.out.println("SUCCESSFULLY CONSTRUCTING USER PRINCIPAL WITH ROLE: ROLE_" + targetRole);

        return new UserPrincipal(
                appUser.getEmail(),
                appUser.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + targetRole)),
                appUser.getId(),
                gym.getId()
        );
    }
}