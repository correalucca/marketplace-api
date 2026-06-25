package com.marketplace.api.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.marketplace.api.service.security.JwtService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("Deve passar adiante quando não há token")
    void shouldPassThroughWhenNoToken() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @DisplayName("Deve passar adiante quando token não começa com Bearer")
    void shouldPassThroughWhenNotBearerToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic token");
        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @DisplayName("Deve passar adiante quando Bearer token vazio")
    void shouldPassThroughWhenBearerTokenIsEmpty() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");
        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @DisplayName("Deve autenticar quando token é válido")
    void shouldAuthenticateWhenTokenIsValid() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-jwt");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user@test.com").password("pass").roles("BUYER").build();

        when(jwtService.extractEmail("valid-jwt")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-jwt", "user@test.com")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtService).extractEmail("valid-jwt");
        verify(jwtService).isTokenValid("valid-jwt", "user@test.com");
    }

    @Test
    @DisplayName("Não deve autenticar quando token é inválido")
    void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-jwt");

        when(jwtService.extractEmail("invalid-jwt")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(org.springframework.security.core.userdetails.User.builder()
                        .username("user@test.com").password("pass").roles("BUYER").build());
        when(jwtService.isTokenValid("invalid-jwt", "user@test.com")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @DisplayName("Deve limpar contexto quando extractEmail lança exceção")
    void shouldClearContextWhenExtractEmailThrows() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer malformed-jwt");

        when(jwtService.extractEmail("malformed-jwt")).thenThrow(new RuntimeException("JWT parse error"));

        filter.doFilterInternal(request, response, filterChain);
    }
}
