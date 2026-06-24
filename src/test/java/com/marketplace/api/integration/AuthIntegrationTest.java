package com.marketplace.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.marketplace.api.entity.enums.Role;

/**
 * Testes de integração para autenticação (endpoints públicos /api/auth/*).
 * <p>
 * Cenários: register (sucesso, email duplicado), login (credenciais válidas, senha errada).
 * Não exige token JWT — esses endpoints são {@code .permitAll()} no SecurityConfig.
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/auth/register → 201 + token para SELLER")
    void shouldRegisterSeller() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Seller","email":"seller@test.com","password":"123456","role":"SELLER"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 201 + token para BUYER")
    void shouldRegisterBuyer() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Buyer","email":"buyer@test.com","password":"123456","role":"BUYER"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 422 para email duplicado")
    void shouldRejectDuplicateEmail() throws Exception {
        String json = """
            {"name":"User","email":"dup@test.com","password":"123456","role":"BUYER"}
            """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 + token")
    void shouldLogin() throws Exception {
        createUser("User", "user@test.com", Role.BUYER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/login → 422 para senha errada")
    void shouldRejectWrongPassword() throws Exception {
        createUser("User", "user@test.com", Role.BUYER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"wrong"}
                    """))
            .andExpect(status().isUnprocessableEntity());
    }
}
