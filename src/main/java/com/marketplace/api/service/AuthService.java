package com.marketplace.api.service;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
        if (userRepository.existsByEmail(request.getEmail())) {
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

        return buildAuthResponse(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid password");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken newToken = refreshTokenService.rotate(request.getRefreshToken());
        User user = newToken.getUser();
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
