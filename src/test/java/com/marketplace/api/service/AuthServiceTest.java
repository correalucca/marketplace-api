package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.marketplace.api.service.auth.AuthService;
import com.marketplace.api.service.security.JwtService;
import com.marketplace.api.service.security.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.marketplace.api.dto.request.AuthRequest;
import com.marketplace.api.dto.request.RefreshTokenRequest;
import com.marketplace.api.dto.request.RegisterRequest;
import com.marketplace.api.dto.response.AuthResponse;
import com.marketplace.api.entity.RefreshToken;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private final User user = User.builder()
            .id(1L).name("John").email("john@test.com")
            .password("encoded-pass").role(Role.BUYER)
            .build();

    @Test
    @DisplayName("register: deve registrar usuário com sucesso")
    void registerShouldSucceed() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John").email("john@test.com").password("123456")
                .phone("11999999999").role(Role.BUYER).build();

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateAccessToken("john@test.com")).thenReturn("jwt-token");

        RefreshToken rt = RefreshToken.builder().token("rt-token").build();
        when(refreshTokenService.create(any(User.class))).thenReturn(rt);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: deve lançar exceção quando email já existe")
    void registerShouldThrowWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@test.com").build();

        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("authenticate: deve autenticar com sucesso")
    void authenticateShouldSucceed() {
        AuthRequest request = AuthRequest.builder()
                .email("john@test.com").password("123456").build();

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded-pass")).thenReturn(true);
        when(jwtService.generateAccessToken("john@test.com")).thenReturn("jwt-token");

        RefreshToken rt = RefreshToken.builder().token("rt-token").build();
        when(refreshTokenService.create(user)).thenReturn(rt);

        AuthResponse response = authService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    @DisplayName("authenticate: deve lançar exceção quando usuário não existe")
    void authenticateShouldThrowWhenUserNotFound() {
        AuthRequest request = AuthRequest.builder()
                .email("unknown@test.com").password("123456").build();

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.authenticate(request));
    }

    @Test
    @DisplayName("authenticate: deve lançar exceção quando senha inválida")
    void authenticateShouldThrowWhenInvalidPassword() {
        AuthRequest request = AuthRequest.builder()
                .email("john@test.com").password("wrong").build();

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.authenticate(request));
    }

    @Test
    @DisplayName("refresh: deve renovar token")
    void refreshShouldSucceed() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-rt");

        User userEntity = User.builder().id(1L).name("John").email("john@test.com").role(Role.BUYER).build();
        RefreshToken newRt = RefreshToken.builder().token("new-rt").user(userEntity).build();

        when(refreshTokenService.rotate("old-rt")).thenReturn(newRt);
        when(jwtService.generateAccessToken("john@test.com")).thenReturn("new-jwt");

        AuthResponse response = authService.refresh(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-jwt");
        assertThat(response.getRefreshToken()).isEqualTo("new-rt");
    }
}
