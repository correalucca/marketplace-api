package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Slf4j
@Component
public class PacShipping implements ShippingStrategy {
    @Override
    public BigDecimal calculate(Order order) {
        log.debug("PAC shipping calculated for orderId={}: 20.00", order.getId());
        return BigDecimal.valueOf(20.00);
    }

    @Override
    public String getType() {
        return "PAC";
    }
}
