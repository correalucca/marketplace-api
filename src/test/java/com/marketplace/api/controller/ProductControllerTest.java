package com.marketplace.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.service.ProductService;
import com.marketplace.api.service.security.SecurityService;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private SecurityService securityService;

    private final User seller = User.builder().id(1L).name("Seller").email("seller@test.com").role(Role.SELLER).build();

    @Test
    @DisplayName("POST /api/products - Deve retornar 201")
    void shouldReturn201OnCreate() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(seller);

        ProductResponse response = ProductResponse.builder()
                .id(1L).name("Notebook").description("Dell XPS")
                .price(BigDecimal.valueOf(5000.00)).stockQuantity(10)
                .sellerId(1L).sellerName("Seller")
                .build();

        when(productService.create(any(ProductRequest.class), eq(seller))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Notebook",
                                    "description": "Dell XPS",
                                    "price": 5000.00,
                                    "stockQuantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Notebook"));
    }

    @Test
    @DisplayName("POST /api/products - Deve retornar 400 quando corpo vazio")
    void shouldReturn400OnCreateWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - Deve retornar 400 quando nome vazio")
    void shouldReturn400OnCreateWithBlankName() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "price": 10.00,
                                    "stockQuantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - Deve retornar 400 quando preço negativo")
    void shouldReturn400OnCreateWithNegativePrice() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Product",
                                    "price": -1,
                                    "stockQuantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products - Deve retornar lista")
    void shouldReturn200OnFindAll() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L).name("Notebook").price(BigDecimal.valueOf(5000.00)).stockQuantity(10)
                .sellerId(1L).sellerName("Seller")
                .build();

        when(productService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/products?name= - Deve filtrar por nome")
    void shouldReturn200OnFindByName() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L).name("Notebook").price(BigDecimal.valueOf(5000.00))
                .sellerId(1L).sellerName("Seller")
                .build();

        when(productService.findByName("note")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products?name=note"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Notebook"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Deve retornar produto")
    void shouldReturn200OnFindById() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L).name("Notebook").price(BigDecimal.valueOf(5000.00))
                .sellerId(1L).sellerName("Seller")
                .build();

        when(productService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Deve retornar 200 ao atualizar")
    void shouldReturn200OnUpdate() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L).name("Notebook Updated").price(BigDecimal.valueOf(5500.00))
                .sellerId(1L).sellerName("Seller")
                .build();

        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Notebook Updated",
                                    "price": 5500.00,
                                    "stockQuantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Notebook Updated"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - Deve retornar 204")
    void shouldReturn204OnDelete() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
