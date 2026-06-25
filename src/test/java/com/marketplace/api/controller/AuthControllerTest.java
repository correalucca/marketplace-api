package com.marketplace.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.marketplace.api.dto.request.AuthRequest;
import com.marketplace.api.dto.request.RefreshTokenRequest;
import com.marketplace.api.dto.request.RegisterRequest;
import com.marketplace.api.dto.response.AuthResponse;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.service.auth.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 201")
    void shouldReturn201OnRegister() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L).name("User").email("user@test.com")
                .role(Role.BUYER).token("jwt-token").refreshToken("rt")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "User",
                                    "email": "user@test.com",
                                    "password": "123456",
                                    "role": "BUYER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 200")
    void shouldReturn200OnLogin() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L).name("User").email("user@test.com")
                .role(Role.BUYER).token("jwt-token").refreshToken("rt")
                .build();

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@test.com",
                                    "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Deve retornar 200")
    void shouldReturn200OnRefresh() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L).name("User").email("user@test.com")
                .role(Role.BUYER).token("new-jwt").refreshToken("new-rt")
                .build();

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 quando corpo vazio")
    void shouldReturn400OnRegisterWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 quando email inválido")
    void shouldReturn400OnRegisterWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "User",
                                    "email": "not-an-email",
                                    "password": "123456",
                                    "role": "BUYER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 quando email vazio")
    void shouldReturn400OnLoginWithEmptyEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "password": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
