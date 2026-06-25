package com.marketplace.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketplace.api.entity.enums.Role;

class AuthIntegrationTest extends AbstractIntegrationTest {
    @Test
    @DisplayName("POST /api/auth/register → 201 + token para SELLER")
    void shouldRegisterSeller() throws Exception {
        ResponseEntity<String> response = post("/api/auth/register", """
            {"name":"Seller","email":"seller@test.com","password":"123456","role":"SELLER"}
            """, jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("token").asText()).isNotEmpty();
        assertThat(body.get("role").asText()).isEqualTo("SELLER");
    }

    @Test
    @DisplayName("POST /api/auth/register → 201 + token para BUYER")
    void shouldRegisterBuyer() throws Exception {
        ResponseEntity<String> response = post("/api/auth/register", """
            {"name":"Buyer","email":"buyer@test.com","password":"123456","role":"BUYER"}
            """, jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("token").asText()).isNotEmpty();
        assertThat(body.get("role").asText()).isEqualTo("BUYER");
    }

    @Test
    @DisplayName("POST /api/auth/register → 422 para email duplicado")
    void shouldRejectDuplicateEmail() throws Exception {
        String json = """
            {"name":"User","email":"dup@test.com","password":"123456","role":"BUYER"}
            """;

        assertThat(post("/api/auth/register", json, jsonHeaders()).getStatusCode()).isEqualTo(CREATED);
        assertThat(post("/api/auth/register", json, jsonHeaders()).getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 + token")
    void shouldLogin() throws Exception {
        createUser("User", "user@test.com", Role.BUYER);

        ResponseEntity<String> response = post("/api/auth/login", """
            {"email":"user@test.com","password":"123456"}
            """, jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("token").asText()).isNotEmpty();
    }

    @Test
    @DisplayName("POST /api/auth/login → 422 para senha errada")
    void shouldRejectWrongPassword() throws Exception {
        createUser("User", "user@test.com", Role.BUYER);

        ResponseEntity<String> response = post("/api/auth/login", """
            {"email":"user@test.com","password":"wrong"}
            """, jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }
}
