package com.web.configandsecurity.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;

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
    public final String getPlayerUsername(){ return getPrincipal().getUsername(); }
    public final String getPlayerEmail(){
        return getPrincipal().getEmail();
    }
    public final SecurityPrincipal getPrincipal(Principal principal){
        Authentication auth=(Authentication)principal;
        SecurityPrincipal userPrincipal = (SecurityPrincipal) auth.getPrincipal();
        return userPrincipal;
    }
    public final long getPlayerId(Principal principal){
        return getPrincipal(principal).getPlayerId();

    }
    public final String getPlayerUsername(Principal principal){
        return getPrincipal(principal).getUsername();
    }
    public final String getPlayerEmail(Principal principal){
        return getPrincipal(principal).getEmail();
    }
}
