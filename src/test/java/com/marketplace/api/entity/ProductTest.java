package com.marketplace.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.enums.Role;

class ProductTest {

    @Test
    @DisplayName("Deve criar produto usando builder")
    void shouldCreateProductViaBuilder() {
        User seller = User.builder().id(1L).name("Seller").email("seller@test.com").role(Role.SELLER).build();

        Product product = Product.builder()
                .id(1L)
                .name("Notebook")
                .description("Dell XPS")
                .price(BigDecimal.valueOf(5000.00))
                .stockQuantity(10)
                .seller(seller)
                .build();

        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("Notebook");
        assertThat(product.getPrice()).isEqualByComparingTo("5000.00");
        assertThat(product.getSeller().getEmail()).isEqualTo("seller@test.com");
    }

    @Test
    @DisplayName("Deve criar produto usando no-arg constructor e setters")
    void shouldCreateProductViaSetters() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Mouse");
        product.setPrice(BigDecimal.valueOf(150.00));
        product.setStockQuantity(50);

        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(product.getStockQuantity()).isEqualTo(50);
    }
}
