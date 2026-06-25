package com.marketplace.api.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.marketplace.api.dto.request.AuthRequest;
import com.marketplace.api.dto.request.RefreshTokenRequest;
import com.marketplace.api.dto.request.RegisterRequest;
import com.marketplace.api.dto.response.AuthResponse;
import com.marketplace.api.entity.RefreshToken;
import com.marketplace.api.entity.User;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.UserRepository;
import com.marketplace.api.service.security.JwtService;
import com.marketplace.api.service.security.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already registered: {}", request.getEmail());
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        userRepository.save(user);
        log.info("User registered successfully: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authentication attempt for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Authentication failed - user not found: {}", request.getEmail());
                    return new UsernameNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Authentication failed - invalid password for email: {}", request.getEmail());
            throw new BusinessException("Invalid password");
        }

        log.info("User authenticated successfully: id={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        log.debug("Refreshing token");
        RefreshToken newToken = refreshTokenService.rotate(request.getRefreshToken());
        User user = newToken.getUser();
        log.info("Token refreshed for user: id={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user, newToken.getToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        RefreshToken refreshToken = refreshTokenService.create(user);
        return buildAuthResponse(user, refreshToken.getToken());
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(jwtService.generateAccessToken(user.getEmail()))
                .refreshToken(refreshToken)
                .build();
    }
}
