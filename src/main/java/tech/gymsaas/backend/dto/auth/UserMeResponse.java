package tech.gymsaas.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserMeResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Boolean active;
    private Long gymId;
    private String gymName;
    private LocalDate accessEndDate;
}