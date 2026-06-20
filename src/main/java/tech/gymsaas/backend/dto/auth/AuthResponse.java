package tech.gymsaas.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Long gymId;
    private String gymName;
    private LocalDate accessEndDate;
    private Boolean active;
}