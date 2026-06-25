package com.marketplace.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.marketplace.api.service.factory.ShippingStrategyFactory;
import com.marketplace.api.service.strategy.EconomicShipping;
import com.marketplace.api.service.strategy.ExpressShipping;
import com.marketplace.api.service.strategy.PacShipping;
import com.marketplace.api.service.strategy.SedexShipping;
import com.marketplace.api.service.strategy.ShippingStrategy;

class ShippingStrategyFactoryTest {

    private ShippingStrategyFactory factory;

    @BeforeEach
    void setUp() {
        List<ShippingStrategy> strategies = List.of(
                new ExpressShipping(),
                new EconomicShipping(),
                new SedexShipping(),
                new PacShipping()
        );
        factory = new ShippingStrategyFactory(strategies);
    }

    @Test
    @DisplayName("Deve retornar strategy para tipo válido")
    void shouldReturnStrategyForValidType() {
        assertThat(factory.getStrategy("EXPRESS")).isInstanceOf(ExpressShipping.class);
        assertThat(factory.getStrategy("economic")).isInstanceOf(EconomicShipping.class);
        assertThat(factory.getStrategy("Sedex")).isInstanceOf(SedexShipping.class);
        assertThat(factory.getStrategy("PAC")).isInstanceOf(PacShipping.class);
    }

    @Test
    @DisplayName("Deve lançar exceção para tipo inválido")
    void shouldThrowForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> factory.getStrategy("INVALID"));
    }
}
