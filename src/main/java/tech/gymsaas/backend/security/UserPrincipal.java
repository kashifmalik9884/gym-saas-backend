package tech.gymsaas.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class UserPrincipal extends User {
    private final Long userId;
    private final Long gymId;

    public UserPrincipal(String username, String password, Collection<? extends GrantedAuthority> authorities, Long userId, Long gymId) {
        super(username, password, authorities);
        this.userId = userId;
        this.gymId = gymId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGymId() {
        return gymId;
    }
}