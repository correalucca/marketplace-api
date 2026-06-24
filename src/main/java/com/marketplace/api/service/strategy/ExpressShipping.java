package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Component
public class ExpressShipping implements ShippingStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        return order.getTotalAmount().multiply(BigDecimal.valueOf(0.10));
    }

    @Override
    public String getType() {
        return "EXPRESS";
    }
}
