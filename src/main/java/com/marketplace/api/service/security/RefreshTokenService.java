package com.marketplace.api.service.security;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.entity.RefreshToken;
import com.marketplace.api.entity.User;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.RefreshTokenRepository;

@Slf4j
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

        RefreshToken saved = refreshTokenRepository.save(token);
        log.debug("Refresh token created for userId={}", user.getId());
        return saved;
    }

    @Transactional
    public RefreshToken rotate(String currentToken) {
        log.debug("Rotating refresh token");
        RefreshToken old = refreshTokenRepository.findByToken(currentToken)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token provided");
                    return new BusinessException("Invalid refresh token");
                });

        if (old.getRevokedAt() != null) {
            log.warn("Refresh token already revoked for userId={}", old.getUser().getId());
            throw new BusinessException("Refresh token already revoked");
        }

        if (old.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired for userId={}", old.getUser().getId());
            throw new BusinessException("Refresh token expired");
        }

        old.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(old);

        RefreshToken newToken = create(old.getUser());
        log.info("Refresh token rotated for userId={}", old.getUser().getId());
        return newToken;
    }

    @Transactional
    public void revokeAllByUser(User user) {
        log.info("Revoking all refresh tokens for userId={}", user.getId());
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
        log.info("Cleaned up expired refresh tokens");
    }
}
