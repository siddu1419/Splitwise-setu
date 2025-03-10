package com.example.demo.service.split;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PercentageSplitStrategy implements ExpenseSplitStrategy {
    @Override
    public void validateShares(Expense expense, List<ExpenseShare> shares) {
        if (shares.isEmpty()) {
            throw new ExpenseValidationException("At least one share is required");
        }

        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (ExpenseShare share : shares) {
            if (share.getPercentage() == null || 
                share.getPercentage().compareTo(BigDecimal.ZERO) <= 0 || 
                share.getPercentage().compareTo(BigDecimal.ONE) > 0) {
                throw new ExpenseValidationException("Percentage must be between 0 and 1");
            }
            totalPercentage = totalPercentage.add(share.getPercentage());
        }

        if (totalPercentage.compareTo(BigDecimal.ONE) != 0) {
            throw new ExpenseValidationException(
                String.format("Total percentage (%s) must sum up to 100%%", 
                    totalPercentage.multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        // Calculate share amounts based on percentages
        for (ExpenseShare share : shares) {
            BigDecimal shareAmount = expense.getAmount()
                    .multiply(share.getPercentage())
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            share.setShareAmount(shareAmount);
        }
    }
} 