package com.web.configandsecurity.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public final SecurityPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityPrincipal userPrincipal = (SecurityPrincipal) authentication.getPrincipal();
        return userPrincipal;
    }

    public final long getPlayerId(){
        return getPrincipal().getPlayerId();
    }
    public final String getPlayerUsername(){
        return getPrincipal().getUsername();
    }
    public final String getPlayerEmail(){
        return getPrincipal().getEmail();
    }
    public final long getPlayerId(SecurityPrincipal principal){
        return principal.getPlayerId();
    }
    public final String getPlayerUsername(SecurityPrincipal principal){
        return principal.getUsername();
    }
    public final String getPlayerEmail(SecurityPrincipal principal){
        return principal.getEmail();
    }
}
