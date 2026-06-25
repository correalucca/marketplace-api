package com.marketplace.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/payments como BUYER dono do pedido → 201 + APPROVED")
    void buyerShouldPayOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(orderResponse);

        ResponseEntity<String> response = post("/api/payments",
            PAYMENT_JSON.formatted(orderId, "10030.00"), authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("APPROVED");
        assertThat(body.get("orderId").asLong()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("POST /api/payments como outro BUYER → 422")
    void buyerShouldNotPayOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer1));
        Long orderId = extractId(orderResponse);

        ResponseEntity<String> response = post("/api/payments",
            PAYMENT_JSON.formatted(orderId, "10030.00"), authHeaders(buyer2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST /api/payments sem token → 403")
    void shouldRejectUnauthenticatedPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(orderResponse);

        ResponseEntity<String> response = post("/api/payments",
            PAYMENT_JSON.formatted(orderId, "10030.00"), jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/payments para pedido cancelado → 402")
    void shouldRejectPaymentForCancelledOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(orderResponse);

        post("/api/orders/" + orderId + "/cancel", "", authHeaders(buyer));

        ResponseEntity<String> response = post("/api/payments",
            PAYMENT_JSON.formatted(orderId, "10030.00"), authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(PAYMENT_REQUIRED);
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} como próprio BUYER → 200")
    void buyerShouldViewOwnPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer));
        Long orderId = extractId(orderResponse);

        post("/api/payments", PAYMENT_JSON.formatted(orderId, "10030.00"), authHeaders(buyer));

        ResponseEntity<String> response = get("/api/payments/order/" + orderId, authHeaders(buyer));

        assertThat(response.getStatusCode()).isEqualTo(OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("orderId").asLong()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} como outro BUYER → 422")
    void buyerShouldNotViewOtherBuyersPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        ResponseEntity<String> orderResponse = post("/api/orders",
            ORDER_JSON.formatted(product.getId(), 2), authHeaders(buyer1));
        Long orderId = extractId(orderResponse);

        post("/api/payments", PAYMENT_JSON.formatted(orderId, "10030.00"), authHeaders(buyer1));

        ResponseEntity<String> response = get("/api/payments/order/" + orderId, authHeaders(buyer2));

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
    }
}
