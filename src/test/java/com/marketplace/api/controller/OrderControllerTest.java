package com.marketplace.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.marketplace.api.dto.response.OrderItemResponse;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.entity.enums.ShippingType;
import com.marketplace.api.service.OrderService;
import com.marketplace.api.service.security.SecurityService;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private SecurityService securityService;

    private final User buyer = User.builder().id(1L).name("Buyer").email("buyer@test.com").role(Role.BUYER).build();

    @Test
    @DisplayName("POST /api/orders - Deve retornar 201 ao criar pedido")
    void shouldReturn201OnCreateOrder() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .buyerId(1L)
                .buyerName("Buyer")
                .items(Collections.singletonList(
                        OrderItemResponse.builder()
                                .id(1L).productId(1L).productName("Notebook")
                                .quantity(2).unitPrice(BigDecimal.valueOf(3000.00))
                                .subtotal(BigDecimal.valueOf(6000.00))
                                .build()
                ))
                .totalAmount(BigDecimal.valueOf(6030.00))
                .shippingAmount(BigDecimal.valueOf(30.00))
                .shippingType(ShippingType.EXPRESS)
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.create(any(), eq(buyer))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "items": [{"productId": 1, "quantity": 2}],
                                    "shippingType": "EXPRESS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.buyerName").value("Buyer"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/orders - Deve retornar 400 quando corpo vazio")
    void shouldReturn400OnCreateWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - Deve retornar 400 quando items vazio")
    void shouldReturn400OnCreateWithEmptyItems() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "items": [],
                                    "shippingType": "EXPRESS"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - Deve retornar 400 quando shippingType ausente")
    void shouldReturn400OnCreateWithoutShippingType() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "items": [{"productId": 1, "quantity": 2}]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/orders - Deve retornar lista de pedidos do comprador")
    void shouldReturn200OnListMyOrders() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        OrderResponse response = OrderResponse.builder()
                .id(1L).buyerId(1L).buyerName("Buyer")
                .totalAmount(BigDecimal.valueOf(100.00))
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.findByBuyer(buyer)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Deve retornar pedido por ID")
    void shouldReturn200OnFindById() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        OrderResponse response = OrderResponse.builder()
                .id(1L).buyerId(1L).buyerName("Buyer")
                .totalAmount(BigDecimal.valueOf(100.00))
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.findById(1L, buyer)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel - Deve retornar 204 ao cancelar")
    void shouldReturn204OnCancelOrder() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isNoContent());
    }
}
