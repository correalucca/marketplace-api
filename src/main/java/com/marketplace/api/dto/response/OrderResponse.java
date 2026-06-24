package com.marketplace.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.ShippingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long buyerId;
    private String buyerName;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private ShippingType shippingType;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
