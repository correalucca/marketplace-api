package com.marketplace.api.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.service.strategy.EconomicShipping;
import com.marketplace.api.service.strategy.ExpressShipping;
import com.marketplace.api.service.strategy.SedexShipping;
import com.marketplace.api.service.strategy.ShippingStrategy;

class ShippingStrategyTest {

    @Test
    @DisplayName("ExpressShipping deve calcular 10% do total")
    void expressShippingShouldCalculateTenPercent() {
        Order order = new Order();
        order.setTotalAmount(BigDecimal.valueOf(1000.00));
        order.setStatus(OrderStatus.PENDING);

        ShippingStrategy strategy = new ExpressShipping();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("EconomicShipping deve retornar 15.00")
    void economicShippingShouldReturnFifteen() {
        Order order = new Order();
        order.setTotalAmount(BigDecimal.valueOf(1000.00));

        ShippingStrategy strategy = new EconomicShipping();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("SedexShipping deve retornar 25.00")
    void sedexShippingShouldReturnTwentyFive() {
        Order order = new Order();
        order.setTotalAmount(BigDecimal.valueOf(1000.00));

        ShippingStrategy strategy = new SedexShipping();
        BigDecimal result = strategy.calculate(order);

        assertThat(result).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("Liskov Substitution: qualquer ShippingStrategy pode ser usada")
    void anyShippingStrategyCanBeUsed() {
        Order order = new Order();
        order.setTotalAmount(BigDecimal.valueOf(500.00));

        assertThat(calculateShipping(new ExpressShipping(), order)).isEqualByComparingTo("50.00");
        assertThat(calculateShipping(new EconomicShipping(), order)).isEqualByComparingTo("15.00");
        assertThat(calculateShipping(new SedexShipping(), order)).isEqualByComparingTo("25.00");
    }

    private BigDecimal calculateShipping(ShippingStrategy strategy, Order order) {
        return strategy.calculate(order);
    }
}
