package com.marketplace.api.service.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.marketplace.api.service.strategy.CommissionStrategy;

@Slf4j
@Component
public class CommissionStrategyFactory {

    private final Map<String, CommissionStrategy> strategies;

    @Autowired
    public CommissionStrategyFactory(List<CommissionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        s -> s.getType().toUpperCase(),
                        Function.identity()
                ));
        log.debug("Registered commission strategies: {}", strategies.keySet());
    }

    public CommissionStrategy getStrategy(String type) {
        CommissionStrategy strategy = strategies.get(type.toUpperCase());
        if (strategy == null) {
            log.warn("Invalid commission type requested: {}", type);
            throw new IllegalArgumentException("Invalid commission type: " + type);
        }
        log.debug("Commission strategy resolved: {} -> {}", type, strategy.getClass().getSimpleName());
        return strategy;
    }
}
