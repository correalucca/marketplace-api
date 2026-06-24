package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import com.marketplace.api.entity.Order;

public interface ShippingStrategy {
    BigDecimal calculate(Order order);
    String getType();
}
