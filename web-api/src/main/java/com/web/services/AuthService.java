package com.web.services;

import com.web.controllers.auth.AuthResponse;
import com.web.controllers.auth.LoginRequest;
import com.web.controllers.auth.RegisterRequest;
import com.web.db.entities.User;

import com.web.db.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new IllegalArgumentException("Username already taken: " + req.username());

        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Email already registered: " + req.email());

        User saved = userRepository.save(
                User.builder()
                        .username(req.username())
                        .email(req.email())
                        .password(passwordEncoder.encode(req.password()))
                        .build()
        );

        return new AuthResponse(jwtService.generateToken(saved));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new IllegalArgumentException("Invalid username or password");

        return new AuthResponse(jwtService.generateToken(user));
    }
}
