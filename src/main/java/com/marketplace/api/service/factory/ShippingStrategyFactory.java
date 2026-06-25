package com.marketplace.api.service.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.marketplace.api.service.strategy.ShippingStrategy;

@Slf4j
@Component
public class ShippingStrategyFactory {

    private final Map<String, ShippingStrategy> strategies;

    @Autowired
    public ShippingStrategyFactory(List<ShippingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        s -> s.getType().toUpperCase(),
                        Function.identity()
                ));
        log.debug("Registered shipping strategies: {}", strategies.keySet());
    }

    public ShippingStrategy getStrategy(String type) {
        ShippingStrategy strategy = strategies.get(type.toUpperCase());
        if (strategy == null) {
            log.warn("Invalid shipping type requested: {}", type);
            throw new IllegalArgumentException("Invalid shipping type: " + type);
        }
        log.debug("Shipping strategy resolved: {} -> {}", type, strategy.getClass().getSimpleName());
        return strategy;
    }
}
