package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Component
public class PacShipping implements ShippingStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        return BigDecimal.valueOf(20.00);
    }

    @Override
    public String getType() {
        return "PAC";
    }
}
