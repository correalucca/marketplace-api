package com.marketplace.api.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.enums.OrderStatus;

class OrderTest {

    @Test
    @DisplayName("Deve criar pedido e gerenciar itens")
    void shouldCreateOrderAndManageItems() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(100.00));

        OrderItem item = OrderItem.builder()
                .id(1L).quantity(2)
                .unitPrice(BigDecimal.valueOf(50.00))
                .subtotal(BigDecimal.valueOf(100.00))
                .build();

        order.getItems().add(item);

        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
    }
}
