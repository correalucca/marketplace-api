package com.marketplace.api.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.service.strategy.CommissionStrategy;
import com.marketplace.api.service.strategy.PremiumCommission;
import com.marketplace.api.service.strategy.StandardCommission;

class CommissionStrategyTest {

    @Test
    @DisplayName("StandardCommission: deve calcular 5% do total")
    void standardCommissionShouldCalculateFivePercent() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.valueOf(1000.00));
        order.setStatus(OrderStatus.PENDING);

        CommissionStrategy strategy = new StandardCommission();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("StandardCommission: deve retornar 0 quando total é zero")
    void standardCommissionShouldReturnZeroWhenTotalIsZero() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        CommissionStrategy strategy = new StandardCommission();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("PremiumCommission: deve calcular 3% do total")
    void premiumCommissionShouldCalculateThreePercent() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.valueOf(1000.00));
        order.setStatus(OrderStatus.PENDING);

        CommissionStrategy strategy = new PremiumCommission();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("PremiumCommission: deve retornar 0 quando total é zero")
    void premiumCommissionShouldReturnZeroWhenTotalIsZero() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        CommissionStrategy strategy = new PremiumCommission();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Princípio de Liskov: qualquer CommissionStrategy pode ser utilizada")
    void anyCommissionStrategyCanBeUsed() {
        Order order = new Order();
        order.setTotalAmount(BigDecimal.valueOf(500.00));

        assertThat(calculateCommission(new StandardCommission(), order)).isEqualByComparingTo("25.00");
        assertThat(calculateCommission(new PremiumCommission(), order)).isEqualByComparingTo("15.00");
    }

    private BigDecimal calculateCommission(CommissionStrategy strategy, Order order) {
        return strategy.calculate(order);
    }
}
