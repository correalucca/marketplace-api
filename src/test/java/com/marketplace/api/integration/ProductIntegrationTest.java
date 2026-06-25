package com.marketplace.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;

class ProductIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/products → 200 (público)")
    void shouldListProductsPublicly() throws Exception {
        ResponseEntity<String> response = get("/api/products", jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
    }

    @Test
    @DisplayName("GET /api/products/{id} → 200 (público)")
    void shouldGetProductByIdPublicly() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = get("/api/products/" + product.getId(), jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isEqualTo(product.getId());
        assertThat(body.get("name").asText()).isEqualTo("Notebook");
    }

    @Test
    @DisplayName("POST /api/products como SELLER → 201")
    void sellerShouldCreateProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);

        ResponseEntity<String> response = post("/api/products",
            PRODUCT_JSON.formatted("Notebook", "Gaming", "5000.00", 10),
            authHeaders(seller));

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("name").asText()).isEqualTo("Notebook");
        assertThat(body.get("sellerId").isNumber()).isTrue();
    }

    @Test
    @DisplayName("POST /api/products como BUYER → 422")
    void buyerShouldNotCreateProduct() throws Exception {
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);

        ResponseEntity<String> response = post("/api/products",
            PRODUCT_JSON.formatted("Phone", "Mobile", "2000.00", 5),
            authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/products sem token → 403")
    void shouldRejectUnauthenticatedCreate() throws Exception {
        ResponseEntity<String> response = post("/api/products",
            PRODUCT_JSON.formatted("Phone", "Mobile", "2000.00", 5),
            jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("PUT /api/products/{id} como próprio SELLER → 200")
    void sellerShouldUpdateOwnProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = put("/api/products/" + product.getId(),
            PRODUCT_JSON.formatted("Updated", "New", "5500.00", 8),
            authHeaders(seller));

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("name").asText()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("PUT /api/products/{id} como outro SELLER → 422")
    void sellerShouldNotUpdateOtherSellersProduct() throws Exception {
        User seller1 = createUser("Seller1", "s1@test.com", Role.SELLER);
        User seller2 = createUser("Seller2", "s2@test.com", Role.SELLER);
        Product product = createProduct(seller1, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = put("/api/products/" + product.getId(),
            PRODUCT_JSON.formatted("Hacked", "", "1.00", 1),
            authHeaders(seller2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} como próprio SELLER → 204")
    void sellerShouldDeleteOwnProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Temp", BigDecimal.valueOf(100), 5);

        ResponseEntity<String> response = delete("/api/products/" + product.getId(), authHeaders(seller));

        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} como outro SELLER → 422")
    void sellerShouldNotDeleteOtherSellersProduct() throws Exception {
        User seller1 = createUser("Seller1", "s1@test.com", Role.SELLER);
        User seller2 = createUser("Seller2", "s2@test.com", Role.SELLER);
        Product product = createProduct(seller1, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = delete("/api/products/" + product.getId(), authHeaders(seller2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} sem token → 403")
    void shouldRejectUnauthenticatedDelete() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Temp", BigDecimal.valueOf(100), 5);

        ResponseEntity<String> response = delete("/api/products/" + product.getId(), jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }
}
