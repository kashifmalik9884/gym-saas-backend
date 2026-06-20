package tech.gymsaas.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.gymsaas.backend.entity.Gym;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.repository.GymRepository;
import tech.gymsaas.backend.repository.MemberRepository;
import tech.gymsaas.backend.repository.PaymentRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProtectedEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
        userRepository.deleteAll();
        gymRepository.deleteAll();
        paymentRepository.deleteAll();

        Gym gym = gymRepository.save(Gym.builder()
                .name("Test Gym")
                .slug("test-gym")
                .ownerName("Owner")
                .ownerEmail("owner@example.com")
                .status("active")
                .accessStartDate(LocalDate.now().minusDays(1))
                .accessEndDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        userRepository.save(User.builder()
                .fullName("Owner User")
                .email("owner@example.com")
                .password(passwordEncoder.encode("password123"))
                .role("OWNER")
                .active(true)
                .gym(gym)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void members_withoutToken_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = {"OWNER"})
    void members_withOwner_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk());
    }
}