package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Component
public class SedexShipping implements ShippingStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        return BigDecimal.valueOf(25.00);
    }

    @Override
    public String getType() {
        return "SEDEX";
    }
}
