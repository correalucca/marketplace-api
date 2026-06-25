package com.marketplace.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;

class ProductMapperTest {
    private final ProductMapper mapper = new ProductMapper();

    private final User seller = User.builder()
            .id(1L).name("Seller").email("seller@test.com").build();

    private final Product product = Product.builder()
            .id(1L)
            .name("Notebook")
            .description("Dell XPS")
            .price(BigDecimal.valueOf(5000.00))
            .stockQuantity(10)
            .seller(seller)
            .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 6, 2, 12, 0))
            .build();

    @Test
    @DisplayName("toResponse: deve mapear todos os campos")
    void toResponseShouldMapAllFields() {
        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Notebook");
        assertThat(response.getDescription()).isEqualTo("Dell XPS");
        assertThat(response.getPrice()).isEqualByComparingTo("5000.00");
        assertThat(response.getStockQuantity()).isEqualTo(10);
        assertThat(response.getSellerId()).isEqualTo(1L);
        assertThat(response.getSellerName()).isEqualTo("Seller");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toEntity: deve mapear request para entidade")
    void toEntityShouldMapRequest() {
        ProductRequest request = ProductRequest.builder()
                .name("Notebook")
                .description("Dell XPS")
                .price(BigDecimal.valueOf(5000.00))
                .stockQuantity(10)
                .build();

        Product entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Notebook");
        assertThat(entity.getDescription()).isEqualTo("Dell XPS");
        assertThat(entity.getPrice()).isEqualByComparingTo("5000.00");
        assertThat(entity.getStockQuantity()).isEqualTo(10);
        assertThat(entity.getSeller()).isNull();
    }

    @Test
    @DisplayName("toResponse: deve mapear produto com descrição null")
    void toResponseShouldHandleNullDescription() {
        Product p = Product.builder()
                .id(1L).name("Mouse").price(BigDecimal.TEN).stockQuantity(5)
                .seller(seller).build();

        ProductResponse response = mapper.toResponse(p);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDescription()).isNull();
    }

    @Test
    @DisplayName("toResponse: deve mapear produto com preço zero")
    void toResponseShouldHandleZeroPrice() {
        Product p = Product.builder()
                .id(1L).name("Free Item").description("Test")
                .price(BigDecimal.ZERO).stockQuantity(1)
                .seller(seller).build();

        ProductResponse response = mapper.toResponse(p);

        assertThat(response.getPrice()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("toResponse: deve mapear produto com estoque zero")
    void toResponseShouldHandleZeroStock() {
        Product p = Product.builder()
                .id(1L).name("Out of Stock").price(BigDecimal.TEN).stockQuantity(0)
                .seller(seller).build();

        ProductResponse response = mapper.toResponse(p);

        assertThat(response.getStockQuantity()).isEqualTo(0);
    }
}
