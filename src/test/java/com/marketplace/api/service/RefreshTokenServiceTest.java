package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.marketplace.api.entity.RefreshToken;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.RefreshTokenRepository;
import com.marketplace.api.service.security.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final User user = User.builder().id(1L).email("user@test.com").role(Role.BUYER).build();

    @Test
    @DisplayName("create: deve criar refresh token")
    void createShouldSucceed() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.create(user);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate: deve rotacionar token com sucesso")
    void rotateShouldSucceed() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);

        RefreshToken oldToken = RefreshToken.builder()
                .id(1L).token("old-token").user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotate("old-token");

        assertThat(newToken).isNotNull();
        assertThat(newToken.getToken()).isNotEqualTo("old-token");
        assertThat(oldToken.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("rotate: deve lançar exceção quando token inválido")
    void rotateShouldThrowWhenInvalidToken() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> refreshTokenService.rotate("invalid"));
    }

    @Test
    @DisplayName("rotate: deve lançar exceção quando token já revogado")
    void rotateShouldThrowWhenAlreadyRevoked() {
        RefreshToken revoked = RefreshToken.builder()
                .token("revoked-token").user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revokedAt(LocalDateTime.now())
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThrows(BusinessException.class, () -> refreshTokenService.rotate("revoked-token"));
    }

    @Test
    @DisplayName("rotate: deve lançar exceção quando token expirado")
    void rotateShouldThrowWhenExpired() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token").user(user)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(BusinessException.class, () -> refreshTokenService.rotate("expired-token"));
    }
}
