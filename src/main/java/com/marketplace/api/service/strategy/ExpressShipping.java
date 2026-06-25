package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Slf4j
@Component
public class ExpressShipping implements ShippingStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        BigDecimal amount = order.getTotalAmount().multiply(BigDecimal.valueOf(0.10));
        log.debug("Express shipping calculated for orderId={}: {}", order.getId(), amount);
        return amount;
    }

    @Override
    public String getType() {
        return "EXPRESS";
    }
}
