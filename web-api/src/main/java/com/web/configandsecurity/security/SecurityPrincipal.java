package com.web.configandsecurity.security;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SecurityPrincipal {
    private long playerId;
    private String username;
    private String email;
}
