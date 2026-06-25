package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.marketplace.api.service.security.JwtService;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        String secret = "bWFya2V0cGxhY2UtYXBpLWp3dC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=";
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 900000L);
    }

    @Test
    @DisplayName("Deve gerar e validar token")
    void shouldGenerateAndValidateToken() {
        String email = "user@test.com";
        String token = jwtService.generateAccessToken(email);

        assertThat(token).isNotBlank();

        String extractedEmail = jwtService.extractEmail(token);
        assertThat(extractedEmail).isEqualTo(email);

        boolean valid = jwtService.isTokenValid(token, email);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar token com email diferente")
    void shouldRejectTokenWithDifferentEmail() {
        String token = jwtService.generateAccessToken("user@test.com");
        boolean valid = jwtService.isTokenValid(token, "other@test.com");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar token inválido")
    void shouldRejectInvalidToken() {
        boolean valid = jwtService.isTokenValid("invalid-token", "user@test.com");
        assertThat(valid).isFalse();
    }
}
