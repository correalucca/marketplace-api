package com.marketplace.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;

/**
 * Testes de integração para pagamentos (endpoints /api/payments/*).
 * <p>
 * Cenários: pagamento do próprio pedido (APPROVED), tentativa de pagar pedido alheio
 * (ownership), pagamento de pedido cancelado, visualização de pagamento (ownership),
 * requisições sem token.
 */
class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/payments como BUYER dono do pedido → 201 + APPROVED")
    void buyerShouldPayOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/payments")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    @DisplayName("POST /api/payments como outro BUYER → 422")
    void buyerShouldNotPayOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/payments")
                .header("Authorization", tokenFor(buyer2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/payments sem token → 403")
    void shouldRejectUnauthenticatedPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/payments para pedido cancelado → 402")
    void shouldRejectPaymentForCancelledOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header("Authorization", tokenFor(buyer)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/payments")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} como próprio BUYER → 200")
    void buyerShouldViewOwnPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/payments")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/order/" + orderId)
                .header("Authorization", tokenFor(buyer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} como outro BUYER → 422")
    void buyerShouldNotViewOtherBuyersPayment() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(orderResult);

        mockMvc.perform(post("/api/payments")
                .header("Authorization", tokenFor(buyer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PAYMENT_JSON, orderId, "10030.00")))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/order/" + orderId)
                .header("Authorization", tokenFor(buyer2)))
            .andExpect(status().isUnprocessableEntity());
    }
}
