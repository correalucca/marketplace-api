package com.marketplace.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.Payment;
import com.marketplace.api.entity.enums.PaymentStatus;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    @DisplayName("toResponse: deve mapear todos os campos")
    void toResponseShouldMapAllFields() {
        Order order = new Order();
        order.setId(1L);

        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .amount(BigDecimal.valueOf(5000.00))
                .status(PaymentStatus.APPROVED)
                .paymentMethod("CREDIT_CARD")
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(response.getTransactionId()).isEqualTo(payment.getTransactionId());
        assertThat(response.getCreatedAt()).isNotNull();
    }
}
