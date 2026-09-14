package com.campaignorganizer.security;

import com.campaignorganizer.accounts.domain.account.Role;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    @Override
    public UUID currentAccountId() {
        return (UUID) authentication().getPrincipal();
    }

    @Override
    public Role currentRole() {
        return authentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated principal has no role"));
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated principal in the security context");
        }
        return authentication;
    }
}
