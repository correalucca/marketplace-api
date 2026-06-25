package com.marketplace.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.service.factory.CommissionStrategyFactory;
import com.marketplace.api.service.strategy.CommissionStrategy;
import com.marketplace.api.service.strategy.PremiumCommission;
import com.marketplace.api.service.strategy.StandardCommission;

class CommissionStrategyFactoryTest {

    private CommissionStrategyFactory factory;

    @BeforeEach
    void setUp() {
        List<CommissionStrategy> strategies = List.of(
                new StandardCommission(),
                new PremiumCommission()
        );
        factory = new CommissionStrategyFactory(strategies);
    }

    @Test
    @DisplayName("Deve retornar strategy para tipo válido")
    void shouldReturnStrategyForValidType() {
        assertThat(factory.getStrategy("STANDARD")).isInstanceOf(StandardCommission.class);
        assertThat(factory.getStrategy("standard")).isInstanceOf(StandardCommission.class);
        assertThat(factory.getStrategy("PREMIUM")).isInstanceOf(PremiumCommission.class);
    }

    @Test
    @DisplayName("Deve lançar exceção para tipo inválido")
    void shouldThrowForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> factory.getStrategy("INVALID"));
    }
}
