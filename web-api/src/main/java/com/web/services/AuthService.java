package com.web.services;

import com.web.app.controllers.auth.AuthResponse;
import com.web.app.controllers.auth.LoginRequest;
import com.web.app.controllers.auth.RegisterRequest;
import com.web.infra.db.entities.User;

import com.web.infra.db.repositories.UserRepository;
import com.web.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisService redisService;

    @Transactional
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

        redisService.SAVE_PLAYER_INFO(saved.getId(), saved.getUsername(), saved.getEmail(), 150);

        return new AuthResponse(jwtService.generateToken(saved));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new IllegalArgumentException("Invalid username or password");

        redisService.SAVE_PLAYER_INFO(user.getId(), user.getUsername(), user.getEmail(), 150);

        return new AuthResponse(jwtService.generateToken(user));
    }
}
