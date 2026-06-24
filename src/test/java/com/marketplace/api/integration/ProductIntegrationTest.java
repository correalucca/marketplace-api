package com.marketplace.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;

/**
 * Testes de integração para produtos (endpoints /api/products/*).
 * <p>
 * Cenários: CRUD público/autenticado, role SELLER vs BUYER,
 * ownership (próprio produto vs produto de outro vendedor), requisições sem token.
 */
class ProductIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/products → 200 (público)")
    void shouldListProductsPublicly() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/products/{id} → 200 (público)")
    void shouldGetProductByIdPublicly() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(get("/api/products/" + product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(product.getId()))
            .andExpect(jsonPath("$.name").value("Notebook"));
    }

    @Test
    @DisplayName("POST /api/products como SELLER → 201")
    void sellerShouldCreateProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);

        mockMvc.perform(post("/api/products")
                .header("Authorization", tokenFor(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PRODUCT_JSON, "Notebook", "Gaming", "5000.00", 10)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Notebook"))
            .andExpect(jsonPath("$.sellerId").isNumber());
    }

    @Test
    @DisplayName("POST /api/products como BUYER → 422")
    void buyerShouldNotCreateProduct() throws Exception {
        User buyer = createUser("Buyer", "buyer@test.com", Role.BUYER);

        mockMvc.perform(post("/api/products")
                .header("Authorization", tokenFor(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PRODUCT_JSON, "Phone", "Mobile", "2000.00", 5)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/products sem token → 403")
    void shouldRejectUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PRODUCT_JSON, "Phone", "Mobile", "2000.00", 5)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/products/{id} como próprio SELLER → 200")
    void sellerShouldUpdateOwnProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(put("/api/products/" + product.getId())
                .header("Authorization", tokenFor(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PRODUCT_JSON, "Updated", "New", "5500.00", 8)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} como outro SELLER → 422")
    void sellerShouldNotUpdateOtherSellersProduct() throws Exception {
        User seller1 = createUser("Seller1", "s1@test.com", Role.SELLER);
        User seller2 = createUser("Seller2", "s2@test.com", Role.SELLER);
        Product product = createProduct(seller1, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(put("/api/products/" + product.getId())
                .header("Authorization", tokenFor(seller2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(PRODUCT_JSON, "Hacked", "", "1.00", 1)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} como próprio SELLER → 204")
    void sellerShouldDeleteOwnProduct() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Temp", BigDecimal.valueOf(100), 5);

        mockMvc.perform(delete("/api/products/" + product.getId())
                .header("Authorization", tokenFor(seller)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} como outro SELLER → 422")
    void sellerShouldNotDeleteOtherSellersProduct() throws Exception {
        User seller1 = createUser("Seller1", "s1@test.com", Role.SELLER);
        User seller2 = createUser("Seller2", "s2@test.com", Role.SELLER);
        Product product = createProduct(seller1, "Notebook", BigDecimal.valueOf(5000), 10);

        mockMvc.perform(delete("/api/products/" + product.getId())
                .header("Authorization", tokenFor(seller2)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} sem token → 403")
    void shouldRejectUnauthenticatedDelete() throws Exception {
        User seller = createUser("Seller", "seller@test.com", Role.SELLER);
        Product product = createProduct(seller, "Temp", BigDecimal.valueOf(100), 5);

        mockMvc.perform(delete("/api/products/" + product.getId()))
            .andExpect(status().isForbidden());
    }
}
