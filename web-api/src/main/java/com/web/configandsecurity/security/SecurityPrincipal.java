package com.web.configandsecurity.security;

import lombok.*;
import org.springframework.security.core.AuthenticatedPrincipal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SecurityPrincipal implements AuthenticatedPrincipal {
    private long playerId;
    private String username;
    private String email;

    @Override
    public String getName() {
        return username;
    }
}