package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import com.marketplace.api.entity.Order;

public interface CommissionStrategy {
    BigDecimal calculate(Order order);
    String getType();
}
