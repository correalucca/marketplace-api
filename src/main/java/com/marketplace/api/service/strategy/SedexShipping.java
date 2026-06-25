package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Slf4j
@Component
public class SedexShipping implements ShippingStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        log.debug("SEDEX shipping calculated for orderId={}: 25.00", order.getId());
        return BigDecimal.valueOf(25.00);
    }

    @Override
    public String getType() {
        return "SEDEX";
    }
}
