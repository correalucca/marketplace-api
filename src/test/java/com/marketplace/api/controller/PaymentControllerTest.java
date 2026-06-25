package com.marketplace.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.marketplace.api.dto.request.PaymentRequest;
import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.PaymentStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.service.PaymentService;
import com.marketplace.api.service.security.SecurityService;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private SecurityService securityService;

    private final User buyer = User.builder().id(1L).name("Buyer").email("buyer@test.com").role(Role.BUYER).build();

    @Test
    @DisplayName("POST /api/payments - Deve retornar 201")
    void shouldReturn201OnProcessPayment() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        PaymentResponse response = PaymentResponse.builder()
                .id(1L).orderId(1L).amount(BigDecimal.valueOf(5000.00))
                .status(PaymentStatus.APPROVED).paymentMethod("CREDIT_CARD")
                .transactionId(UUID.randomUUID().toString())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class), eq(buyer))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": 1,
                                    "amount": 5000.00,
                                    "paymentMethod": "CREDIT_CARD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/payments - Deve retornar 400 quando corpo vazio")
    void shouldReturn400OnProcessPaymentWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payments - Deve retornar 400 quando amount negativo")
    void shouldReturn400OnProcessPaymentWithNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": 1,
                                    "amount": -10.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} - Deve retornar 200")
    void shouldReturn200OnFindByOrderId() throws Exception {
        when(securityService.getAuthenticatedUser()).thenReturn(buyer);

        PaymentResponse response = PaymentResponse.builder()
                .id(1L).orderId(1L).amount(BigDecimal.valueOf(5000.00))
                .status(PaymentStatus.APPROVED).paymentMethod("CREDIT_CARD")
                .transactionId(UUID.randomUUID().toString())
                .build();

        when(paymentService.findByOrderId(1L, buyer)).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }
}
