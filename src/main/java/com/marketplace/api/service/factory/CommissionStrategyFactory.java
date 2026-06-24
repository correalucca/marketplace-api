package com.marketplace.api.service.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.marketplace.api.service.strategy.CommissionStrategy;

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
    }

    public CommissionStrategy getStrategy(String type) {
        CommissionStrategy strategy = strategies.get(type.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Invalid commission type: " + type);
        }
        return strategy;
    }
}
