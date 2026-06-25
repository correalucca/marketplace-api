package com.marketplace.api.service.strategy;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.entity.Order;

@Slf4j
@Component
public class StandardCommission implements CommissionStrategy {

    @Override
    public BigDecimal calculate(Order order) {
        BigDecimal commission = order.getTotalAmount().multiply(BigDecimal.valueOf(0.05));
        log.debug("Standard commission calculated for orderId={}: {}", order.getId(), commission);
        return commission;
    }

    @Override
    public String getType() {
        return "STANDARD";
    }
}
