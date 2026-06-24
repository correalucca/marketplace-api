package com.marketplace.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getAuthenticatedUser: deve retornar usuário quando autenticado")
    void shouldReturnAuthenticatedUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().id(1L).email("test@test.com").role(Role.BUYER).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = securityService.getAuthenticatedUser();
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getAuthenticatedUser: deve lançar exceção quando não autenticado")
    void shouldThrowWhenNotAuthenticated() {
        assertThrows(BusinessException.class, () -> securityService.getAuthenticatedUser());
    }

    @Test
    @DisplayName("getAuthenticatedUser: deve lançar exceção quando autenticação inválida")
    void shouldThrowWhenAuthenticationInvalid() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(BusinessException.class, () -> securityService.getAuthenticatedUser());
    }

    @Test
    @DisplayName("requireRole: deve passar quando role é compatível")
    void shouldPassWhenRoleMatches() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("seller@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().id(1L).email("seller@test.com").role(Role.SELLER).build();
        when(userRepository.findByEmail("seller@test.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> securityService.requireRole(Role.SELLER));
    }

    @Test
    @DisplayName("requireRole: deve lançar exceção quando role é incompatível")
    void shouldThrowWhenRoleDoesNotMatch() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("buyer@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().id(1L).email("buyer@test.com").role(Role.BUYER).build();
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> securityService.requireRole(Role.SELLER));
    }

    @Test
    @DisplayName("requireRole: deve passar quando ADMIN mesmo com role diferente")
    void shouldPassWhenAdminWithDifferentRole() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().id(1L).email("admin@test.com").role(Role.ADMIN).build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> securityService.requireRole(Role.SELLER));
    }
}
