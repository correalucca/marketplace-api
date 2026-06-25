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

class OrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/orders como BUYER → 201")
    void buyerShouldCreateOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").isNumber()).isTrue();
        assertThat(body.get("buyerId").isNumber()).isTrue();
        assertThat(body.get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("POST /api/orders como SELLER → 422")
    void sellerShouldNotCreateOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User other = createUser("Other", "other@test.com", Role.SELLER);
        Product product = createProduct(other, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 1), authHeaders(seller));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/orders sem token → 403")
    void shouldRejectUnauthenticatedCreate() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> response = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 1), jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/orders → 200 (apenas próprios)")
    void buyerShouldListOwnOrders() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> listResponse = get("/api/orders", authHeaders(buyer));

        assertThat(listResponse.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(listResponse.getBody());
        assertThat(body.get(0).get("id").asLong()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("GET /api/orders/{id} como próprio BUYER → 200")
    void buyerShouldViewOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> response = get("/api/orders/" + orderId, authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("id").asLong()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("GET /api/orders/{id} como outro BUYER → 422")
    void buyerShouldNotViewOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer1));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> response = get("/api/orders/" + orderId, authHeaders(buyer2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel como próprio BUYER → 204")
    void buyerShouldCancelOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> response = post("/api/orders/" + orderId + "/cancel", "", authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel como outro BUYER → 422")
    void buyerShouldNotCancelOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer1));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> response = post("/api/orders/" + orderId + "/cancel", "", authHeaders(buyer2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel sem token → 403")
    void shouldRejectUnauthenticatedCancel() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> createResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(createResponse);

        ResponseEntity<String> response = post("/api/orders/" + orderId + "/cancel", "", jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }
}
