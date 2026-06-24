package com.marketplace.api.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;

class OwnershipValidatorTest {

    @Test
    @DisplayName("Deve passar quando o próprio dono acessa")
    void shouldPassWhenOwnerAccesses() {
        User owner = User.builder().id(1L).role(Role.BUYER).build();
        assertDoesNotThrow(() ->
            OwnershipValidator.validateOwnership(1L, owner, "message"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando outro usuário acessa")
    void shouldThrowWhenOtherUserAccesses() {
        User other = User.builder().id(2L).role(Role.BUYER).build();
        assertThrows(BusinessException.class, () ->
            OwnershipValidator.validateOwnership(1L, other, "Access denied"));
    }

    @Test
    @DisplayName("Deve passar quando ADMIN acessa recurso de outro")
    void shouldPassWhenAdminAccesses() {
        User admin = User.builder().id(2L).role(Role.ADMIN).build();
        assertDoesNotThrow(() ->
            OwnershipValidator.validateOwnership(1L, admin, "message"));
    }
}
