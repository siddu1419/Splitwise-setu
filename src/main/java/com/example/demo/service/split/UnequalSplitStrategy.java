package com.example.demo.service.split;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class UnequalSplitStrategy implements ExpenseSplitStrategy {
    @Override
    public void validateShares(Expense expense, List<ExpenseShare> shares) {
        if (shares.isEmpty()) {
            throw new ExpenseValidationException("At least one share is required");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (ExpenseShare share : shares) {
            if (share.getShareAmount() == null || share.getShareAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ExpenseValidationException("Share amount must be greater than 0");
            }
            sum = sum.add(share.getShareAmount());
        }

        if (sum.compareTo(expense.getAmount()) != 0) {
            throw new ExpenseValidationException(
                String.format("Split amounts (%s) do not match the total amount (%s)", 
                    sum.setScale(2, BigDecimal.ROUND_HALF_UP),
                    expense.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
        }
    }
} 