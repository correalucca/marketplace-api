package com.marketplace.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.dto.response.OrderItemResponse;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.ShippingType;

class OrderMapperTest {
    private final OrderMapper mapper = new OrderMapper();

    @Test
    @DisplayName("toResponse: deve mapear todos os campos do pedido")
    void toResponseShouldMapAllFields() {
        User buyer = User.builder().id(1L).name("Buyer").build();
        Product product = Product.builder().id(1L).name("Notebook").price(BigDecimal.valueOf(3000.00)).build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(3000.00))
                .subtotal(BigDecimal.valueOf(6000.00))
                .build();

        Order order = new Order();
        order.setId(1L);
        order.setBuyer(buyer);
        order.setItems(List.of(item));
        order.setTotalAmount(BigDecimal.valueOf(6030.00));
        order.setShippingAmount(BigDecimal.valueOf(30.00));
        order.setShippingType(ShippingType.EXPRESS);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        order.setUpdatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBuyerId()).isEqualTo(1L);
        assertThat(response.getBuyerName()).isEqualTo("Buyer");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("6030.00");
        assertThat(response.getShippingAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getShippingType()).isEqualTo(ShippingType.EXPRESS);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);

        assertThat(response.getItems()).hasSize(1);
        OrderItemResponse itemResp = response.getItems().get(0);
        assertThat(itemResp.getId()).isEqualTo(1L);
        assertThat(itemResp.getProductId()).isEqualTo(1L);
        assertThat(itemResp.getProductName()).isEqualTo("Notebook");
        assertThat(itemResp.getQuantity()).isEqualTo(2);
        assertThat(itemResp.getUnitPrice()).isEqualByComparingTo("3000.00");
        assertThat(itemResp.getSubtotal()).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("toResponse: deve mapear pedido sem itens")
    void toResponseShouldMapOrderWithNoItems() {
        User buyer = User.builder().id(1L).name("Buyer").build();

        Order order = new Order();
        order.setId(1L);
        order.setBuyer(buyer);
        order.setItems(List.of());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getItems()).isEmpty();
    }
}
