package com.marketplace.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.enums.Role;

class UserTest {

    @Test
    @DisplayName("Deve criar usuário com builder")
    void shouldCreateUserViaBuilder() {
        User user = User.builder()
                .id(1L).name("John").email("john@test.com")
                .password("secret").phone("11999999999")
                .role(Role.SELLER).build();

        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getEmail()).isEqualTo("john@test.com");
        assertThat(user.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    @DisplayName("Deve considerar o id no equals")
    void equalsShouldConsiderId() {
        User user1 = User.builder().id(1L).name("John").email("john@test.com").role(Role.BUYER).build();
        User user2 = User.builder().id(1L).name("John").email("john@test.com").role(Role.BUYER).build();
        User user3 = User.builder().id(2L).name("Jane").email("jane@test.com").role(Role.BUYER).build();

        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
    }
}
