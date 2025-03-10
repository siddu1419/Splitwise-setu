package com.example.demo.service.split;

import com.example.demo.model.SplitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExpenseSplitStrategyFactory {
    private final Map<SplitType, ExpenseSplitStrategy> strategies;

    @Autowired
    public ExpenseSplitStrategyFactory(
            EqualSplitStrategy equalStrategy,
            UnequalSplitStrategy unequalStrategy,
            PercentageSplitStrategy percentageStrategy) {
        this.strategies = Map.of(
            SplitType.EQUAL, equalStrategy,
            SplitType.UNEQUAL, unequalStrategy,
            SplitType.PERCENTAGE, percentageStrategy
        );
    }

    public ExpenseSplitStrategy getStrategy(SplitType splitType) {
        ExpenseSplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split type: " + splitType);
        }
        return strategy;
    }
} 