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
 * Testes de integração para pedidos (endpoints /api/orders/*).
 * <p>
 * Cenários: criação (role BUYER vs SELLER), listagem (apenas próprios),
 * visualização por ID (ownership), cancelamento (ownership), requisições sem token.
 */
class OrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/orders como BUYER → 201")
    void buyerShouldCreateOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.buyerId").isNumber())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/orders como SELLER → 422")
    void sellerShouldNotCreateOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User other = createUser("Other", "other@test.com", Role.SELLER);
        Product product = createProduct(other, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 1)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/orders sem token → 403")
    void shouldRejectUnauthenticatedCreate() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 1)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/orders → 200 (apenas próprios)")
    void buyerShouldListOwnOrders() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(get("/api/orders")
                .header("Authorization", tokenFor(buyer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    @DisplayName("GET /api/orders/{id} como próprio BUYER → 200")
    void buyerShouldViewOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", tokenFor(buyer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    @DisplayName("GET /api/orders/{id} como outro BUYER → 422")
    void buyerShouldNotViewOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", tokenFor(buyer2)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel como próprio BUYER → 204")
    void buyerShouldCancelOwnOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header("Authorization", tokenFor(buyer)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel como outro BUYER → 422")
    void buyerShouldNotCancelOtherBuyersOrder() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer1 = createUser("Buyer1", "b1@test.com", Role.BUYER);
        User buyer2 = createUser("Buyer2", "b2@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header("Authorization", tokenFor(buyer2)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel sem token → 403")
    void shouldRejectUnauthenticatedCancel() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ORDER_JSON, product.getId(), 2)))
            .andExpect(status().isCreated()).andReturn();
        Long orderId = extractId(result);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
            .andExpect(status().isForbidden());
    }
}
