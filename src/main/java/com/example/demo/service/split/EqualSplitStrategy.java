package com.example.demo.service.split;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class EqualSplitStrategy implements ExpenseSplitStrategy {
    @Override
    public void validateShares(Expense expense, List<ExpenseShare> shares) {
        if (shares.isEmpty()) {
            throw new ExpenseValidationException("At least one share is required");
        }

        BigDecimal amountShouldBePresent = expense.getAmount()
                .divide(BigDecimal.valueOf(shares.size()), 2, RoundingMode.HALF_UP);

        for (ExpenseShare share : shares) {
            if (share.getShareAmount().compareTo(amountShouldBePresent) != 0) {
                throw new ExpenseValidationException(
                    String.format("Each person should have an equal split of %s", amountShouldBePresent));
            }
        }
    }
} 