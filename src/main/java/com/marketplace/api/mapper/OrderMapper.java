package com.marketplace.api.mapper;

import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.dto.response.OrderItemResponse;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.Order;

@Slf4j
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        log.debug("Mapping Order to response: id={}", order.getId());
        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getName())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .totalAmount(order.getTotalAmount())
                .shippingAmount(order.getShippingAmount())
                .shippingType(order.getShippingType())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
