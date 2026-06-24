package com.marketplace.api.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.entity.RefreshToken;
import com.marketplace.api.entity.User;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken create(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotate(String currentToken) {
        RefreshToken old = refreshTokenRepository.findByToken(currentToken)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (old.getRevokedAt() != null) {
            throw new BusinessException("Refresh token already revoked");
        }

        if (old.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expired");
        }

        old.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(old);

        return create(old.getUser());
    }

    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()) && rt.getRevokedAt() == null)
                .forEach(rt -> {
                    rt.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(rt);
                });
    }

    @Scheduled(fixedRate = 86400000)
    @Transactional
    public void cleanupExpired() {
        refreshTokenRepository.deleteAllExpiredSince(LocalDateTime.now());
    }
}
